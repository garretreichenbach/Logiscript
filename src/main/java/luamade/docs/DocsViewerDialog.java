package luamade.docs;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
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

	public DocsViewerDialog() {
		super(GameClient.getClientState());
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
	}

	public static class DocsPanel extends GUIInputDialogPanel {

		private static final int WINDOW_WIDTH = 1280;
		private static final int WINDOW_HEIGHT = 760;
		private static final int LEFT_WIDTH = 280;
		private static final int PADDING = 12;
		private static final int SEARCH_HEIGHT = 24;
		private static final int TOPIC_BUTTON_HEIGHT = 28;
		private static final int TOPIC_BUTTON_GAP = 4;

		private final List<DocTopic> allTopics = new ArrayList<>(DocsRepository.getTopics());
		private final List<DocTopic> filteredTopics = new ArrayList<>();
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

		public DocsPanel(InputState inputState, GUICallback guiCallback) {
			super(inputState, "LUAMADE_DOCS", "LuaMade Documentation", "", WINDOW_WIDTH, WINDOW_HEIGHT, guiCallback);
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();

			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIElement root = contentPane.getContent(0);

			searchAnchor = new GUIAncor(getState(), LEFT_WIDTH - (PADDING * 2), SEARCH_HEIGHT);
			root.attach(searchAnchor);

			searchBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 80, 1, "Search topics", searchAnchor, new TextCallback() {
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

			topicsScrollPanel = new GUIScrollablePanel(LEFT_WIDTH, WINDOW_HEIGHT - 80, root, getState());
			topicsScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			topicsContent = new GUIAncor(getState(), LEFT_WIDTH - 12, WINDOW_HEIGHT - 80);
			topicsScrollPanel.setContent(topicsContent);
			root.attach(topicsScrollPanel);

			emptyTopicsOverlay = new GUITextOverlay(LEFT_WIDTH - 24, 18, FontLibrary.FontSize.MEDIUM, getState());
			emptyTopicsOverlay.setTextSimple("No matching topics");
			emptyTopicsOverlay.onInit();

			contentBackground = new GUIColoredRectangle(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, new Vector4f(0.05F, 0.06F, 0.09F, 0.35F));
			contentBackground.onInit();
			root.attach(contentBackground);

			contentScrollPanel = new GUIScrollablePanel(WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44, root, getState());
			contentScrollPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			contentBlocks = new GUIAncor(getState(), WINDOW_WIDTH - LEFT_WIDTH - (PADDING * 3), WINDOW_HEIGHT - 44);
			contentScrollPanel.setContent(contentBlocks);
			root.attach(contentScrollPanel);

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

			float contentHeight = Math.max(120.0F, getHeight() - 58.0F);
			float leftHeight = Math.max(120.0F, contentHeight - SEARCH_HEIGHT - 12.0F);
			float rightX = LEFT_WIDTH + (PADDING * 2);
			float rightWidth = Math.max(300.0F, getWidth() - rightX - PADDING);

			searchAnchor.setWidth(LEFT_WIDTH - (PADDING * 2));
			searchAnchor.setHeight(SEARCH_HEIGHT);
			searchAnchor.setPos(PADDING, 6.0F, 0.0F);
			searchBar.setPos(PADDING, 6.0F, 0.0F);

			topicsBackground.setWidth(LEFT_WIDTH);
			topicsBackground.setHeight(leftHeight);
			topicsBackground.setPos(PADDING, SEARCH_HEIGHT + 16.0F, 0.0F);
			topicsScrollPanel.setWidth(LEFT_WIDTH);
			topicsScrollPanel.setHeight(leftHeight);
			topicsScrollPanel.setPos(PADDING, SEARCH_HEIGHT + 16.0F, 0.0F);
			topicsContent.setWidth(LEFT_WIDTH - 12);

			contentBackground.setWidth(rightWidth);
			contentBackground.setHeight(contentHeight);
			contentBackground.setPos(rightX, 6.0F, 0.0F);
			contentScrollPanel.setWidth(rightWidth);
			contentScrollPanel.setHeight(contentHeight);
			contentScrollPanel.setPos(rightX, 6.0F, 0.0F);
			contentBlocks.setWidth(rightWidth - 12);
		}

		private void filterTopics() {
			filteredTopics.clear();
			String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
			for(DocTopic topic : allTopics) {
				if(normalizedQuery.isEmpty() || topic.getTitle().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
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

			for(DocTopic topic : filteredTopics) {
				GUITextButton topicButton = new GUITextButton(getState(), LEFT_WIDTH - 18, TOPIC_BUTTON_HEIGHT,
						topic.equals(selectedTopic) ? GUITextButton.ColorPalette.OK : GUITextButton.ColorPalette.TRANSPARENT,
						topic.getTitle(), new GUICallback() {
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
				topicButton.setTextPos(8, 4);
				topicButton.setPos(0, y, 0);
				topicButton.onInit();
				topicsContent.attach(topicButton);
				y += TOPIC_BUTTON_HEIGHT + TOPIC_BUTTON_GAP;
			}

			topicsContent.setHeight(Math.max(40.0F, y));
		}

		private void selectTopic(DocTopic topic) {
			selectedTopic = topic;
			rebuildTopicButtons();
			rebuildRenderedContent();
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
			UnicodeFont font = getFontForBlock(block.getType());
			GUITextOverlay overlay = new GUITextOverlay(width, 10, font, getState());
			overlay.setTextSimple(block.getText());
			overlay.autoWrapOn = contentBlocks;
			overlay.onInit();
			overlay.updateTextSize();
			overlay.setHeight(overlay.getTextHeight());

			int x = 12;
			int totalHeight = overlay.getTextHeight();

			switch(block.getType()) {
				case HEADING_1:
					overlay.setColor(1.0F, 1.0F, 1.0F, 1.0F);
					overlay.setPos(x, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 18;
				case HEADING_2:
					overlay.setColor(0.95F, 0.95F, 1.0F, 1.0F);
					overlay.setPos(x, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 14;
				case HEADING_3:
					overlay.setColor(0.90F, 0.95F, 1.0F, 1.0F);
					overlay.setPos(x, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 12;
				case CODE:
					GUIColoredRectangle codeBackground = new GUIColoredRectangle(getState(), width + 8, totalHeight + 10, new Vector4f(0.08F, 0.10F, 0.15F, 0.80F));
					codeBackground.onInit();
					codeBackground.setPos(8, y - 3, 0);
					contentBlocks.attach(codeBackground);
					overlay.setColor(0.95F, 0.95F, 0.95F, 1.0F);
					overlay.setPos(x + 4, y + 2, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 18;
				case SEPARATOR:
					overlay.setColor(0.55F, 0.65F, 0.80F, 1.0F);
					overlay.setPos(x, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 10;
				case BULLET:
				case ORDERED:
					overlay.setColor(0.92F, 0.92F, 0.92F, 1.0F);
					overlay.setPos(x + 8, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 8;
				case PARAGRAPH:
				default:
					overlay.setColor(0.88F, 0.88F, 0.88F, 1.0F);
					overlay.setPos(x, y, 0);
					contentBlocks.attach(overlay);
					return totalHeight + 10;
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
	}
}

