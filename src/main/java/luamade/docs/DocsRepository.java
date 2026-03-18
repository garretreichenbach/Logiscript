package luamade.docs;

import luamade.LuaMade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DocsRepository {

	private static final String DOC_INDEX_PATH = "docs/docs.index";

	private static List<DocTopic> cachedTopics;

	private DocsRepository() {
	}

	public static List<DocTopic> getTopics() {
		if(cachedTopics != null) {
			return cachedTopics;
		}
		List<DocTopic> topics = loadTopics();
		if(!topics.isEmpty()) {
			// Only cache a non-empty result so a transient load failure can be retried next open
			cachedTopics = Collections.unmodifiableList(topics);
		}
		return topics;
	}

	/** Clears the topic cache so it is reloaded on the next call to getTopics(). */
	public static void invalidateCache() {
		cachedTopics = null;
	}

	private static List<DocTopic> loadTopics() {
		List<DocTopic> topics = new ArrayList<>();
		for(String file : getDocFiles()) {
			try(InputStream in = openResource(file)) {
				if(in == null) {
					LuaMade.getInstance().logWarning("Documentation file not found in resources: " + file);
					continue;
				}

				String markdown = readAll(in);
				if(markdown.isEmpty()) {
					continue;
				}
				String title = extractTitle(markdown, file);
				String sectionKey = extractSectionKey(file);
				topics.add(new DocTopic("/" + file, title, markdown, sectionKey, formatSectionLabel(sectionKey)));
			} catch(IOException exception) {
				LuaMade.getInstance().logException("Error loading documentation file: " + file, exception);
			}
		}
		topics.sort(Comparator.comparingInt(DocsRepository::getSectionOrder).thenComparing(DocTopic::getSectionLabel).thenComparing(DocTopic::getTitle));
		return topics;
	}

	private static int getSectionOrder(DocTopic topic) {
		if(topic == null || topic.getSectionKey() == null) {
			return 100;
		}
		switch(topic.getSectionKey()) {
			case "core":
				return 0;
			case "io":
				return 1;
			case "math":
				return 2;
			case "entities":
				return 3;
			case "systems":
				return 4;
			case "functions":
				return 5;
			default:
				return 10;
		}
	}

	private static List<String> getDocFiles() {
		List<String> docFiles = loadDocFilesFromIndex();
		if(!docFiles.isEmpty()) {
			return docFiles;
		}

		return new ArrayList<>();
	}

	/**
	 * Tries multiple ClassLoader strategies to find a resource.
	 * ClassLoader.getResourceAsStream() requires no leading slash, unlike Class.getResourceAsStream().
	 */
	private static InputStream openResource(String path) {
		// Ensure no leading slash for ClassLoader-based lookups
		String bare = path.startsWith("/") ? path.substring(1) : path;
		return LuaMade.getInstance().getClass().getClassLoader().getResourceAsStream(bare);
	}

	private static List<String> loadDocFilesFromIndex() {
		List<String> files = new ArrayList<>();
		try(InputStream in = openResource(DOC_INDEX_PATH)) {
			if(in == null) {
				LuaMade.getInstance().logWarning("Documentation index not found, falling back to hardcoded file list");
				return files;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if(trimmed.endsWith(".md")) {
					files.add(trimmed);
				}
			}
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Error loading documentation index", exception);
		}
		return files;
	}

	private static String readAll(InputStream in) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		StringBuilder builder = new StringBuilder();
		String line;
		while((line = reader.readLine()) != null) {
			builder.append(line).append('\n');
		}
		return builder.toString().trim();
	}

	private static String extractTitle(String markdown, String fallbackPath) {
		String[] lines = markdown.split("\\n");
		for(String line : lines) {
			String trimmed = line.trim();
			if(trimmed.startsWith("#")) {
				return trimmed.replaceFirst("^#+\\s*", "").trim();
			}
		}

		int slash = fallbackPath.lastIndexOf('/');
		String fallback = slash >= 0 ? fallbackPath.substring(slash + 1) : fallbackPath;
		return fallback.replace(".md", "");
	}

	private static String extractSectionKey(String path) {
		int slash = path.indexOf('/');
		if(slash <= 0) {
			return "general";
		}
		return path.substring(0, slash).trim().toLowerCase(Locale.ROOT);
	}

	private static String formatSectionLabel(String sectionKey) {
		if(sectionKey == null || sectionKey.trim().isEmpty()) {
			return "General";
		}

		String normalized = sectionKey.trim().replace('-', ' ').replace('_', ' ');
		String[] words = normalized.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for(String word : words) {
			if(word.isEmpty()) {
				continue;
			}
			if(builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(Character.toUpperCase(word.charAt(0)));
			if(word.length() > 1) {
				builder.append(word.substring(1).toLowerCase(Locale.ROOT));
			}
		}
		return builder.length() == 0 ? "General" : builder.toString();
	}
}

