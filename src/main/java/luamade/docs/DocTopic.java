package luamade.docs;

public class DocTopic {

	private final String resourcePath;
	private final String title;
	private final String markdown;
	private final String sectionKey;
	private final String sectionLabel;
	private final String searchText;

	public DocTopic(String resourcePath, String title, String markdown, String sectionKey, String sectionLabel) {
		this.resourcePath = resourcePath;
		this.title = title;
		this.markdown = markdown;
		this.sectionKey = sectionKey;
		this.sectionLabel = sectionLabel;
		searchText = normalizeForSearch(sectionLabel + "\n" + title + "\n" + markdown + "\n" + resourcePath);
	}

	public String getResourcePath() {
		return resourcePath;
	}

	public String getTitle() {
		return title;
	}

	public String getMarkdown() {
		return markdown;
	}

	private static String normalizeForSearch(String text) {
		if(text == null) {
			return "";
		}

		return text.toLowerCase()
				.replace('\r', ' ')
				.replace('\n', ' ')
				.replaceAll("[`*_>#\\[\\](){}:;,.!\\-]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	public String getSectionKey() {
		return sectionKey;
	}

	public String getSectionLabel() {
		return sectionLabel;
	}

	public boolean matchesSearch(String query) {
		String normalizedQuery = normalizeForSearch(query);
		if(normalizedQuery.isEmpty()) {
			return true;
		}

		String[] terms = normalizedQuery.split("\\s+");
		for(String term : terms) {
			if(!term.isEmpty() && !searchText.contains(term)) {
				return false;
			}
		}
		return true;
	}
}

