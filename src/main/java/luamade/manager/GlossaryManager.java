package luamade.manager;

import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import luamade.LuaMade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Manages the in-game glossary entries for the mod.
 * Loads documentation from markdown files and converts them to glossary entries.
 *
 * @author VideoGoose (TheDerpGamer)
 */
public class GlossaryManager {

	/**
	 * Loads all documentation markdown files from resources/docs/markdown/ and adds them as entries to the category.
	 *
	 * @param category The category to add entries to
	 */
	private static void loadDocumentationFromMarkdown(GlossarCategory category) {
		String[] docFiles = {
				"overview.md",
				"filesystem.md",
				"terminal.md",
				"networking.md",
				"console.md",
				"usage.md",
				"enhancements.md",
				"comparison.md",
				"development.md"
		};
		for(String file : docFiles) {
			String path = "/docs/markdown/" + file;
			try(InputStream in = GlossaryManager.class.getResourceAsStream(path)) {
				if(in != null) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					StringBuilder contentBuilder = new StringBuilder();
					String title = null;
					String line;
					while((line = reader.readLine()) != null) {
						if(title == null && line.trim().startsWith("#")) {
							title = line.replaceFirst("#* ", "").trim();
						} else {
							contentBuilder.append(line).append("\n");
						}
					}
					if(title != null) {
						category.addEntry(new GlossarEntry(title, contentBuilder.toString().trim()));
					}
				}
			} catch(IOException e) {
				LuaMade.getInstance().logException("Error loading documentation file: " + path, e);
			}
		}
	}

	public static void initialize(LuaMade instance) {
		GlossarInit.initGlossar(instance);
		GlossarCategory luaMadeDocs = new GlossarCategory("LuaMade");
		loadDocumentationFromMarkdown(luaMadeDocs);
		GlossarInit.addCategory(luaMadeDocs);
	}
}
