package luamade.gui.editor;

import luamade.LuaMade;
import luamade.lua.fs.FileSystem;
import luamade.lua.terminal.Terminal;
import luamade.manager.ConfigManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Swing-based code editor using RSyntaxTextArea. Reads and writes purely
 * from the mod's virtual filesystem — no files are written to the user's disk.
 */
public class SwingEditorFrame extends JFrame {

	private static final Map<String, SwingEditorFrame> openEditors = new ConcurrentHashMap<>();

	private final RSyntaxTextArea textArea;
	private final String virtualPath;
	private final FileSystem fileSystem;
	private final Terminal terminal;
	private boolean dirty;

	private SwingEditorFrame(String virtualPath, String initialContent, FileSystem fileSystem, Terminal terminal) {
		super("Edit: " + virtualPath);
		this.virtualPath = virtualPath;
		this.fileSystem = fileSystem;
		this.terminal = terminal;
		dirty = false;

		textArea = new RSyntaxTextArea(30, 100);
		textArea.setSyntaxEditingStyle(chooseSyntaxStyle(virtualPath));
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		textArea.setAutoIndentEnabled(true);
		textArea.setBracketMatchingEnabled(true);
		textArea.setTabSize(4);
		textArea.setTabsEmulated(true);
		textArea.setText(initialContent);
		textArea.setCaretPosition(0);
		textArea.discardAllEdits();
		applyTheme(ConfigManager.isEditorDarkTheme());
		applyFontSize(ConfigManager.getEditorFontSize());

		textArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				markDirty();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				markDirty();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				markDirty();
			}
		});

		RTextScrollPane scrollPane = new RTextScrollPane(textArea);
		scrollPane.setLineNumbersEnabled(true);
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		setJMenuBar(buildMenuBar());
		setupKeyBindings();
		setupZoomMouseWheel(scrollPane);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				handleClose();
			}
		});

		setSize(900, 650);
		setLocationRelativeTo(null);
	}

	/**
	 * Opens a file in the Swing editor. If the file is already open, brings
	 * that window to the front.
	 *
	 * @return true if the editor was opened or focused successfully
	 */
	public static boolean open(String virtualPath, String content, FileSystem fileSystem, Terminal terminal) {
		SwingEditorFrame existing = openEditors.get(virtualPath);
		if(existing != null) {
			SwingUtilities.invokeLater(() -> {
				existing.toFront();
				existing.requestFocus();
			});
			return true;
		}

		try {
			SwingUtilities.invokeLater(() -> {
				try {
					SwingEditorFrame frame = new SwingEditorFrame(virtualPath, content, fileSystem, terminal);
					openEditors.put(virtualPath, frame);
					frame.setVisible(true);
					frame.toFront();
				} catch(Exception e) {
					LuaMade.getInstance().logException("Failed to open Swing editor", e);
					openEditors.remove(virtualPath);
				}
			});
			return true;
		} catch(Exception e) {
			LuaMade.getInstance().logException("Failed to schedule Swing editor", e);
			return false;
		}
	}

	public static boolean isFileOpen(String virtualPath) {
		return openEditors.containsKey(virtualPath);
	}

	private static String chooseSyntaxStyle(String path) {
		if(path == null) {
			return SyntaxConstants.SYNTAX_STYLE_LUA;
		}
		String lower = path.toLowerCase();
		if(lower.endsWith(".lua")) {
			return SyntaxConstants.SYNTAX_STYLE_LUA;
		} else if(lower.endsWith(".json")) {
			return SyntaxConstants.SYNTAX_STYLE_JSON;
		} else if(lower.endsWith(".xml")) {
			return SyntaxConstants.SYNTAX_STYLE_XML;
		} else if(lower.endsWith(".md")) {
			return SyntaxConstants.SYNTAX_STYLE_MARKDOWN;
		} else if(lower.endsWith(".yaml") || lower.endsWith(".yml")) {
			return SyntaxConstants.SYNTAX_STYLE_YAML;
		}
		return SyntaxConstants.SYNTAX_STYLE_LUA;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// File menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);

		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		saveItem.addActionListener(e -> save());
		fileMenu.add(saveItem);

		JMenuItem saveRunItem = new JMenuItem("Save & Run");
		saveRunItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
		saveRunItem.addActionListener(e -> saveAndRun());
		fileMenu.add(saveRunItem);

		fileMenu.addSeparator();

		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
		closeItem.addActionListener(e -> handleClose());
		fileMenu.add(closeItem);

		menuBar.add(fileMenu);

		// Edit menu
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);

		JMenuItem undoItem = new JMenuItem("Undo");
		undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
		undoItem.addActionListener(e -> textArea.undoLastAction());
		editMenu.add(undoItem);

		JMenuItem redoItem = new JMenuItem("Redo");
		redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
		redoItem.addActionListener(e -> textArea.redoLastAction());
		editMenu.add(redoItem);

		editMenu.addSeparator();

		JMenuItem cutItem = new JMenuItem("Cut");
		cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
		cutItem.addActionListener(e -> textArea.cut());
		editMenu.add(cutItem);

		JMenuItem copyItem = new JMenuItem("Copy");
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
		copyItem.addActionListener(e -> textArea.copy());
		editMenu.add(copyItem);

		JMenuItem pasteItem = new JMenuItem("Paste");
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
		pasteItem.addActionListener(e -> textArea.paste());
		editMenu.add(pasteItem);

		editMenu.addSeparator();

		JMenuItem selectAllItem = new JMenuItem("Select All");
		selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
		selectAllItem.addActionListener(e -> textArea.selectAll());
		editMenu.add(selectAllItem);

		editMenu.addSeparator();

		JMenuItem findItem = new JMenuItem("Find...");
		findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
		findItem.addActionListener(e -> showFindDialog());
		editMenu.add(findItem);

		JMenuItem goToLineItem = new JMenuItem("Go to Line...");
		goToLineItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
		goToLineItem.addActionListener(e -> showGoToLineDialog());
		editMenu.add(goToLineItem);

		menuBar.add(editMenu);

		// View menu
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);

		JMenuItem toggleThemeItem = new JMenuItem("Toggle Dark/Light Theme");
		toggleThemeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
		toggleThemeItem.addActionListener(e -> toggleTheme());
		viewMenu.add(toggleThemeItem);

		JCheckBoxMenuItem wordWrapItem = new JCheckBoxMenuItem("Word Wrap");
		wordWrapItem.setSelected(textArea.getLineWrap());
		wordWrapItem.addActionListener(e -> {
			textArea.setLineWrap(wordWrapItem.isSelected());
			textArea.setWrapStyleWord(wordWrapItem.isSelected());
		});
		viewMenu.add(wordWrapItem);

		JCheckBoxMenuItem codeFoldingItem = new JCheckBoxMenuItem("Code Folding");
		codeFoldingItem.setSelected(textArea.isCodeFoldingEnabled());
		codeFoldingItem.addActionListener(e -> textArea.setCodeFoldingEnabled(codeFoldingItem.isSelected()));
		viewMenu.add(codeFoldingItem);

		viewMenu.addSeparator();

		JMenuItem zoomInItem = new JMenuItem("Zoom In");
		zoomInItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK));
		zoomInItem.addActionListener(e -> zoom(2));
		viewMenu.add(zoomInItem);

		JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
		zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));
		zoomOutItem.addActionListener(e -> zoom(-2));
		viewMenu.add(zoomOutItem);

		JMenuItem zoomResetItem = new JMenuItem("Reset Zoom");
		zoomResetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK));
		zoomResetItem.addActionListener(e -> {
			applyFontSize(14);
			ConfigManager.setEditorFontSize(14);
		});
		viewMenu.add(zoomResetItem);

		menuBar.add(viewMenu);

		return menuBar;
	}

	private void showFindDialog() {
		String query = JOptionPane.showInputDialog(this, "Find:", "Find", JOptionPane.PLAIN_MESSAGE);
		if(query == null || query.isEmpty()) return;

		String text = textArea.getText();
		int caretPos = textArea.getCaretPosition();
		int idx = text.indexOf(query, caretPos);
		if(idx < 0) {
			idx = text.indexOf(query, 0);
		}
		if(idx >= 0) {
			textArea.setCaretPosition(idx);
			textArea.moveCaretPosition(idx + query.length());
			textArea.requestFocusInWindow();
		} else {
			JOptionPane.showMessageDialog(this, "Not found: " + query, "Find", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void showGoToLineDialog() {
		String input = JOptionPane.showInputDialog(this, "Go to line:", "Go to Line", JOptionPane.PLAIN_MESSAGE);
		if(input == null || input.trim().isEmpty()) return;
		try {
			int line = Integer.parseInt(input.trim()) - 1;
			if(line < 0) line = 0;
			if(line >= textArea.getLineCount()) line = textArea.getLineCount() - 1;
			int offset = textArea.getLineStartOffset(line);
			textArea.setCaretPosition(offset);
			textArea.requestFocusInWindow();
		} catch(NumberFormatException | BadLocationException ignored) {
		}
	}

	private void setupKeyBindings() {
		InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
		ActionMap actionMap = textArea.getActionMap();

		// Ctrl+S: Save
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
		actionMap.put("save", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}
		});

		// Ctrl+W: Close
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK), "close");
		actionMap.put("close", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleClose();
			}
		});

		// Ctrl+R: Save and run
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "saveAndRun");
		actionMap.put("saveAndRun", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAndRun();
			}
		});

		// Ctrl+T: Toggle dark/light theme
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "toggleTheme");
		actionMap.put("toggleTheme", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleTheme();
			}
		});
	}

	private boolean save() {
		String content = textArea.getText();
		if(fileSystem.write(virtualPath, content)) {
			dirty = false;
			updateTitle();
			return true;
		} else {
			JOptionPane.showMessageDialog(this, "Failed to save file: " + virtualPath, "Save Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private void saveAndRun() {
		if(!save()) {
			return;
		}
		closeAndRemove();
		if(terminal != null) {
			terminal.handleInput("run " + virtualPath);
		}
	}

	private void handleClose() {
		if(dirty) {
			int result = JOptionPane.showConfirmDialog(this, "Save changes to " + virtualPath + "?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if(result == JOptionPane.YES_OPTION) {
				if(!save()) {
					return;
				}
				closeAndRemove();
			} else if(result == JOptionPane.NO_OPTION) {
				closeAndRemove();
			}
			// CANCEL: do nothing, keep editor open
		} else {
			closeAndRemove();
		}
	}

	private void closeAndRemove() {
		openEditors.remove(virtualPath);
		dispose();
	}

	private void markDirty() {
		if(!dirty) {
			dirty = true;
			updateTitle();
		}
	}

	private void updateTitle() {
		String title = "Edit: " + virtualPath;
		if(dirty) {
			title = "* " + title;
		}
		setTitle(title);
	}

	private void setupZoomMouseWheel(RTextScrollPane scrollPane) {
		MouseWheelListener[] scrollListeners = scrollPane.getMouseWheelListeners();
		for(MouseWheelListener listener : scrollListeners) {
			scrollPane.removeMouseWheelListener(listener);
		}
		MouseWheelListener[] textListeners = textArea.getMouseWheelListeners();
		for(MouseWheelListener listener : textListeners) {
			textArea.removeMouseWheelListener(listener);
		}

		MouseWheelListener zoomOrScroll = e -> {
			if((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0) {
				if(e.getWheelRotation() < 0) {
					zoom(2);
				} else if(e.getWheelRotation() > 0) {
					zoom(-2);
				}
			} else {
				scrollPane.getVerticalScrollBar().setValue(
					scrollPane.getVerticalScrollBar().getValue() + e.getUnitsToScroll() * scrollPane.getVerticalScrollBar().getUnitIncrement()
				);
			}
		};
		scrollPane.addMouseWheelListener(zoomOrScroll);
		textArea.addMouseWheelListener(zoomOrScroll);
	}

	private void zoom(int delta) {
		int currentSize = textArea.getFont().getSize();
		int newSize = Math.max(8, Math.min(48, currentSize + delta));
		if(newSize != currentSize) {
			applyFontSize(newSize);
			ConfigManager.setEditorFontSize(newSize);
		}
	}

	private void applyFontSize(int size) {
		Font current = textArea.getFont();
		textArea.setFont(current.deriveFont((float) size));
	}

	private void toggleTheme() {
		boolean wasDark = ConfigManager.isEditorDarkTheme();
		ConfigManager.setEditorTheme(wasDark ? "light" : "dark");
		applyTheme(!wasDark);
	}

	private void applyTheme(boolean dark) {
		Color bg, fg, caret, selection, currentLine, marginLine;
		if(dark) {
			bg = new Color(30, 30, 30);
			fg = new Color(212, 212, 212);
			caret = new Color(220, 220, 220);
			selection = new Color(38, 79, 120);
			currentLine = new Color(40, 40, 40);
			marginLine = new Color(50, 50, 50);
		} else {
			bg = new Color(255, 255, 255);
			fg = new Color(30, 30, 30);
			caret = new Color(0, 0, 0);
			selection = new Color(173, 214, 255);
			currentLine = new Color(245, 245, 245);
			marginLine = new Color(220, 220, 220);
		}

		textArea.setBackground(bg);
		textArea.setForeground(fg);
		textArea.setCaretColor(caret);
		textArea.setSelectionColor(selection);
		textArea.setCurrentLineHighlightColor(currentLine);
		textArea.setMarginLineColor(marginLine);
		textArea.setFadeCurrentLineHighlight(false);
		textArea.setRoundedSelectionEdges(true);
		applySyntaxColors(dark);
		textArea.repaint();
	}

	private void applySyntaxColors(boolean dark) {
		SyntaxScheme scheme = textArea.getSyntaxScheme();
		if(dark) {
			// VS Code Dark+ inspired colors
			setTokenStyle(scheme, Token.RESERVED_WORD, new Color(86, 156, 214), true);    // keywords: blue
			setTokenStyle(scheme, Token.RESERVED_WORD_2, new Color(197, 134, 192), true); // secondary keywords: purple
			setTokenStyle(scheme, Token.FUNCTION, new Color(220, 220, 170), false);        // functions: yellow
			setTokenStyle(scheme, Token.LITERAL_BOOLEAN, new Color(86, 156, 214), false);  // true/false: blue
			setTokenStyle(scheme, Token.LITERAL_NUMBER_DECIMAL_INT, new Color(181, 206, 168), false);
			setTokenStyle(scheme, Token.LITERAL_NUMBER_FLOAT, new Color(181, 206, 168), false);
			setTokenStyle(scheme, Token.LITERAL_NUMBER_HEXADECIMAL, new Color(181, 206, 168), false);
			setTokenStyle(scheme, Token.LITERAL_STRING_DOUBLE_QUOTE, new Color(206, 145, 120), false);
			setTokenStyle(scheme, Token.LITERAL_CHAR, new Color(206, 145, 120), false);
			setTokenStyle(scheme, Token.LITERAL_BACKQUOTE, new Color(206, 145, 120), false);
			setTokenStyle(scheme, Token.COMMENT_EOL, new Color(106, 153, 85), false);      // comments: green
			setTokenStyle(scheme, Token.COMMENT_MULTILINE, new Color(106, 153, 85), false);
			setTokenStyle(scheme, Token.COMMENT_DOCUMENTATION, new Color(106, 153, 85), false);
			setTokenStyle(scheme, Token.OPERATOR, new Color(212, 212, 212), false);
			setTokenStyle(scheme, Token.SEPARATOR, new Color(212, 212, 212), false);
			setTokenStyle(scheme, Token.IDENTIFIER, new Color(156, 220, 254), false);      // identifiers: light blue
			setTokenStyle(scheme, Token.VARIABLE, new Color(156, 220, 254), false);
			setTokenStyle(scheme, Token.DATA_TYPE, new Color(78, 201, 176), false);        // types: teal
		} else {
			// VS Code Light+ inspired colors
			setTokenStyle(scheme, Token.RESERVED_WORD, new Color(0, 0, 255), true);        // keywords: blue
			setTokenStyle(scheme, Token.RESERVED_WORD_2, new Color(175, 0, 219), true);    // secondary keywords: purple
			setTokenStyle(scheme, Token.FUNCTION, new Color(121, 94, 38), false);           // functions: dark yellow
			setTokenStyle(scheme, Token.LITERAL_BOOLEAN, new Color(0, 0, 255), false);
			setTokenStyle(scheme, Token.LITERAL_NUMBER_DECIMAL_INT, new Color(9, 134, 88), false);
			setTokenStyle(scheme, Token.LITERAL_NUMBER_FLOAT, new Color(9, 134, 88), false);
			setTokenStyle(scheme, Token.LITERAL_NUMBER_HEXADECIMAL, new Color(9, 134, 88), false);
			setTokenStyle(scheme, Token.LITERAL_STRING_DOUBLE_QUOTE, new Color(163, 21, 21), false);
			setTokenStyle(scheme, Token.LITERAL_CHAR, new Color(163, 21, 21), false);
			setTokenStyle(scheme, Token.LITERAL_BACKQUOTE, new Color(163, 21, 21), false);
			setTokenStyle(scheme, Token.COMMENT_EOL, new Color(0, 128, 0), false);
			setTokenStyle(scheme, Token.COMMENT_MULTILINE, new Color(0, 128, 0), false);
			setTokenStyle(scheme, Token.COMMENT_DOCUMENTATION, new Color(0, 128, 0), false);
			setTokenStyle(scheme, Token.OPERATOR, new Color(0, 0, 0), false);
			setTokenStyle(scheme, Token.SEPARATOR, new Color(0, 0, 0), false);
			setTokenStyle(scheme, Token.IDENTIFIER, new Color(0, 16, 128), false);
			setTokenStyle(scheme, Token.VARIABLE, new Color(0, 16, 128), false);
			setTokenStyle(scheme, Token.DATA_TYPE, new Color(38, 127, 153), false);
		}
		textArea.setSyntaxScheme(scheme);
	}

	private static void setTokenStyle(SyntaxScheme scheme, int tokenType, Color color, boolean bold) {
		Style style = scheme.getStyle(tokenType);
		if(style == null) {
			style = new Style();
		}
		style.foreground = color;
		if(bold) {
			Font base = style.font;
			if(base != null) {
				style.font = base.deriveFont(Font.BOLD);
			}
		}
		scheme.setStyle(tokenType, style);
	}
}
