package luamade.system.module;

import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.lua.gfx.Gfx2d;
import luamade.lua.input.InputApi;
import luamade.lua.networking.NetworkInterface;
import luamade.lua.terminal.Terminal;
import org.schema.game.common.data.SegmentPiece;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * New Computer Module class to replace the old one.
 * <br/>This does not handle the actual computer logic, but rather the integration with StarMade's systems.
 */
public class ComputerModule {

	private final SegmentPiece segmentPiece;
	private final String uuid;
	private final Console console;
	private final FileSystem fileSystem;
	private final NetworkInterface networkInterface;
	private final Gfx2d gfxApi;
	private final Terminal terminal;
	private final InputApi inputApi;
	private ComputerMode lastMode = ComputerMode.OFF;
	private long lastTouched;
	private String lastOpenFile = "";
	private String savedTerminalInput = "";
	private String lastDocsTopicPath = "";
	private final Set<String> collapsedDocsSections = new LinkedHashSet<>();
	private String displayName;
	private ScrollMode scrollMode = ScrollMode.VERTICAL;
	private boolean forwardEnterWhileInputMasked = true;

	public ComputerModule(SegmentPiece segmentPiece, String uuid) {
		this.uuid = uuid;
		this.segmentPiece = segmentPiece;
		displayName = defaultDisplayNameFor(uuid);
		lastTouched = System.currentTimeMillis();
		console = new Console(this);
		fileSystem = FileSystem.initNewFileSystem(this);
		networkInterface = new NetworkInterface(this);
		gfxApi = new Gfx2d();
		terminal = new Terminal(this, console, fileSystem);
		inputApi = new InputApi();
	}

	public static String generateComputerUUID(long absIndex) {
		return UUID.nameUUIDFromBytes((String.valueOf(absIndex)).getBytes(StandardCharsets.UTF_8)).toString();
	}

	public static String generateLegacyComputerUUID(long absIndex) {
		return generateComputerUUID(absIndex);
	}

	public static String generateComputerUUID(SegmentPiece segmentPiece) {
		if(segmentPiece == null) {
			return generateComputerUUID(0L);
		}
		return generateComputerUUID(segmentPiece.getAbsoluteIndex());
	}

	public SegmentPiece getSegmentPiece() {
		return segmentPiece;
	}

	public String getUUID() {
		return uuid;
	}

	private static String defaultDisplayNameFor(String uuid) {
		int shortLength = Math.min(8, uuid.length());
		return "computer-" + uuid.substring(0, shortLength);
	}

	public String getPromptComputerName() {
		return displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean setDisplayName(String displayName) {
		if(displayName == null) {
			return false;
		}

		String normalized = displayName.trim();
		if(normalized.isEmpty() || normalized.length() > 32 || normalized.contains("\n") || normalized.contains("\r")) {
			return false;
		}

		this.displayName = normalized;
		return true;
	}

	public void resetDisplayName() {
		displayName = defaultDisplayNameFor(uuid);
	}

	public void resumeFromLastMode() {
		switch(lastMode) {
			case OFF:
				loadIntoTerminal();
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

	public String getLastTextContent() {
		switch(lastMode) {
			case OFF:
				return terminal.getTextContents();
			case TERMINAL:
				return terminal.getTextContents();
			case FILE_EDIT:
				if(lastOpenFile == null || lastOpenFile.isEmpty()) {
					return "";
				}
				if(!fileSystem.exists(lastOpenFile) || fileSystem.isDir(lastOpenFile)) {
					return "";
				}
				String fileText = fileSystem.read(lastOpenFile);
				return fileText == null ? "" : fileText;
		}
		return "";
	}

	public void loadIntoFileEdit(String file) {
		if(file == null || file.isEmpty()) {
			return;
		}

		String normalized = fileSystem.normalizePath(file);
		if(fileSystem.isDir(normalized)) {
			return;
		}
		if(!fileSystem.exists(normalized)) {
			fileSystem.write(normalized, "");
		}

		lastOpenFile = normalized;
		lastMode = ComputerMode.FILE_EDIT;
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

	public Gfx2d getGfxApi() {
		return gfxApi;
	}

	public InputApi getInputApi() {
		return inputApi;
	}

	public String getSavedTerminalInput() {
		return savedTerminalInput;
	}

	public String getLastOpenFile() {
		return lastOpenFile;
	}

	public void setSavedTerminalInput(String input) {
		savedTerminalInput = input;
	}

	public String getLastDocsTopicPath() {
		return lastDocsTopicPath;
	}

	public void setLastDocsTopicPath(String topicPath) {
		lastDocsTopicPath = topicPath == null ? "" : topicPath;
	}

	public Set<String> getCollapsedDocsSections() {
		return new LinkedHashSet<>(collapsedDocsSections);
	}

	public void setCollapsedDocsSections(Collection<String> sectionKeys) {
		collapsedDocsSections.clear();
		if(sectionKeys == null) {
			return;
		}

		for(String key : sectionKeys) {
			if(key == null) {
				continue;
			}
			String normalized = key.trim();
			if(!normalized.isEmpty()) {
				collapsedDocsSections.add(normalized);
			}
		}
	}

	public boolean openFileInEditor(String file) {
		if(file == null || file.trim().isEmpty()) {
			return false;
		}

		loadIntoFileEdit(file.trim());
		return lastMode == ComputerMode.FILE_EDIT;
	}

	public String getScrollModeName() {
		return scrollMode.name();
	}

	public ScrollMode getScrollMode() {
		return scrollMode;
	}

	public boolean setScrollMode(String modeName) {
		ScrollMode parsed = ScrollMode.fromName(modeName);
		if(parsed == null) {
			return false;
		}
		scrollMode = parsed;
		setTouched();
		return true;
	}

	public boolean isMaskedEnterForwardingEnabled() {
		return forwardEnterWhileInputMasked;
	}

	public void setMaskedEnterForwardingEnabled(boolean enabled) {
		forwardEnterWhileInputMasked = enabled;
		setTouched();
	}

	/**
	 * Restores lightweight module state from container serialization.
	 * File-system contents are loaded independently by FileSystem using computer UUID.
	 */
	public void restoreSerializedState(ComputerMode mode, String openFile, String savedInput, String hostname, String displayName, String lastDocsTopicPath, Collection<String> collapsedDocsSections, String scrollModeName, boolean forwardEnterWhileMasked) {
		if(mode != null) {
			lastMode = mode;
		}
		lastOpenFile = openFile == null ? "" : openFile;
		savedTerminalInput = savedInput == null ? "" : savedInput;
		this.lastDocsTopicPath = lastDocsTopicPath == null ? "" : lastDocsTopicPath;
		setCollapsedDocsSections(collapsedDocsSections);

		if(hostname != null && !hostname.isEmpty()) {
			networkInterface.setHostname(hostname);
		}

		if(displayName != null && !displayName.isEmpty()) {
			setDisplayName(displayName);
		}

		if(scrollModeName != null && !scrollModeName.isEmpty()) {
			setScrollMode(scrollModeName);
		}

		setMaskedEnterForwardingEnabled(forwardEnterWhileMasked);
	}

	/**
	 * Saves the computer's file system to disk and cleans up temporary files.
	 * This should be called when the computer goes idle or when the server shuts down.
	 */
	public void saveAndCleanup() {
		terminal.stop();
		if(fileSystem.saveToDisk()) {
			fileSystem.cleanupTempFiles();
		}
	}

	/**
	 * Checks if the computer has been idle for too long and should be saved to disk.
	 * @param idleTimeMs The maximum idle time in milliseconds before saving
	 * @return true if the computer should be saved
	 */
	public boolean shouldSave(long idleTimeMs) {
		return (System.currentTimeMillis() - lastTouched) > idleTimeMs;
	}

	public enum ComputerMode {
		OFF,
		TERMINAL,
		FILE_EDIT
	}

	public enum ScrollMode {
		NONE,
		HORIZONTAL,
		VERTICAL,
		BOTH;

		public static ScrollMode fromName(String modeName) {
			if(modeName == null) {
				return null;
			}
			String normalized = modeName.trim();
			for(ScrollMode mode : values()) {
				if(mode.name().equalsIgnoreCase(normalized)) {
					return mode;
				}
			}
			return null;
		}
	}
}
