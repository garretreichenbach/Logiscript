package luamade.docs;

import luamade.LuaMade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DocsRepository {

	private static final String[] DOC_FILES = {
			"general/luamade.md",
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
		for(String file : DOC_FILES) {
			String path = "/docs/markdown/" + file;
			try(InputStream in = DocsRepository.class.getResourceAsStream(path)) {
				if(in == null) {
					continue;
				}

				String markdown = readAll(in);
				String title = extractTitle(markdown, file);
				topics.add(new DocTopic(path, title, markdown));
			} catch(IOException exception) {
				LuaMade.getInstance().logException("Error loading documentation file: " + path, exception);
			}
		}
		return topics;
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
}

