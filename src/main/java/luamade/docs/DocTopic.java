package luamade.docs;

public class DocTopic {

	private final String resourcePath;
	private final String title;
	private final String markdown;

	public DocTopic(String resourcePath, String title, String markdown) {
		this.resourcePath = resourcePath;
		this.title = title;
		this.markdown = markdown;
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
}

