package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import luamade.docs.DocTopic;
import luamade.docs.DocsRepository;
import luamade.docs.MarkdownDocRenderer;
import luamade.system.module.ComputerModule;
import org.newdawn.slick.UnicodeFont;
import org.schema.game.client.controller.PlayerInput;
import org.schema.schine.common.TabCallback;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

import javax.vecmath.Vector4f;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DocsViewerDialog extends PlayerInput {

	private final DocsPanel docsPanel;
	private final ComputerModule reopenComputerModule;
	private boolean reopenHandled;

	public DocsViewerDialog() {
		this(null);
	}

	public DocsViewerDialog(ComputerModule reopenComputerModule) {
		super(GameClient.getClientState());
		this.reopenComputerModule = reopenComputerModule;
		docsPanel = new DocsPanel(getState(), this);
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
		if(!isOccluded() && mouseEvent.pressedLeftMouse()) {
			if(callingElement.getUserPointer() instanceof String) {
				String userPointer = (String) callingElement.getUserPointer();
				if("X".equals(userPointer) || "CANCEL".equals(userPointer) || "OK".equals(userPointer)) {
					deactivate();
				}
			}
		}
	}

	@Override
	public DocsPanel getInputPanel() {
		return docsPanel;
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
	}

	@Override
	public void onDeactivate() {
		// Invalidate the topic cache so a failed first-load is retried next open
		DocsRepository.invalidateCache();
		if(reopenComputerModule != null && !reopenHandled) {
			reopenHandled = true;
			new ComputerDialog(reopenComputerModule).activate();
		}
	}

	public static class DocsPanel extends GUIInputDialogPanel {

		private static final int WINDOW_WIDTH = 1280;
		private static final int WINDOW_HEIGHT = 760;
		private static final int LEFT_WIDTH = 280;
		private static final int PADDING = 12;
		private static final int SEARCH_HEIGHT = 24;
		private static final int SECTION_HEADER_HEIGHT = 22;
		private static final int SECTION_HEADER_GAP = 4;
		private static final int TOPIC_BUTTON_HEIGHT = 28;
		private static final int TOPIC_BUTTON_GAP = 4;
		private static final int TOPIC_INDENT = 10;
		private static final int CONTENT_MARGIN = 12;
		private static final int INLINE_CODE_PADDING_X = 4;
		private static final int INLINE_CODE_PADDING_Y = 4;
		private static final int SCROLLBAR_WIDTH = 16;
		private static final int RIGHT_PANEL_MARGIN = 14;

		private final List<DocTopic> allTopics = new ArrayList<>(DocsRepository.getTopics());
		private final List<DocTopic> filteredTopics = new ArrayList<>();
		private final java.util.Set<String> collapsedSections = new java.util.HashSet<>();
		private DocTopic selectedTopic;
		private String searchQuery = "";

		private GUIAncor searchAnchor;
		private GUIActivatableTextBar searchBar;
		private GUIScrollablePanel topicsScrollPanel;
		private GUIAncor topicsContent;
		private GUIColoredRectangle topicsBackground;
		private GUIScrollablePanel contentScrollPanel;
		private GUIAncor contentBlocks;
		private GUIColoredRectangle contentBackground;
		private GUITextOverlay emptyTopicsOverlay;
		private GUIContentPane mainContentPane;

		public DocsPanel(InputState inputState, GUICallback guiCallback) {
			super(inputState, "LUAMADE_DOCS", "LuaMade Documentation", "", WINDOW_WIDTH, WINDOW_HEIGHT, guiCallback);
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();

			mainContentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIElement root = mainContentPane.getContent(0);

			searchAnchor = new GUIAncor(getState(), LEFT_WIDTH - (PADDING * 2), SEARCH_HEIGHT);
			root.attach(searchAnchor);

			searchBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Search titles or content", searchAnchor, new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return new String[0];
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return "";
				}

				@Override
				public void onFailedTextCheck(String s) {
				}

				@Override
				public void onTextEnter(String input, boolean send, boolean keep) {
				}

				@Override
				public void newLine() {
				}
			}, input -> {
				searchQuery = input == null ? "" : input.trim();
				filterTopics();
				rebuildTopicButtons();
				return input;
			});
			searchBar.onInit();
			root.attach(searchBar);
			searchBar.activateBar();
			TextAreaInput searchTextArea = searchBar.getTextArea();
			if(searchTextArea != null) {
				searchTextArea.onTabCallback = new TabCallback() {
					@Override
					public boolean catchTab(TextAreaInput textAreaInput) {
						return true;
					}

					@Override
					public void onEnter() {
					}
				};
			}

			topicsBackground = new GUIColoredRectangle(getState(), LEFT_WIDTH, WINDOW_HEIGHT - 80, new Vector4f(0.05F, 0.06F, 0.09F, 0.55F));
			topicsBackground.onInit();
			root.attach(topicsBackground);

			// Parent the scroll panel to its pane background so scrollbar math stays local to this pane.
			topicsScrollPanel = new GUIScrollablePanel(LEFT_WIDTH, WINDOW_HEIGHT - 80, topicsBackground, getState());
			topicsScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			topicsContent = new GUIAncor(getState(), LEFT_WIDTH - 12, WINDOW_HEIGHT - 80);
			topicsScrollPanel.setContent(topicsContent);
			topicsBackground.attach(topicsScrollPanel);

			emptyTopicsOverlay = new GUITextOverlay(LEFT_WIDTH - 24, 18, FontLibrary.FontSize.MEDIUM, getState());
			emptyTopicsOverlay.setTextSimple("No matching topics");
			emptyTopicsOverlay.onInit();

			contentBackground = new GUIColoredRectangle(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, new Vector4f(0.05F, 0.06F, 0.09F, 0.35F));
			contentBackground.onInit();
			root.attach(contentBackground);

			// Parent the right scroll panel to its background to keep scrollbar inside the right pane bounds.
			contentScrollPanel = new GUIScrollablePanel(WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, contentBackground, getState());
			contentScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			contentBlocks = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			contentScrollPanel.setContent(contentBlocks);
			contentBackground.attach(contentScrollPanel);

			filterTopics();
			rebuildTopicButtons();
			selectTopic(selectedTopic != null ? selectedTopic : (filteredTopics.isEmpty() ? null : filteredTopics.get(0)));
			layoutComponents();
		}

		@Override
		public void draw() {
			layoutComponents();
			super.draw();
		}

		private void layoutComponents() {
			if(searchAnchor == null) {
				return;
			}

			// Use the content pane's actual usable dimensions rather than the full window size.
			// GUIContentPane.getWidth/Height() exclude the dialog title bar and chrome.
			float availableWidth = mainContentPane != null && mainContentPane.getWidth() > 0 ? mainContentPane.getWidth() : getWidth();
			float availableHeight = mainContentPane != null && mainContentPane.getHeight() > 0 ? mainContentPane.getHeight() : getHeight();

			float contentHeight = Math.max(120.0F, availableHeight - 20.0F);
			float leftHeight = Math.max(120.0F, contentHeight - SEARCH_HEIGHT - 16.0F);
			float rightX = LEFT_WIDTH + (PADDING * 2);
			// RIGHT_PANEL_MARGIN keeps the scroll panel (including its scrollbar) away from the dialog's right chrome
			float rightWidth = Math.max(300.0F, availableWidth - rightX - RIGHT_PANEL_MARGIN);

			searchAnchor.setWidth(LEFT_WIDTH - (PADDING * 2));
			searchAnchor.setHeight(SEARCH_HEIGHT);
			searchAnchor.setPos(PADDING, 6.0F, 0.0F);
			searchBar.setPos(PADDING, 6.0F, 0.0F);

			topicsBackground.setWidth(LEFT_WIDTH);
			topicsBackground.setHeight(leftHeight);
			topicsBackground.setPos(PADDING, SEARCH_HEIGHT + 16.0F, 0.0F);
			topicsScrollPanel.setWidth(LEFT_WIDTH);
			topicsScrollPanel.setHeight(leftHeight);
			// Scroll panels are now children of pane backgrounds.
			topicsScrollPanel.setPos(0.0F, 0.0F, 0.0F);
			// Leave room for the topics scrollbar.
			topicsContent.setWidth(LEFT_WIDTH - SCROLLBAR_WIDTH - 4);

			contentBackground.setWidth(rightWidth);
			contentBackground.setHeight(contentHeight);
			contentBackground.setPos(rightX, 6.0F, 0.0F);
			contentScrollPanel.setWidth(rightWidth);
			contentScrollPanel.setHeight(contentHeight);
			// Scroll panel is now a child of the right pane background.
			contentScrollPanel.setPos(0.0F, 0.0F, 0.0F);
			// Leave room for the content scrollbar so text doesn't flow under it.
			contentBlocks.setWidth(rightWidth - SCROLLBAR_WIDTH - 8);
		}

		private void filterTopics() {
			filteredTopics.clear();
			String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
			for(DocTopic topic : allTopics) {
				if(topic.matchesSearch(normalizedQuery)) {
					filteredTopics.add(topic);
				}
			}

			if(selectedTopic != null && !filteredTopics.contains(selectedTopic)) {
				selectedTopic = filteredTopics.isEmpty() ? null : filteredTopics.get(0);
			}
		}

		private void rebuildTopicButtons() {
			if(topicsContent == null) {
				return;
			}

			topicsContent.detachAll();
			int y = 0;
			if(filteredTopics.isEmpty()) {
				emptyTopicsOverlay.setPos(8.0F, 8.0F, 0.0F);
				topicsContent.attach(emptyTopicsOverlay);
				topicsContent.setHeight(40.0F);
				return;
			}

			String currentSectionKey = null;
			for(DocTopic topic : filteredTopics) {
				if(!topic.getSectionKey().equals(currentSectionKey)) {
					y += addSectionHeader(y, topic.getSectionKey(), topic.getSectionLabel());
					currentSectionKey = topic.getSectionKey();
				}
				// Skip topic rows for collapsed sections
				if(!collapsedSections.contains(topic.getSectionKey())) {
					y += addTopicButton(y, topic);
				}
			}

			topicsContent.setHeight(Math.max(40.0F, y));
		}

		private void toggleSection(String sectionKey) {
			if(collapsedSections.contains(sectionKey)) {
				collapsedSections.remove(sectionKey);
			} else {
				collapsedSections.add(sectionKey);
			}
			rebuildTopicButtons();
		}

		private int addSectionHeader(int y, String sectionKey, String sectionLabel) {
			boolean collapsed = collapsedSections.contains(sectionKey);
			String arrow = collapsed ? "\u25BA  " : "\u25BC  ";  // ► / ▼

			GUITextButton headerButton = new GUITextButton(getState(), LEFT_WIDTH - PADDING, SECTION_HEADER_HEIGHT,
					GUITextButton.ColorPalette.TUTORIAL, arrow + sectionLabel.toUpperCase(), new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if(event.pressedLeftMouse()) {
						toggleSection(sectionKey);
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			});
			headerButton.setTextPos(8, 3);
			headerButton.setMouseUpdateEnabled(true);
			headerButton.setPos(0, y, 0);
			headerButton.onInit();
			topicsContent.attach(headerButton);
			return SECTION_HEADER_HEIGHT + SECTION_HEADER_GAP;
		}

		private int addTopicButton(int y, DocTopic topic) {
			boolean selected = topic.equals(selectedTopic);

			GUIColoredRectangle rowBackground = new GUIColoredRectangle(getState(), LEFT_WIDTH - 18 - TOPIC_INDENT, TOPIC_BUTTON_HEIGHT, selected ? new Vector4f(0.10F, 0.26F, 0.44F, 0.95F) : new Vector4f(0.06F, 0.08F, 0.13F, 0.30F));
			rowBackground.onInit();
			rowBackground.setPos(TOPIC_INDENT, y, 0);
			topicsContent.attach(rowBackground);

			GUITextButton topicButton = new GUITextButton(getState(), LEFT_WIDTH - 18 - TOPIC_INDENT, TOPIC_BUTTON_HEIGHT, selected ? GUITextButton.ColorPalette.FRIENDLY : GUITextButton.ColorPalette.NEUTRAL, topic.getTitle(), new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if(event.pressedLeftMouse()) {
						selectTopic(topic);
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			});
			topicButton.setTextPos(10, 4);
			topicButton.setMouseUpdateEnabled(true);
			topicButton.setPos(TOPIC_INDENT, y, 0);
			topicButton.onInit();
			topicsContent.attach(topicButton);
			return TOPIC_BUTTON_HEIGHT + TOPIC_BUTTON_GAP;
		}

		private void selectTopic(DocTopic topic) {
			selectedTopic = topic;
			// Auto-expand the section if the selected topic is in a collapsed one
			if(topic != null) {
				collapsedSections.remove(topic.getSectionKey());
			}
			rebuildTopicButtons();
			rebuildRenderedContent();
			if(contentScrollPanel != null) {
				contentScrollPanel.scrollVerticalPercent(0.0F);
			}
		}

		private void rebuildRenderedContent() {
			if(contentBlocks == null) {
				return;
			}

			contentBlocks.detachAll();
			if(selectedTopic == null) {
				GUITextOverlay emptyOverlay = new GUITextOverlay(400, 24, FontLibrary.FontSize.BIG, getState());
				emptyOverlay.setTextSimple("No documentation topic selected");
				emptyOverlay.onInit();
				emptyOverlay.setPos(12, 12, 0);
				contentBlocks.attach(emptyOverlay);
				contentBlocks.setHeight(60.0F);
				return;
			}

			List<MarkdownDocRenderer.RenderedBlock> blocks = MarkdownDocRenderer.render(selectedTopic.getMarkdown());
			int y = 12;
			int width = Math.max(320, (int) contentBlocks.getWidth() - 24);

			for(MarkdownDocRenderer.RenderedBlock block : blocks) {
				y += addRenderedBlock(width, y, block);
			}

			contentBlocks.setHeight(Math.max(y + 12.0F, contentScrollPanel.getHeight()));
		}

		private int addRenderedBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			switch(block.getType()) {
				case CODE:
					return addCodeBlock(width, y, block);
				case SEPARATOR:
					return addSeparatorBlock(width, y);
				case HEADING_1:
				case HEADING_2:
				case HEADING_3:
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				default:
					return addInlineBlock(width, y, block);
			}
		}

		private int addCodeBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			UnicodeFont font = getFontForBlock(block.getType());
			GUITextOverlay overlay = new GUITextOverlay(width, 10, font, getState());
			overlay.setTextSimple(block.getText());
			overlay.autoWrapOn = contentBlocks;
			overlay.onInit();
			overlay.updateTextSize();
			overlay.setHeight(overlay.getTextHeight());

			GUIColoredRectangle codeBackground = new GUIColoredRectangle(getState(), width + 8, overlay.getTextHeight() + 10, new Vector4f(0.08F, 0.10F, 0.15F, 0.80F));
			codeBackground.onInit();
			codeBackground.setPos(8, y - 3, 0);
			contentBlocks.attach(codeBackground);

			overlay.setColor(0.95F, 0.95F, 0.95F, 1.0F);
			overlay.setPos(CONTENT_MARGIN + 4, y + 2, 0);
			contentBlocks.attach(overlay);
			return overlay.getTextHeight() + 18;
		}

		private int addSeparatorBlock(int width, int y) {
			GUIColoredRectangle separator = new GUIColoredRectangle(getState(), width, 2, new Vector4f(0.35F, 0.48F, 0.65F, 0.90F));
			separator.onInit();
			separator.setPos(CONTENT_MARGIN, y + 4, 0);
			contentBlocks.attach(separator);
			return 12;
		}

		private int addInlineBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			int startX = CONTENT_MARGIN;
			int maxX = CONTENT_MARGIN + width;
			int currentX = startX;
			int currentY = y;
			int lineHeight = getDefaultLineHeight(block.getType());
			int blockSpacing = getBlockSpacing(block.getType());

			for(InlineToken token : tokenizeSegments(block.getSegments())) {
				if(token.lineBreak) {
					currentY += lineHeight + 4;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					continue;
				}

				UnicodeFont font = getFontForInlineSegment(block.getType(), token.style);
				int textWidth = Math.max(0, font.getWidth(token.text));
				int advanceWidth = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? textWidth + (INLINE_CODE_PADDING_X * 2) + 2 : textWidth;
				if(currentX > startX && currentX + advanceWidth > maxX) {
					currentY += lineHeight + 4;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					if(token.whitespace) {
						continue;
					}
				}

				if(token.whitespace) {
					if(currentX > startX) {
						currentX += textWidth;
					}
					continue;
				}

				GUITextOverlay overlay = new GUITextOverlay(Math.max(12, textWidth + 8), 10, font, getState());
				overlay.setTextSimple(token.text);
				overlay.onInit();
				overlay.updateTextSize();
				int overlayHeight = Math.max(getDefaultLineHeight(block.getType()), overlay.getTextHeight());

				if(token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
					GUIColoredRectangle inlineCodeBackground = new GUIColoredRectangle(getState(), textWidth + (INLINE_CODE_PADDING_X * 2), overlayHeight + (INLINE_CODE_PADDING_Y * 2), new Vector4f(0.12F, 0.15F, 0.23F, 0.95F));
					inlineCodeBackground.onInit();
					inlineCodeBackground.setPos(currentX - 1, currentY - 1, 0);
					contentBlocks.attach(inlineCodeBackground);
					overlay.setPos(currentX + INLINE_CODE_PADDING_X - 1, currentY + INLINE_CODE_PADDING_Y - 1, 0);
				} else {
					overlay.setPos(currentX, currentY, 0);
				}

				Vector4f color = getInlineColor(block.getType(), token.style);
				overlay.setColor(color.x, color.y, color.z, color.w);
				contentBlocks.attach(overlay);

				currentX += advanceWidth;
				lineHeight = Math.max(lineHeight, overlayHeight + (token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? INLINE_CODE_PADDING_Y : 0));
			}

			return (currentY - y) + lineHeight + blockSpacing;
		}

		private List<InlineToken> tokenizeSegments(List<MarkdownDocRenderer.InlineSegment> segments) {
			List<InlineToken> tokens = new ArrayList<>();
			for(MarkdownDocRenderer.InlineSegment segment : segments) {
				String text = segment.getText();
				if(text == null || text.isEmpty()) {
					continue;
				}

				if(segment.getStyle() == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
					appendInlineCodeTokens(tokens, text, segment.getStyle());
				} else {
					appendWrappedTokens(tokens, text, segment.getStyle());
				}
			}
			return tokens;
		}

		private void appendInlineCodeTokens(List<InlineToken> tokens, String text, MarkdownDocRenderer.InlineStyle style) {
			String[] lines = text.split("\\n", -1);
			for(int i = 0; i < lines.length; i++) {
				if(!lines[i].isEmpty()) {
					tokens.add(new InlineToken(lines[i], style, false, false));
				}
				if(i < lines.length - 1) {
					tokens.add(new InlineToken("", style, false, true));
				}
			}
		}

		private void appendWrappedTokens(List<InlineToken> tokens, String text, MarkdownDocRenderer.InlineStyle style) {
			StringBuilder current = new StringBuilder();
			Boolean whitespace = null;
			for(int i = 0; i < text.length(); i++) {
				char character = text.charAt(i);
				if(character == '\n') {
					flushInlineToken(tokens, current, style, whitespace != null && whitespace);
					tokens.add(new InlineToken("", style, false, true));
					whitespace = null;
					continue;
				}

				boolean isWhitespace = Character.isWhitespace(character);
				if(whitespace != null && whitespace != isWhitespace) {
					flushInlineToken(tokens, current, style, whitespace);
				}

				current.append(character);
				whitespace = isWhitespace;
			}
			flushInlineToken(tokens, current, style, whitespace != null && whitespace);
		}

		private void flushInlineToken(List<InlineToken> tokens, StringBuilder current, MarkdownDocRenderer.InlineStyle style, boolean whitespace) {
			if(current.length() == 0) {
				return;
			}
			tokens.add(new InlineToken(current.toString(), style, whitespace, false));
			current.setLength(0);
		}

		private int getDefaultLineHeight(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return 34;
				case HEADING_2:
					return 28;
				case HEADING_3:
					return 24;
				case CODE:
					return 18;
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				default:
					return 18;
			}
		}

		private int getBlockSpacing(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return 18;
				case HEADING_2:
					return 14;
				case HEADING_3:
					return 12;
				case BULLET:
				case ORDERED:
					return 8;
				case PARAGRAPH:
				default:
					return 10;
			}
		}

		private UnicodeFont getFontForInlineSegment(MarkdownDocRenderer.BlockType blockType, MarkdownDocRenderer.InlineStyle style) {
			if(style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
				return FontLibrary.getCourierNew12White();
			}

			switch(blockType) {
				case HEADING_1:
					return FontLibrary.getBoldArial30WhiteNoOutline();
				case HEADING_2:
					return FontLibrary.getBoldArial24WhiteNoOutline();
				case HEADING_3:
					return FontLibrary.getBoldArial20WhiteNoOutline();
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				default:
					if(style == MarkdownDocRenderer.InlineStyle.BOLD || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) {
						return FontLibrary.getBoldArial14White();
					}
					return FontLibrary.getRegularArial13White();
			}
		}

		private Vector4f getInlineColor(MarkdownDocRenderer.BlockType blockType, MarkdownDocRenderer.InlineStyle style) {
			switch(blockType) {
				case HEADING_1:
					return style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.92F, 0.97F, 1.0F, 1.0F) : new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
				case HEADING_2:
					return style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.90F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.95F, 0.95F, 1.0F, 1.0F);
				case HEADING_3:
					return style == MarkdownDocRenderer.InlineStyle.ITALIC || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC ? new Vector4f(0.88F, 0.96F, 1.0F, 1.0F) : new Vector4f(0.90F, 0.95F, 1.0F, 1.0F);
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				default:
					if(style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
						return new Vector4f(0.97F, 0.97F, 0.97F, 1.0F);
					}
					if(style == MarkdownDocRenderer.InlineStyle.BOLD || style == MarkdownDocRenderer.InlineStyle.BOLD_ITALIC) {
						return new Vector4f(0.98F, 0.98F, 0.98F, 1.0F);
					}
					if(style == MarkdownDocRenderer.InlineStyle.ITALIC) {
						return new Vector4f(0.84F, 0.92F, 1.0F, 1.0F);
					}
					return new Vector4f(0.88F, 0.88F, 0.88F, 1.0F);
			}
		}

		private UnicodeFont getFontForBlock(MarkdownDocRenderer.BlockType type) {
			switch(type) {
				case HEADING_1:
					return FontLibrary.getBoldArial30WhiteNoOutline();
				case HEADING_2:
					return FontLibrary.getBoldArial24WhiteNoOutline();
				case HEADING_3:
					return FontLibrary.getBoldArial20WhiteNoOutline();
				case CODE:
					return FontLibrary.getCourierNew12White();
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				case SEPARATOR:
				default:
					return FontLibrary.FontSize.MEDIUM.getFont();
			}
		}

		private static final class InlineToken {
			private final String text;
			private final MarkdownDocRenderer.InlineStyle style;
			private final boolean whitespace;
			private final boolean lineBreak;

			private InlineToken(String text, MarkdownDocRenderer.InlineStyle style, boolean whitespace, boolean lineBreak) {
				this.text = text;
				this.style = style;
				this.whitespace = whitespace;
				this.lineBreak = lineBreak;
			}
		}
	}
}

