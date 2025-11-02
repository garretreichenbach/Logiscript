package luamade.system.module;

import luamade.gui.ComputerDialog;
import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.lua.networking.NetworkInterface;
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

	private final SegmentPiece segmentPiece;
	private final String uuid;
	private final Console console;
	private final FileSystem fileSystem;
	private final NetworkInterface networkInterface;
	private final Terminal terminal;
	private ComputerMode lastMode = ComputerMode.IDLE;
	private long lastTouched;
	private String lastOpenFile = "";

	public ComputerModule(SegmentPiece segmentPiece, String uuid) {
		this.uuid = uuid;
		this.segmentPiece = segmentPiece;
		lastTouched = System.currentTimeMillis();
		console = new Console(this);
		fileSystem = FileSystem.initNewFileSystem(this);
		networkInterface = new NetworkInterface(this);
		terminal = new Terminal(this, console, fileSystem);
	}

	public SegmentPiece getSegmentPiece() {
		return segmentPiece;
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

	public Console getConsole() {
		return console;
	}

	public FileSystem getFileSystem() {
		return fileSystem;
	}

	public Terminal getTerminal() {
		return terminal;
	}

	public NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	/**
	 * Saves the computer's file system to disk and cleans up temporary files.
	 * This should be called when the computer goes idle or when the server shuts down.
	 */
	public void saveAndCleanup() {
		fileSystem.saveToDisk();
		fileSystem.cleanupTempFiles();
	}

	/**
	 * Checks if the computer has been idle for too long and should be saved to disk.
	 * @param idleTimeMs The maximum idle time in milliseconds before saving
	 * @return true if the computer should be saved
	 */
	public boolean shouldSave(long idleTimeMs) {
		return (System.currentTimeMillis() - lastTouched) > idleTimeMs;
	}
}
