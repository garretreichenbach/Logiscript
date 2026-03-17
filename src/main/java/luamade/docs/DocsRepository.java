package luamade.docs;

import luamade.LuaMade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DocsRepository {

	private static final String DOC_INDEX_PATH = "/docs/markdown/docs.index";
	private static final String[] FALLBACK_DOC_FILES = {
			"general/luamade.md",
			"general/channels.md",
			"general/terminal.md",
			"functions/console.md",
			"functions/block.md",
			"functions/blockinfo.md",
			"functions/itemstack.md"
	};

	private static List<DocTopic> cachedTopics;

	private DocsRepository() {
	}

	public static List<DocTopic> getTopics() {
		if(cachedTopics == null) {
			cachedTopics = Collections.unmodifiableList(loadTopics());
		}
		return cachedTopics;
	}

	private static List<DocTopic> loadTopics() {
		List<DocTopic> topics = new ArrayList<>();
		for(String file : getDocFiles()) {
			String path = "/docs/markdown/" + file;
			try(InputStream in = DocsRepository.class.getResourceAsStream(path)) {
				if(in == null) {
					continue;
				}

				String markdown = readAll(in);
				String title = extractTitle(markdown, file);
				String sectionKey = extractSectionKey(file);
				topics.add(new DocTopic(path, title, markdown, sectionKey, formatSectionLabel(sectionKey)));
			} catch(IOException exception) {
				LuaMade.getInstance().logException("Error loading documentation file: " + path, exception);
			}
		}
		topics.sort(Comparator.comparing(DocTopic::getSectionLabel).thenComparing(DocTopic::getTitle));
		return topics;
	}

	private static List<String> getDocFiles() {
		List<String> docFiles = loadDocFilesFromIndex();
		if(!docFiles.isEmpty()) {
			return docFiles;
		}

		List<String> fallbackFiles = new ArrayList<>();
		Collections.addAll(fallbackFiles, FALLBACK_DOC_FILES);
		return fallbackFiles;
	}

	private static List<String> loadDocFilesFromIndex() {
		List<String> files = new ArrayList<>();
		try(InputStream in = DocsRepository.class.getResourceAsStream(DOC_INDEX_PATH)) {
			if(in == null) {
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

