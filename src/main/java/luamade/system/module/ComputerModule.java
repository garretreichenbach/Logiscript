package luamade.system.module;

import luamade.gui.ComputerDialog;
import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.lua.terminal.Terminal;
import org.schema.game.common.data.SegmentPiece;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * New Computer Module class to replace the old one.
 * <br/>This does not handle the actual computer logic, but rather the integration with StarMade's systems.
 */
public class ComputerModule {
	
	public enum ComputerMode {
		OFF,
		IDLE,
		TERMINAL,
		FILE_EDIT
	}

	private final byte VERSION = 1;
	private final String uuid;
	private final Console console;
	private ComputerMode lastMode = ComputerMode.IDLE;
	private long lastTouched;
	private String lastOpenFile = "";
	private final Terminal terminal;
	private final FileSystem fileSystem;

	public ComputerModule(String uuid) {
		this.uuid = uuid;
		lastTouched = System.currentTimeMillis();
		console = new Console(this);
		fileSystem = FileSystem.initNewFileSystem(this);
		terminal = new Terminal(this, console, fileSystem);
	}

	public static String generateComputerUUID(long absIndex) {
		return UUID.nameUUIDFromBytes((String.valueOf(absIndex)).getBytes(StandardCharsets.UTF_8)).toString();
	}

	public String getUUID() {
		return uuid;
	}

	public String getLastTextContent() {
		switch(lastMode) {
			case OFF:
				return "";
			case IDLE:
				return "";
			case TERMINAL:
				return terminal.getTextContents();
			case FILE_EDIT:
				return fileSystem.getFile(lastOpenFile).getTextContents();
		}
		return "";
	}

	public void openGUI(SegmentPiece segmentPiece) {
		try {
			ComputerDialog dialog = new ComputerDialog(this);
			dialog.getInputPanel().onInit();
			dialog.activate();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	public void resumeFromLastMode() {
		switch(lastMode) {
			case OFF:
				// Do nothing
				break;
			case TERMINAL:
				// Resume terminal state
				loadIntoTerminal();
				break;
			case FILE_EDIT:
				// Resume file edit state
				loadIntoFileEdit(lastOpenFile);
				break;
		}
	}

	public ComputerMode getLastMode() {
		return lastMode;
	}

	public void setLastMode(ComputerMode lastMode) {
		this.lastMode = lastMode;
		resumeFromLastMode();
	}

	public void loadIntoTerminal() {
		terminal.start();
	}

	public void loadIntoFileEdit(String file) {
		lastOpenFile = file;
		console.setTextContents(file);
	}

	public long getLastTouched() {
		return lastTouched;
	}

	public void setTouched() {
		lastTouched = System.currentTimeMillis();
	}
}
