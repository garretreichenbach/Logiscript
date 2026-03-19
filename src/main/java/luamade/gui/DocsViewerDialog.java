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
		docsPanel = new DocsPanel(getState(), this, reopenComputerModule);
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
		private static final int TOP_LEFT_BUTTON_GAP = 8;
		private static final int COLLAPSE_ALL_BUTTON_WIDTH = 110;
		private static final int COLLAPSE_ALL_BUTTON_HEIGHT = SEARCH_HEIGHT;
		private static final int SECTION_HEADER_HEIGHT = 22;
		private static final int SECTION_HEADER_GAP = 4;
		private static final int TOPIC_BUTTON_HEIGHT = 28;
		private static final int TOPIC_BUTTON_GAP = 4;
		private static final int TOPIC_INDENT = 10;
		private static final int CONTENT_MARGIN = 12;
		private static final int INLINE_CODE_PADDING_X = 4;
		private static final int INLINE_CODE_PADDING_Y = 1;
		private static final int CODE_BLOCK_PADDING_X = 8;
		private static final int CODE_BLOCK_PADDING_Y = 3;
		private static final int SCROLLBAR_WIDTH = 16;
		private static final int RIGHT_PANEL_MARGIN = 14;
		private static final int TOPICS_CONTENT_HORIZONTAL_PADDING = 4;
		private static final int CODE_TAB_WIDTH = 12;
		private static final int TOPICS_SCROLL_BOTTOM_CLAMP = 12;
		private static final int CONTENT_SCROLL_BOTTOM_CLAMP = 18;

		private final List<DocTopic> allTopics = new ArrayList<>(DocsRepository.getTopics());
		private final List<DocTopic> filteredTopics = new ArrayList<>();
		private final java.util.Set<String> collapsedSections = new java.util.HashSet<>();
		private final ComputerModule computerModule;
		private DocTopic selectedTopic;
		private String searchQuery = "";

		private GUIAncor searchAnchor;
		private GUIActivatableTextBar searchBar;
		private GUITextButton collapseAllButton;
		private GUIScrollablePanel topicsScrollPanel;
		private GUIAncor topicsContent;
		private GUIAncor topicsPane;
		private GUIScrollablePanel contentScrollPanel;
		private GUIAncor contentBlocks;
		private GUIAncor contentPane;
		private GUITextOverlay emptyTopicsOverlay;
		private GUIContentPane mainContentPane;
		private GUIAncor rootContentPane;

		public DocsPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "LUAMADE_DOCS", "LuaMade Documentation", "", WINDOW_WIDTH, WINDOW_HEIGHT, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();

			mainContentPane = ((GUIDialogWindow) background).getMainContentPane();
			rootContentPane = mainContentPane.getContent(0);
			GUIElement root = rootContentPane;

			searchAnchor = new GUIAncor(getState(), LEFT_WIDTH - (PADDING * 2), SEARCH_HEIGHT);
			root.attach(searchAnchor);

			collapseAllButton = new GUITextButton(getState(), COLLAPSE_ALL_BUTTON_WIDTH, COLLAPSE_ALL_BUTTON_HEIGHT, GUITextButton.ColorPalette.NEUTRAL, new Object() {
				@Override
				public String toString() {
					return areAllSectionsCollapsed() ? "Expand All" : "Collapse All";
				}
			}, new GUICallback() {
				@Override
				public void callback(GUIElement callingGuiElement, MouseEvent event) {
					if(event.pressedLeftMouse()) {
						toggleAllSections();
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			});
			collapseAllButton.setMouseUpdateEnabled(true);
			collapseAllButton.setTextPos(8, 4);
			collapseAllButton.onInit();
			root.attach(collapseAllButton);

			searchBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Search", searchAnchor, new TextCallback() {
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

			topicsPane = new GUIAncor(getState(), LEFT_WIDTH, WINDOW_HEIGHT - 80);
			root.attach(topicsPane);

			// Parent the scroll panel to its pane background so scrollbar math stays local to this pane.
			topicsScrollPanel = new GUIScrollablePanel(LEFT_WIDTH, WINDOW_HEIGHT - 80, topicsPane, getState());
			topicsScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			topicsContent = new GUIAncor(getState(), LEFT_WIDTH - 12, WINDOW_HEIGHT - 80);
			topicsScrollPanel.setContent(topicsContent);
			topicsPane.attach(topicsScrollPanel);

			emptyTopicsOverlay = new GUITextOverlay(LEFT_WIDTH - 24, 18, FontLibrary.FontSize.MEDIUM, getState());
			emptyTopicsOverlay.setTextSimple("No matching topics");
			emptyTopicsOverlay.onInit();

			contentPane = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			root.attach(contentPane);

			// Parent the right scroll panel to its background to keep scrollbar inside the right pane bounds.
			contentScrollPanel = new GUIScrollablePanel(WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, contentPane, getState());
			contentScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			contentBlocks = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			contentScrollPanel.setContent(contentBlocks);
			contentPane.attach(contentScrollPanel);

			filterTopics();
			selectedTopic = resolveInitialTopic();
			rebuildTopicButtons();
			selectTopic(selectedTopic != null ? selectedTopic : (filteredTopics.isEmpty() ? null : filteredTopics.get(0)));
			layoutComponents();
		}

		private DocTopic resolveInitialTopic() {
			if(computerModule == null) {
				return null;
			}

			String lastTopicPath = computerModule.getLastDocsTopicPath();
			if(lastTopicPath == null || lastTopicPath.trim().isEmpty()) {
				return null;
			}

			for(DocTopic topic : allTopics) {
				if(topic != null && lastTopicPath.equals(topic.getResourcePath())) {
					return topic;
				}
			}

			return null;
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
			float availableWidth = rootContentPane != null && rootContentPane.getWidth() > 0 ? rootContentPane.getWidth() : getWidth();
			float availableHeight = rootContentPane != null && rootContentPane.getHeight() > 0 ? rootContentPane.getHeight() : getHeight();

			float contentHeight = Math.max(120.0F, availableHeight - 20.0F);
			float leftHeight = Math.max(120.0F, contentHeight - SEARCH_HEIGHT - 16.0F);
			float leftScrollHeight = Math.max(80.0F, leftHeight - TOPICS_SCROLL_BOTTOM_CLAMP);
			float rightX = LEFT_WIDTH + (PADDING * 2);
			// RIGHT_PANEL_MARGIN keeps the scroll panel (including its scrollbar) away from the dialog's right chrome
			float rightWidth = Math.max(300.0F, availableWidth - rightX - RIGHT_PANEL_MARGIN);
			float contentScrollHeight = Math.max(80.0F, contentHeight - CONTENT_SCROLL_BOTTOM_CLAMP);

			float searchX = PADDING + COLLAPSE_ALL_BUTTON_WIDTH + TOP_LEFT_BUTTON_GAP;
			float searchWidth = Math.max(80.0F, LEFT_WIDTH - (PADDING * 2) - COLLAPSE_ALL_BUTTON_WIDTH - TOP_LEFT_BUTTON_GAP);

			collapseAllButton.setWidth(COLLAPSE_ALL_BUTTON_WIDTH);
			collapseAllButton.setHeight(COLLAPSE_ALL_BUTTON_HEIGHT);
			collapseAllButton.setPos(PADDING, 6.0F, 0.0F);

			searchAnchor.setWidth(searchWidth);
			searchAnchor.setHeight(SEARCH_HEIGHT);
			searchAnchor.setPos(searchX, 6.0F, 0.0F);
			searchBar.setPos(searchX, 6.0F, 0.0F);

			topicsPane.setWidth(LEFT_WIDTH);
			topicsPane.setHeight(leftHeight);
			topicsPane.setPos(PADDING, SEARCH_HEIGHT + 16.0F, 0.0F);
			topicsScrollPanel.setWidth(LEFT_WIDTH);
			topicsScrollPanel.setHeight(leftScrollHeight);
			// Scroll panels are now children of pane backgrounds.
			topicsScrollPanel.setPos(0.0F, 0.0F, 0.0F);
			// Leave room for the topics scrollbar.
			topicsContent.setWidth(LEFT_WIDTH - SCROLLBAR_WIDTH - (TOPIC_INDENT * 2) - TOPICS_CONTENT_HORIZONTAL_PADDING);

			contentPane.setWidth(rightWidth);
			contentPane.setHeight(contentHeight);
			contentPane.setPos(rightX, 6.0F, 0.0F);
			contentScrollPanel.setWidth(rightWidth);
			contentScrollPanel.setHeight(contentScrollHeight);
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

		private void collapseAllSections() {
			collapsedSections.clear();
			collapsedSections.addAll(getAllSectionKeys());
			rebuildTopicButtons();
		}

		private void expandAllSections() {
			collapsedSections.clear();
			rebuildTopicButtons();
		}

		private void toggleAllSections() {
			if(areAllSectionsCollapsed()) {
				expandAllSections();
			} else {
				collapseAllSections();
			}
		}

		private boolean areAllSectionsCollapsed() {
			java.util.Set<String> sectionKeys = getAllSectionKeys();
			return !sectionKeys.isEmpty() && collapsedSections.containsAll(sectionKeys);
		}

		private java.util.Set<String> getAllSectionKeys() {
			java.util.Set<String> sectionKeys = new java.util.HashSet<>();
			for(DocTopic topic : allTopics) {
				if(topic != null && topic.getSectionKey() != null) {
					sectionKeys.add(topic.getSectionKey());
				}
			}
			return sectionKeys;
		}

		private int addSectionHeader(int y, String sectionKey, String sectionLabel) {
			boolean collapsed = collapsedSections.contains(sectionKey);
			String arrow = collapsed ? "\u25BA  " : "\u25BC  ";  // ► / ▼
			int rowWidth = Math.max(120, (int) topicsContent.getWidth());

			GUITextButton headerButton = new GUITextButton(getState(), rowWidth, SECTION_HEADER_HEIGHT, GUITextButton.ColorPalette.TUTORIAL, arrow + sectionLabel.toUpperCase(), new GUICallback() {
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
			headerButton.setPos(TOPIC_INDENT, y, 0);
			headerButton.onInit();
			topicsContent.attach(headerButton);
			return SECTION_HEADER_HEIGHT + SECTION_HEADER_GAP;
		}

		private int addTopicButton(int y, DocTopic topic) {
			boolean selected = topic.equals(selectedTopic);
			int rowWidth = Math.max(120, (int) topicsContent.getWidth());
			String label = isIndexTopic(topic) ? "Overview: " + topic.getTitle() : topic.getTitle();
			GUITextButton.ColorPalette palette = selected ? (isIndexTopic(topic) ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.FRIENDLY) : GUITextButton.ColorPalette.NEUTRAL;

			GUITextButton topicButton = new GUITextButton(getState(), rowWidth, TOPIC_BUTTON_HEIGHT, palette, label, new GUICallback() {
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

		private boolean isIndexTopic(DocTopic topic) {
			if(topic == null || topic.getResourcePath() == null) {
				return false;
			}
			return topic.getResourcePath().toLowerCase(Locale.ROOT).endsWith("/index.md");
		}

		private void selectTopic(DocTopic topic) {
			selectedTopic = topic;
			if(computerModule != null) {
				computerModule.setLastDocsTopicPath(topic == null ? "" : topic.getResourcePath());
			}
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
			int width = Math.max(280, (int) contentBlocks.getWidth() - (CONTENT_MARGIN * 2));

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
					return addSeparatorBlock();
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
			overlay.setTextSimple(formatCodeTextForDisplay(block.getText()));
			overlay.autoWrapOn = contentBlocks;
			overlay.onInit();
			overlay.updateTextSize();
			overlay.setHeight(overlay.getTextHeight());

			int textHeight = Math.max(1, overlay.getTextHeight());
			int blockHeight = textHeight + (CODE_BLOCK_PADDING_Y * 2);
			GUIColoredRectangle codeBackground = new GUIColoredRectangle(getState(), width + CODE_BLOCK_PADDING_X, blockHeight, new Vector4f(0.08F, 0.10F, 0.15F, 0.80F));
			codeBackground.onInit();
			codeBackground.setPos(CONTENT_MARGIN - 4, y, 0);
			contentBlocks.attach(codeBackground);

			overlay.setColor(0.95F, 0.95F, 0.95F, 1.0F);
			overlay.setPos(CONTENT_MARGIN + 2, y + CODE_BLOCK_PADDING_Y, 0);
			contentBlocks.attach(overlay);
			return blockHeight + 8;
		}

		private int addSeparatorBlock() {
			return 10;
		}

		private int addInlineBlock(int width, int y, MarkdownDocRenderer.RenderedBlock block) {
			int startX = CONTENT_MARGIN;
			int maxX = CONTENT_MARGIN + width;
			int lineMaxWidth = Math.max(48, maxX - startX);
			int currentX = startX;
			int currentY = y;
			int lineHeight = getDefaultLineHeight(block.getType());
			int blockSpacing = getBlockSpacing(block.getType());

			for(InlineToken token : tokenizeSegments(block.getSegments())) {
				if(token.lineBreak) {
					currentY += lineHeight + 3;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					continue;
				}

				UnicodeFont font = getFontForInlineSegment(block.getType(), token.style);
				String renderedText = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? formatCodeTextForDisplay(token.text) : token.text;
				int textWidth = Math.max(0, font.getWidth(renderedText));
				int advanceWidth = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? textWidth + (INLINE_CODE_PADDING_X * 2) + 2 : textWidth;
				if(currentX > startX && currentX + advanceWidth > maxX) {
					currentY += lineHeight + 3;
					currentX = startX;
					lineHeight = getDefaultLineHeight(block.getType());
					if(token.whitespace) {
						continue;
					}
				}

				if(token.whitespace) {
					if(currentX > startX) {
						if(currentX + textWidth > maxX) {
							currentY += lineHeight + 3;
							currentX = startX;
							lineHeight = getDefaultLineHeight(block.getType());
							continue;
						}
						currentX += textWidth;
					}
					continue;
				}
				int tokenMaxWidth = lineMaxWidth - (token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? (INLINE_CODE_PADDING_X * 2) + 2 : 0);
				List<String> tokenChunks = splitTokenForWidth(renderedText, font, tokenMaxWidth);
				for(int chunkIndex = 0; chunkIndex < tokenChunks.size(); chunkIndex++) {
					String chunk = tokenChunks.get(chunkIndex);
					int chunkWidth = Math.max(0, font.getWidth(chunk));
					int chunkAdvanceWidth = token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE ? chunkWidth + (INLINE_CODE_PADDING_X * 2) + 2 : chunkWidth;

					if(currentX > startX && currentX + chunkAdvanceWidth > maxX) {
						currentY += lineHeight + 3;
						currentX = startX;
						lineHeight = getDefaultLineHeight(block.getType());
					}

					GUITextOverlay overlay = new GUITextOverlay(Math.max(12, chunkWidth + 8), 10, font, getState());
					overlay.setTextSimple(chunk);
					overlay.onInit();
					overlay.updateTextSize();
					int overlayHeight = Math.max(getDefaultLineHeight(block.getType()), overlay.getTextHeight());

					if(token.style == MarkdownDocRenderer.InlineStyle.INLINE_CODE) {
						int inlineCodeHeight = overlay.getTextHeight() + (INLINE_CODE_PADDING_Y * 2);
						GUIColoredRectangle inlineCodeBackground = new GUIColoredRectangle(getState(), chunkWidth + (INLINE_CODE_PADDING_X * 2), inlineCodeHeight, new Vector4f(0.12F, 0.15F, 0.23F, 0.95F));
						inlineCodeBackground.onInit();
						inlineCodeBackground.setPos(currentX - 1, currentY, 0);
						contentBlocks.attach(inlineCodeBackground);
						overlay.setPos(currentX + INLINE_CODE_PADDING_X - 1, currentY + INLINE_CODE_PADDING_Y, 0);
						overlayHeight = inlineCodeHeight;
					} else {
						overlay.setPos(currentX, currentY, 0);
					}

					Vector4f color = getInlineColor(block.getType(), token.style);
					overlay.setColor(color.x, color.y, color.z, color.w);
					contentBlocks.attach(overlay);

					currentX += chunkAdvanceWidth;
					lineHeight = Math.max(lineHeight, overlayHeight);

					if(chunkIndex < tokenChunks.size() - 1) {
						currentY += lineHeight + 3;
						currentX = startX;
						lineHeight = getDefaultLineHeight(block.getType());
					}
				}
			}

			return (currentY - y) + lineHeight + blockSpacing;
		}

		private List<String> splitTokenForWidth(String text, UnicodeFont font, int maxWidth) {
			List<String> chunks = new ArrayList<>();
			if(text == null || text.isEmpty()) {
				return chunks;
			}

			if(font.getWidth(text) <= maxWidth) {
				chunks.add(text);
				return chunks;
			}

			StringBuilder current = new StringBuilder();
			int currentWidth = 0;
			for(int i = 0; i < text.length(); i++) {
				char character = text.charAt(i);
				String asString = String.valueOf(character);
				int characterWidth = Math.max(1, font.getWidth(asString));

				if(current.length() > 0 && currentWidth + characterWidth > maxWidth) {
					chunks.add(current.toString());
					current.setLength(0);
					currentWidth = 0;
				}

				current.append(character);
				currentWidth += characterWidth;
			}

			if(current.length() > 0) {
				chunks.add(current.toString());
			}
			return chunks;
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
					return 16;
				case BULLET:
				case ORDERED:
				case PARAGRAPH:
				default:
					return 16;
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
					return 6;
				case PARAGRAPH:
				default:
					return 8;
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

		private String formatCodeTextForDisplay(String text) {
			if(text == null || text.isEmpty()) {
				return "";
			}

			String normalized = text.replace("\r", "");
			String[] lines = normalized.split("\\n", -1);
			StringBuilder builder = new StringBuilder(normalized.length() + 16);

			for(int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
				String line = lines[lineIndex];
				int i = 0;
				while(i < line.length()) {
					char character = line.charAt(i);
					if(character == ' ') {
						builder.append('\u00A0');
						i++;
						continue;
					}
					if(character == '\t') {
						for(int tab = 0; tab < CODE_TAB_WIDTH; tab++) {
							builder.append('\u00A0');
						}
						i++;
						continue;
					}
					break;
				}

				for(; i < line.length(); i++) {
					char character = line.charAt(i);
					if(character == '\t') {
						for(int tab = 0; tab < CODE_TAB_WIDTH; tab++) {
							builder.append(' ');
						}
					} else {
						builder.append(character);
					}
				}

				if(lineIndex < lines.length - 1) {
					builder.append('\n');
				}
			}

			return builder.toString();
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

