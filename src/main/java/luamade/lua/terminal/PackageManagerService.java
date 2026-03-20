package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.manager.ConfigManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class PackageManagerService {

	private static final String INSTALLED_DB_PATH = "/etc/pkg/installed.json";

	private final FileSystem fileSystem;
	private final Console console;

	PackageManagerService(FileSystem fileSystem, Console console) {
		this.fileSystem = fileSystem;
		this.console = console;
	}

	void handleCommand(String rawArgs) {
		if(!ConfigManager.isPackageManagerEnabled()) {
			print("Error: Package manager is disabled by server config");
			return;
		}

		List<String> args = parseArgs(rawArgs == null ? "" : rawArgs.trim());
		if(args.isEmpty()) {
			printUsage();
			return;
		}

		String subCommand = args.get(0).toLowerCase(Locale.ROOT);
		switch(subCommand) {
			case "search":
				handleSearch(args);
				return;
			case "info":
				handleInfo(args);
				return;
			case "fetch":
				handleFetch(args);
				return;
			case "install":
				handleInstall(args);
				return;
			case "list":
				handleList();
				return;
			case "remove":
				handleRemove(args);
				return;
			default:
				print("Error: Unknown pkg subcommand: " + subCommand);
				printUsage();
		}
	}

	private void handleSearch(List<String> args) {
		if(args.size() < 2) {
			print("Error: Usage: pkg search <query>");
			return;
		}

		String query = join(args, 1);
		String encoded = urlEncode(query);
		if(encoded == null) {
			print("Error: Invalid search query");
			return;
		}

		URL endpoint = buildRegistryUrl("/v1/search?q=" + encoded);
		if(endpoint == null) {
			return;
		}

		String response = fetchText(endpoint);
		if(response == null) {
			return;
		}

		List<JSONObject> entries = parseSearchResults(response);
		if(entries.isEmpty()) {
			print("No packages found");
			return;
		}

		for(JSONObject entry : entries) {
			String name = entry.optString("name", "unknown");
			String version = entry.optString("version", "?");
			String description = entry.optString("description", "");
			if(description.length() > 96) {
				description = description.substring(0, 96) + "...";
			}
			if(description.isEmpty()) {
				print(name + " " + version);
			} else {
				print(name + " " + version + " - " + description);
			}
		}
	}

	private void handleInfo(List<String> args) {
		if(args.size() < 2) {
			print("Error: Usage: pkg info <name> [version]");
			return;
		}

		PackageMetadata metadata = fetchMetadata(args.get(1), args.size() >= 3 ? args.get(2) : null);
		if(metadata == null) {
			return;
		}

		print("name: " + metadata.name);
		print("version: " + metadata.version);
		if(metadata.type != null && !metadata.type.isEmpty()) {
			print("type: " + metadata.type);
		}
		if(metadata.description != null && !metadata.description.isEmpty()) {
			print("description: " + metadata.description);
		}
		print("artifact: " + metadata.artifactType);
		print("download: " + metadata.downloadUrl);
		if(metadata.sha256 != null && !metadata.sha256.isEmpty()) {
			print("sha256: " + metadata.sha256);
		}
		print("install-root: " + metadata.installRoot);
	}

	private void handleFetch(List<String> args) {
		if(args.size() < 2) {
			print("Error: Usage: pkg fetch <name> [version] [output-file]");
			return;
		}

		String name = args.get(1);
		String version = null;
		String outputPath = null;
		if(args.size() >= 3) {
			version = args.get(2);
		}
		if(args.size() >= 4) {
			outputPath = args.get(3);
		}

		PackageMetadata metadata = fetchMetadata(name, version);
		if(metadata == null) {
			return;
		}

		byte[] payload = downloadArtifact(metadata);
		if(payload == null) {
			return;
		}

		String targetPath = outputPath;
		if(targetPath == null || targetPath.trim().isEmpty()) {
			String ext = "zip".equals(metadata.artifactType) ? ".zip" : ".lua";
			targetPath = "/tmp/" + metadata.name + "-" + metadata.version + ext;
		}

		if(fileSystem.write(targetPath, new String(payload, StandardCharsets.UTF_8))) {
			print("Saved " + payload.length + " bytes to " + fileSystem.normalizePath(targetPath));
		} else {
			print("Error: Could not write output file");
		}
	}

	private void handleInstall(List<String> args) {
		if(args.size() < 2) {
			print("Error: Usage: pkg install <name> [version]");
			return;
		}

		String name = args.get(1);
		String version = args.size() >= 3 ? args.get(2) : null;

		PackageMetadata metadata = fetchMetadata(name, version);
		if(metadata == null) {
			return;
		}

		byte[] payload = downloadArtifact(metadata);
		if(payload == null) {
			return;
		}

		boolean installed;
		if("lua".equals(metadata.artifactType)) {
			installed = installLuaArtifact(metadata, payload);
		} else {
			installed = installZipArtifact(metadata, payload);
		}

		if(!installed) {
			return;
		}

		recordInstalledPackage(metadata);
		print("Installed " + metadata.name + " " + metadata.version + " to " + metadata.installRoot);
	}

	private void handleList() {
		JSONObject packages = getInstalledPackagesObject();
		if(packages.length() == 0) {
			print("No packages installed");
			return;
		}

		List<String> names = new ArrayList<>();
		Iterator<String> keys = packages.keys();
		while(keys.hasNext()) {
			names.add(keys.next());
		}
		Collections.sort(names);

		for(String name : names) {
			JSONObject entry = packages.optJSONObject(name);
			if(entry == null) {
				continue;
			}
			print(name + " " + entry.optString("version", "?") + " -> " + entry.optString("installRoot", "?"));
		}
	}

	private void handleRemove(List<String> args) {
		if(args.size() < 2) {
			print("Error: Usage: pkg remove <name>");
			return;
		}

		String name = args.get(1).toLowerCase(Locale.ROOT);
		JSONObject packages = getInstalledPackagesObject();
		JSONObject entry = packages.optJSONObject(name);
		if(entry == null) {
			print("Error: Package is not installed: " + name);
			return;
		}

		String installRoot = fileSystem.normalizePath(entry.optString("installRoot", ""));
		if(installRoot.isEmpty() || "/".equals(installRoot)) {
			print("Error: Refusing to remove unsafe install root");
			return;
		}

		if(!removePathRecursive(installRoot)) {
			print("Error: Failed removing package files at " + installRoot);
			return;
		}

		packages.remove(name);
		saveInstalledPackages(packages);
		print("Removed " + name);
	}

	private PackageMetadata fetchMetadata(String name, String version) {
		if(name == null || name.trim().isEmpty()) {
			print("Error: Package name is required");
			return null;
		}

		String normalizedName = name.trim().toLowerCase(Locale.ROOT);
		String encodedName = urlEncodePathSegment(normalizedName);
		if(encodedName == null) {
			print("Error: Invalid package name");
			return null;
		}

		String endpointPath;
		if(version == null || version.trim().isEmpty()) {
			endpointPath = "/v1/packages/" + encodedName;
		} else {
			String encodedVersion = urlEncodePathSegment(version.trim());
			if(encodedVersion == null) {
				print("Error: Invalid package version");
				return null;
			}
			endpointPath = "/v1/packages/" + encodedName + "/" + encodedVersion;
		}

		URL endpoint = buildRegistryUrl(endpointPath);
		if(endpoint == null) {
			return null;
		}

		String payload = fetchText(endpoint);
		if(payload == null) {
			return null;
		}

		try {
			JSONObject json = new JSONObject(payload);
			PackageMetadata metadata = PackageMetadata.fromJson(json);
			if(metadata == null) {
				print("Error: Invalid package metadata response");
				return null;
			}
			return metadata;
		} catch(JSONException jsonException) {
			print("Error: Registry returned invalid JSON");
			return null;
		}
	}

	private byte[] downloadArtifact(PackageMetadata metadata) {
		URL artifactUrl = validateArtifactUrl(metadata.downloadUrl);
		if(artifactUrl == null) {
			return null;
		}

		byte[] payload = fetchBytes(artifactUrl);
		if(payload == null) {
			return null;
		}

		if(metadata.sha256 != null && !metadata.sha256.isEmpty()) {
			String computed = sha256Hex(payload);
			if(computed == null || !computed.equalsIgnoreCase(metadata.sha256)) {
				print("Error: SHA-256 verification failed for package artifact");
				return null;
			}
		}

		return payload;
	}

	private boolean installLuaArtifact(PackageMetadata metadata, byte[] payload) {
		String installRoot = metadata.installRoot;
		if("/bin".equals(installRoot)) {
			String targetPath = fileSystem.normalizePath(installRoot + "/" + metadata.name + ".lua");
			return fileSystem.write(targetPath, new String(payload, StandardCharsets.UTF_8));
		}

		if(!ensureDirectory(installRoot)) {
			return false;
		}

		String targetPath = fileSystem.normalizePath(installRoot + "/init.lua");
		return fileSystem.write(targetPath, new String(payload, StandardCharsets.UTF_8));
	}

	private boolean installZipArtifact(PackageMetadata metadata, byte[] payload) {
		if(!ensureDirectory(metadata.installRoot)) {
			return false;
		}

		try {
			ZipInputStream zipInputStream = new ZipInputStream(new java.io.ByteArrayInputStream(payload), StandardCharsets.UTF_8);
			ZipEntry entry;
			while((entry = zipInputStream.getNextEntry()) != null) {
				String entryName = sanitizeZipEntryName(entry.getName());
				if(entryName == null || entryName.isEmpty()) {
					zipInputStream.closeEntry();
					continue;
				}

				String targetPath = fileSystem.normalizePath(metadata.installRoot + "/" + entryName);
				if(!isUnderRoot(targetPath, metadata.installRoot)) {
					print("Error: Package archive contains unsafe path: " + entry.getName());
					zipInputStream.closeEntry();
					return false;
				}

				if(entry.isDirectory()) {
					if(!ensureDirectory(targetPath)) {
						zipInputStream.closeEntry();
						return false;
					}
					zipInputStream.closeEntry();
					continue;
				}

				String parentPath = parentPathOf(targetPath);
				if(parentPath != null && !ensureDirectory(parentPath)) {
					zipInputStream.closeEntry();
					return false;
				}

				byte[] fileBytes = readFully(zipInputStream, ConfigManager.getPackageManagerMaxBytes());
				if(fileBytes == null) {
					print("Error: Package file exceeded package_manager_max_bytes limit");
					zipInputStream.closeEntry();
					return false;
				}

				if(!fileSystem.write(targetPath, new String(fileBytes, StandardCharsets.UTF_8))) {
					print("Error: Failed writing file: " + targetPath);
					zipInputStream.closeEntry();
					return false;
				}

				zipInputStream.closeEntry();
			}
			zipInputStream.close();
			return true;
		} catch(IOException ioException) {
			print("Error: Failed to read package archive: " + ioException.getMessage());
			return false;
		}
	}

	private void recordInstalledPackage(PackageMetadata metadata) {
		JSONObject packages = getInstalledPackagesObject();
		JSONObject packageJson = new JSONObject();
		packageJson.put("name", metadata.name);
		packageJson.put("version", metadata.version);
		packageJson.put("type", metadata.type);
		packageJson.put("installRoot", metadata.installRoot);
		packageJson.put("artifactType", metadata.artifactType);
		packageJson.put("installedAt", System.currentTimeMillis());
		packages.put(metadata.name.toLowerCase(Locale.ROOT), packageJson);
		saveInstalledPackages(packages);
	}

	private JSONObject getInstalledPackagesObject() {
		String content = fileSystem.read(INSTALLED_DB_PATH);
		if(content == null || content.trim().isEmpty()) {
			return new JSONObject();
		}

		try {
			JSONObject root = new JSONObject(content);
			JSONObject packages = root.optJSONObject("packages");
			if(packages != null) {
				return packages;
			}
		} catch(JSONException ignored) {
			// Fall through to empty package object.
		}
		return new JSONObject();
	}

	private void saveInstalledPackages(JSONObject packages) {
		JSONObject root = new JSONObject();
		root.put("packages", packages);
		fileSystem.write(INSTALLED_DB_PATH, root.toString(2));
	}

	private boolean removePathRecursive(String path) {
		String normalized = fileSystem.normalizePath(path);
		if(!fileSystem.exists(normalized)) {
			return true;
		}

		if(fileSystem.isDir(normalized)) {
			List<String> children = fileSystem.list(normalized);
			for(String child : children) {
				String childPath = fileSystem.normalizePath(normalized + "/" + child);
				if(!removePathRecursive(childPath)) {
					return false;
				}
			}
		}

		return fileSystem.delete(normalized);
	}

	private URL validateArtifactUrl(String rawUrl) {
		URL url;
		try {
			url = new URL(rawUrl);
		} catch(MalformedURLException malformedURLException) {
			print("Error: Registry returned invalid download URL");
			return null;
		}

		String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
		if(!"http".equals(protocol) && !"https".equals(protocol)) {
			print("Error: Package artifact URL must use http/https");
			return null;
		}

		String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
		if(host.isEmpty()) {
			print("Error: Package artifact URL is missing a hostname");
			return null;
		}

		if(ConfigManager.isPackageManagerTrustedDomainsOnly() && !ConfigManager.isTrustedWebDomain(host)) {
			print("Error: Package artifact domain is not in trusted allowlist");
			return null;
		}

		return url;
	}

	private URL buildRegistryUrl(String pathAndQuery) {
		String rawBase = ConfigManager.getPackageManagerBaseUrl();
		if(rawBase == null || rawBase.trim().isEmpty()) {
			print("Error: package_manager_base_url is empty");
			return null;
		}

		String trimmedBase = rawBase.trim();
		if(trimmedBase.endsWith("/")) {
			trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
		}

		URL baseUrl;
		try {
			baseUrl = new URL(trimmedBase);
		} catch(MalformedURLException malformedURLException) {
			print("Error: Invalid package manager base URL in config");
			return null;
		}

		String protocol = baseUrl.getProtocol() == null ? "" : baseUrl.getProtocol().toLowerCase(Locale.ROOT);
		if(!"http".equals(protocol) && !"https".equals(protocol)) {
			print("Error: package_manager_base_url must use http or https");
			return null;
		}

		String host = baseUrl.getHost() == null ? "" : baseUrl.getHost().toLowerCase(Locale.ROOT);
		if(host.isEmpty()) {
			print("Error: package_manager_base_url must include a hostname");
			return null;
		}

		if(ConfigManager.isPackageManagerTrustedDomainsOnly() && !ConfigManager.isTrustedWebDomain(host)) {
			print("Error: package_manager_base_url host is not in trusted_domains.txt");
			return null;
		}

		try {
			return new URL(trimmedBase + pathAndQuery);
		} catch(MalformedURLException malformedURLException) {
			print("Error: Invalid package manager endpoint URL");
			return null;
		}
	}

	private String fetchText(URL url) {
		byte[] bytes = fetchBytes(url);
		if(bytes == null) {
			return null;
		}
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private byte[] fetchBytes(URL url) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(ConfigManager.getPackageManagerTimeoutMs());
			connection.setReadTimeout(ConfigManager.getPackageManagerTimeoutMs());
			connection.setRequestProperty("User-Agent", "LuaMadePkg/1.0");

			int status = connection.getResponseCode();
			InputStream input = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
			byte[] payload = readFully(input, ConfigManager.getPackageManagerMaxBytes());
			if(payload == null) {
				print("Error: Response exceeded package_manager_max_bytes limit");
				return null;
			}

			if(status < 200 || status >= 300) {
				String body = new String(payload, StandardCharsets.UTF_8).trim();
				if(body.isEmpty()) {
					print("Error: HTTP status " + status);
				} else {
					print("Error: HTTP status " + status + " - " + body);
				}
				return null;
			}

			return payload;
		} catch(IOException ioException) {
			print("Error fetching package data: " + ioException.getMessage());
			return null;
		} finally {
			if(connection != null) {
				connection.disconnect();
			}
		}
	}

	private byte[] readFully(InputStream input, int maxBytes) throws IOException {
		if(input == null) {
			return new byte[0];
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int total = 0;
		while(true) {
			int read = input.read(buffer);
			if(read < 0) {
				break;
			}
			total += read;
			if(total > maxBytes) {
				return null;
			}
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	private List<JSONObject> parseSearchResults(String payload) {
		try {
			Object json = new org.json.JSONTokener(payload).nextValue();
			if(json instanceof JSONArray) {
				return jsonArrayToObjects((JSONArray) json);
			}
			if(json instanceof JSONObject) {
				JSONObject object = (JSONObject) json;
				JSONArray packages = object.optJSONArray("packages");
				if(packages == null) {
					packages = object.optJSONArray("results");
				}
				if(packages == null) {
					packages = object.optJSONArray("items");
				}
				if(packages != null) {
					return jsonArrayToObjects(packages);
				}
			}
		} catch(JSONException ignored) {
			// Return empty list on malformed response.
		}
		return Collections.emptyList();
	}

	private List<JSONObject> jsonArrayToObjects(JSONArray array) {
		List<JSONObject> out = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			JSONObject object = array.optJSONObject(i);
			if(object != null) {
				out.add(object);
			}
		}
		return out;
	}

	private boolean ensureDirectory(String path) {
		String normalized = fileSystem.normalizePath(path);
		if(fileSystem.exists(normalized)) {
			if(fileSystem.isDir(normalized)) {
				return true;
			}
			print("Error: Path exists but is not a directory: " + normalized);
			return false;
		}
		if(!fileSystem.makeDir(normalized)) {
			print("Error: Could not create directory: " + normalized);
			return false;
		}
		return true;
	}

	private String sanitizeZipEntryName(String rawName) {
		if(rawName == null) {
			return null;
		}

		String normalized = rawName.replace('\\', '/').trim();
		while(normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if(normalized.isEmpty()) {
			return null;
		}
		if(normalized.contains("../") || normalized.equals("..") || normalized.contains(":/")) {
			return null;
		}
		return normalized;
	}

	private boolean isUnderRoot(String path, String root) {
		String normalizedRoot = fileSystem.normalizePath(root);
		String normalizedPath = fileSystem.normalizePath(path);
		if(normalizedPath.equals(normalizedRoot)) {
			return true;
		}
		return normalizedPath.startsWith(normalizedRoot + "/");
	}

	private String parentPathOf(String path) {
		if(path == null || path.isEmpty() || "/".equals(path)) {
			return null;
		}
		int lastSlash = path.lastIndexOf('/');
		if(lastSlash <= 0) {
			return "/";
		}
		return path.substring(0, lastSlash);
	}

	private String sha256Hex(byte[] payload) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(payload);
			StringBuilder out = new StringBuilder(hash.length * 2);
			for(byte b : hash) {
				out.append(String.format(Locale.ROOT, "%02x", b));
			}
			return out.toString();
		} catch(NoSuchAlgorithmException exception) {
			return null;
		}
	}

	private String urlEncode(String text) {
		try {
			return URLEncoder.encode(text, "UTF-8");
		} catch(Exception exception) {
			return null;
		}
	}

	private String urlEncodePathSegment(String text) {
		String encoded = urlEncode(text);
		if(encoded == null) {
			return null;
		}
		return encoded.replace("+", "%20");
	}

	private List<String> parseArgs(String input) {
		if(input == null || input.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> tokens = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;

		for(int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if((c == '\'' || c == '"') && (!inQuotes || c == quoteChar)) {
				if(inQuotes) {
					inQuotes = false;
					quoteChar = 0;
				} else {
					inQuotes = true;
					quoteChar = c;
				}
				continue;
			}
			if(Character.isWhitespace(c) && !inQuotes) {
				if(current.length() > 0) {
					tokens.add(current.toString());
					current.setLength(0);
				}
				continue;
			}
			current.append(c);
		}
		if(current.length() > 0) {
			tokens.add(current.toString());
		}
		return tokens;
	}

	private String join(List<String> parts, int startIndex) {
		if(parts == null || startIndex >= parts.size()) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		for(int i = startIndex; i < parts.size(); i++) {
			if(out.length() > 0) {
				out.append(' ');
			}
			out.append(parts.get(i));
		}
		return out.toString();
	}

	static String normalizePackageToken(String input) {
		if(input == null) {
			return "";
		}
		String normalized = input.trim().toLowerCase(Locale.ROOT);
		if(normalized.isEmpty()) {
			return "";
		}
		for(int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.') {
				continue;
			}
			return "";
		}
		return normalized;
	}

	private void printUsage() {
		print("Usage: pkg <search|info|fetch|install|list|remove> ...");
		print("  pkg search <query>");
		print("  pkg info <name> [version]");
		print("  pkg fetch <name> [version] [output-file]");
		print("  pkg install <name> [version]");
		print("  pkg list");
		print("  pkg remove <name>");
	}

	private void print(String line) {
		console.print(LuaValue.valueOf(line));
	}

	private static final class PackageMetadata {
		private final String name;
		private final String version;
		private final String type;
		private final String description;
		private final String downloadUrl;
		private final String sha256;
		private final String artifactType;
		private final String installRoot;

		private PackageMetadata(String name, String version, String type, String description, String downloadUrl, String sha256, String artifactType, String installRoot) {
			this.name = name;
			this.version = version;
			this.type = type;
			this.description = description;
			this.downloadUrl = downloadUrl;
			this.sha256 = sha256;
			this.artifactType = artifactType;
			this.installRoot = installRoot;
		}

		private static PackageMetadata fromJson(JSONObject json) {
			String name = normalizePackageToken(json.optString("name", ""));
			String version = normalizePackageToken(json.optString("version", ""));
			String downloadUrl = json.optString("downloadUrl", "").trim();
			if(name.isEmpty() || version.isEmpty() || downloadUrl.isEmpty()) {
				return null;
			}

			String type = normalizePackageToken(json.optString("type", "library"));
			if(type.isEmpty()) {
				type = "library";
			}

			String artifactType = normalizePackageToken(json.optString("artifactType", "zip"));
			if(!"lua".equals(artifactType)) {
				artifactType = "zip";
			}

			String defaultInstallRoot = "program".equals(type) ? "/bin" : "/lib/" + name;
			String installRootRaw = json.optString("installRoot", defaultInstallRoot).trim();
			if(installRootRaw.isEmpty()) {
				installRootRaw = defaultInstallRoot;
			}
			String installRoot = installRootRaw.startsWith("/") ? installRootRaw : "/" + installRootRaw;

			String description = json.optString("description", "").trim();
			String sha256 = json.optString("sha256", "").trim();

			return new PackageMetadata(name, version, type, description, downloadUrl, sha256, artifactType, installRoot);
		}
	}
}
