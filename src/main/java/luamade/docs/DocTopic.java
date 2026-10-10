package luamade.docs;

public class DocTopic {

	private final String resourcePath;
	private final String title;
	private final String markdown;
	private final String sectionKey;
	private final String sectionLabel;

	public DocTopic(String resourcePath, String title, String markdown, String sectionKey, String sectionLabel) {
		this.resourcePath = resourcePath;
		this.title = title;
		this.markdown = markdown;
		this.sectionKey = sectionKey;
		this.sectionLabel = sectionLabel;
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

	public String getSectionKey() {
		return sectionKey;
	}

	public String getSectionLabel() {
		return sectionLabel;
	}
}
