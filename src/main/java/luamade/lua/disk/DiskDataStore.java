package luamade.lua.disk;

import luamade.LuaMade;
import luamade.utils.DataUtils;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class DiskDataStore {

	private static final File diskStorage = resolveDiskStorage();
	private static volatile boolean storagePathLogged;
	private static final String DISK_ID_KEY = "luamadeDiskId";

	private DiskDataStore() {
	}

	private static File resolveDiskStorage() {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath != null && !worldDataPath.trim().isEmpty()) {
			return new File(worldDataPath, "disks");
		}

		String fallbackRoot = DataUtils.getResourcesPath() + "/data/luamade-local";
		return new File(fallbackRoot, "disks");
	}

	private static void ensureStorageReady() {
		if(!diskStorage.exists()) {
			diskStorage.mkdirs();
		}
		logStoragePathOnce();
	}

	private static void logStoragePathOnce() {
		if(storagePathLogged) {
			return;
		}

		synchronized(DiskDataStore.class) {
			if(storagePathLogged) {
				return;
			}
			storagePathLogged = true;
			LuaMade.getInstance().logInfo("Using disk storage: " + diskStorage.getAbsolutePath());
		}
	}

	public static String resolveDiskKey(InventorySlot slot, SegmentPiece drivePiece) {
		if(slot == null) {
			return null;
		}

		String metadata = readDiskMetadata(slot);
		if(metadata == null || metadata.isEmpty()) {
			metadata = "dsk-" + UUID.randomUUID();
			if(!writeDiskMetadata(slot, metadata)) {
				metadata = null;
			}
		}

		if(metadata != null && !metadata.isEmpty()) {
			return sanitizeDiskKey(metadata);
		}

		if(drivePiece == null) {
			return null;
		}

		// Fallback when runtime inventory metadata APIs are unavailable.
		return "drive-" + drivePiece.getAbsoluteIndex() + "-slot0";
	}

	public static boolean saveProgram(String diskKey, String programName, String sourceCode) {
		String normalizedName = normalizeProgramName(programName);
		if(normalizedName == null || sourceCode == null) {
			return false;
		}
		return writeText(diskKey, "/programs/" + normalizedName + ".lua", sourceCode);
	}

	public static String loadProgram(String diskKey, String programName) {
		String normalizedName = normalizeProgramName(programName);
		if(normalizedName == null) {
			return null;
		}
		return readText(diskKey, "/programs/" + normalizedName + ".lua");
	}

	public static List<String> listPrograms(String diskKey) {
		List<String> out = new ArrayList<String>();
		File programsDirectory = resolveDiskPath(diskKey, "/programs");
		if(programsDirectory == null || !programsDirectory.exists() || !programsDirectory.isDirectory()) {
			return out;
		}

		File[] files = programsDirectory.listFiles();
		if(files == null) {
			return out;
		}

		for(File file : files) {
			if(file == null || !file.isFile()) {
				continue;
			}
			String fileName = file.getName();
			if(fileName.endsWith(".lua")) {
				out.add(fileName.substring(0, fileName.length() - 4));
			}
		}
		return out;
	}

	public static boolean setProgramPrice(String diskKey, String programName, int price) {
		String normalizedName = normalizeProgramName(programName);
		if(normalizedName == null || price < 0) {
			return false;
		}
		return writeText(diskKey, "/programs/" + normalizedName + ".price", String.valueOf(price));
	}

	public static Integer getProgramPrice(String diskKey, String programName) {
		String normalizedName = normalizeProgramName(programName);
		if(normalizedName == null) {
			return null;
		}
		String raw = readText(diskKey, "/programs/" + normalizedName + ".price");
		if(raw == null || raw.trim().isEmpty()) {
			return null;
		}
		try {
			return Integer.parseInt(raw.trim());
		} catch(Exception ignored) {
			return null;
		}
	}

	public static boolean writeText(String diskKey, String path, String text) {
		File file = resolveDiskPath(diskKey, path);
		if(file == null || text == null) {
			return false;
		}

		try {
			File parent = file.getParentFile();
			if(parent != null && !parent.exists()) {
				parent.mkdirs();
			}
			Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
			return true;
		} catch(IOException ignored) {
			return false;
		}
	}

	public static String readText(String diskKey, String path) {
		File file = resolveDiskPath(diskKey, path);
		if(file == null || !file.exists() || !file.isFile()) {
			return null;
		}

		try {
			byte[] bytes = Files.readAllBytes(file.toPath());
			return new String(bytes, StandardCharsets.UTF_8);
		} catch(IOException ignored) {
			return null;
		}
	}

	private static File resolveDiskPath(String diskKey, String path) {
		String normalizedKey = sanitizeDiskKey(diskKey);
		if(normalizedKey == null || normalizedKey.isEmpty()) {
			return null;
		}
		String normalizedPath = normalizePath(path);
		if(normalizedPath == null) {
			return null;
		}

		ensureStorageReady();
		File diskDir = new File(diskStorage, normalizedKey);
		if(!diskDir.exists()) {
			diskDir.mkdirs();
		}

		File target = new File(diskDir, normalizedPath.substring(1));
		if(!isInside(diskDir, target)) {
			return null;
		}
		return target;
	}

	private static String normalizePath(String inputPath) {
		if(inputPath == null || inputPath.trim().isEmpty()) {
			return null;
		}

		String working = inputPath.replace('\\', '/').trim();
		if(!working.startsWith("/")) {
			working = "/" + working;
		}

		String[] tokens = working.split("/");
		List<String> resolved = new ArrayList<String>();
		for(String token : tokens) {
			if(token == null || token.isEmpty() || ".".equals(token)) {
				continue;
			}
			if("..".equals(token)) {
				if(!resolved.isEmpty()) {
					resolved.remove(resolved.size() - 1);
				}
				continue;
			}
			resolved.add(token);
		}

		StringBuilder out = new StringBuilder("/");
		for(int i = 0; i < resolved.size(); i++) {
			if(i > 0) {
				out.append('/');
			}
			out.append(resolved.get(i));
		}
		return out.toString();
	}

	private static boolean isInside(File root, File child) {
		try {
			String rootPath = root.getCanonicalPath();
			String childPath = child.getCanonicalPath();
			return childPath.equals(rootPath) || childPath.startsWith(rootPath + File.separator);
		} catch(IOException ignored) {
			return false;
		}
	}

	private static String normalizeProgramName(String name) {
		if(name == null) {
			return null;
		}
		String normalized = name.trim();
		if(normalized.isEmpty()) {
			return null;
		}

		StringBuilder out = new StringBuilder();
		for(int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			if(Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '-') {
				out.append(c);
			}
		}
		return out.length() == 0 ? null : out.toString();
	}

	private static String sanitizeDiskKey(String input) {
		if(input == null) {
			return null;
		}
		String value = input.trim().toLowerCase(Locale.ROOT);
		if(value.isEmpty()) {
			return null;
		}

		StringBuilder out = new StringBuilder();
		for(int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if(Character.isLetterOrDigit(c) || c == '-' || c == '_') {
				out.append(c);
			}
		}
		return out.length() == 0 ? null : out.toString();
	}

	private static String readDiskMetadata(InventorySlot slot) {
		if(slot == null || !slot.hasCustomData()) {
			return null;
		}

		JSONObject customData = slot.getCustomData();
		if(customData == null || !customData.has(DISK_ID_KEY) || customData.isNull(DISK_ID_KEY)) {
			return null;
		}
		return customData.optString(DISK_ID_KEY, null);
	}

	private static boolean writeDiskMetadata(InventorySlot slot, String value) {
		if(slot == null || value == null || value.isEmpty()) {
			return false;
		}

		JSONObject customData = slot.getOrCreateCustomData();
		customData.put(DISK_ID_KEY, value);
		slot.setCustomData(customData);
		return true;
	}
}
