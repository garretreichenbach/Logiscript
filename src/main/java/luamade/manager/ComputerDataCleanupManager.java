package luamade.manager;

import luamade.LuaMade;
import luamade.utils.DataUtils;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans stale computer storage artifacts that are safe to remove.
 *
 * <p>This intentionally stays conservative: it only deletes uncompressed
 * leftover directories and zero-byte compressed files for non-active UUIDs.</p>
 */
public final class ComputerDataCleanupManager {

	private static final Pattern FS_DIRECTORY_PATTERN = Pattern.compile("^computer_([0-9a-fA-F\\-]{36})_fs$");
	private static final Pattern FS_RESTORE_DIRECTORY_PATTERN = Pattern.compile("^computer_([0-9a-fA-F\\-]{36})_fs\\.restore$");
	private static final Pattern FS_ARCHIVE_PATTERN = Pattern.compile("^computer_([0-9a-fA-F\\-]{36})_fs\\.smdat$");
	private static final Pattern FS_TEMP_ARCHIVE_PATTERN = Pattern.compile("^computer_([0-9a-fA-F\\-]{36})_fs\\.smdat\\.tmp$");

	private ComputerDataCleanupManager() {
	}

	public static void cleanupOrphanedComputerData(Set<String> protectedUUIDs) {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath == null || worldDataPath.isEmpty()) {
			return;
		}

		File computersDir = new File(worldDataPath, "computers");
		if(!computersDir.exists() || !computersDir.isDirectory()) {
			return;
		}

		Set<String> protectedSet = protectedUUIDs == null ? Collections.emptySet() : protectedUUIDs;
		File[] entries = computersDir.listFiles();
		if(entries == null) {
			return;
		}

		for(File entry : entries) {
			if(entry == null) {
				continue;
			}

			Matcher dirMatcher = FS_DIRECTORY_PATTERN.matcher(entry.getName());
			if(entry.isDirectory() && dirMatcher.matches()) {
				String uuid = dirMatcher.group(1);
				if(protectedSet.contains(uuid)) {
					continue;
				}
				if(deleteRecursive(entry)) {
					LuaMade.getInstance().logInfo("Cleaned orphaned computer temp directory: " + entry.getName());
				} else {
					LuaMade.getInstance().logWarning("Failed to clean orphaned computer temp directory: " + entry.getName());
				}
				continue;
			}

			Matcher restoreDirMatcher = FS_RESTORE_DIRECTORY_PATTERN.matcher(entry.getName());
			if(entry.isDirectory() && restoreDirMatcher.matches()) {
				String uuid = restoreDirMatcher.group(1);
				if(protectedSet.contains(uuid)) {
					continue;
				}
				if(deleteRecursive(entry)) {
					LuaMade.getInstance().logInfo("Cleaned orphaned computer restore directory: " + entry.getName());
				} else {
					LuaMade.getInstance().logWarning("Failed to clean orphaned computer restore directory: " + entry.getName());
				}
				continue;
			}

			Matcher archiveMatcher = FS_ARCHIVE_PATTERN.matcher(entry.getName());
			if(entry.isFile() && archiveMatcher.matches()) {
				String uuid = archiveMatcher.group(1);
				if(protectedSet.contains(uuid)) {
					continue;
				}
				if(entry.length() == 0L) {
					if(entry.delete()) {
						LuaMade.getInstance().logInfo("Removed orphaned zero-byte computer archive: " + entry.getName());
					} else {
						LuaMade.getInstance().logWarning("Failed to remove orphaned zero-byte computer archive: " + entry.getName());
					}
				}
				continue;
			}

			Matcher tempArchiveMatcher = FS_TEMP_ARCHIVE_PATTERN.matcher(entry.getName());
			if(entry.isFile() && tempArchiveMatcher.matches()) {
				String uuid = tempArchiveMatcher.group(1);
				if(protectedSet.contains(uuid)) {
					continue;
				}
				if(entry.delete()) {
					LuaMade.getInstance().logInfo("Removed orphaned temporary computer archive: " + entry.getName());
				} else {
					LuaMade.getInstance().logWarning("Failed to remove orphaned temporary computer archive: " + entry.getName());
				}
			}
		}
	}

	private static boolean deleteRecursive(File file) {
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			if(children != null) {
				for(File child : children) {
					if(!deleteRecursive(child)) {
						return false;
					}
				}
			}
		}
		return file.delete();
	}
}

