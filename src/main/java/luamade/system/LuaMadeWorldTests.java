package luamade.system;

import com.google.gson.JsonObject;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.server.test.world.WorldTestExtension;
import org.schema.game.server.test.world.WorldTestSpec;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World-test extension (engine {@code run_tests}) for LuaMade computers. Tests live in {@code src/main/resources/tests}.
 *
 * <p>Setup, on a Computer block of any body: {@code "luamade": [{"at": [x,y,z], "script": "<lua>"}]} writes the script
 * to {@code /home/test.lua} and runs it once the computer has booted.
 *
 * <p>Expect: {@code {"luamade": [x,y,z], "measure": ...}} on that computer, where {@code measure} is
 * <ul>
 *   <li>{@code "console"} with {@code "text"}: 1 once the console output contains the text, else 0;</li>
 *   <li>{@code "manual_turrets"}: how many turrets in the computer's rail tree a script is free-aiming;</li>
 *   <li>{@code "camera"}: 1 while a camera feed is set ({@code console.setCamera}), else 0.</li>
 * </ul>
 */
public final class LuaMadeWorldTests implements WorldTestExtension {

	private static final String SCRIPT_PATH = "/home/test.lua";
	private final Set<String> logged = ConcurrentHashMap.newKeySet();

	@Override
	public String setupKey() {
		return "luamade";
	}

	@Override
	public String expectKey() {
		return "luamade";
	}

	@Override
	public boolean ready(SegmentController entity, long index) {
		return module(entity, index, true) != null;
	}

	@Override
	public void apply(SegmentController entity, long index, WorldTestSpec.Extra extra) {
		ComputerModule module = module(entity, index, true);
		JsonObject json = extra.json();
		if(module == null || !json.has("script")) {
			return;
		}
		if(module.getTerminal() != null && !module.getTerminal().isRunning()) {
			module.resumeFromLastMode();
		}
		module.getFileSystem().write(SCRIPT_PATH, json.get("script").getAsString());
		module.getTerminal().handleInput("run " + SCRIPT_PATH, null);
	}

	@Override
	public double measure(SegmentController entity, long index, WorldTestSpec.Expect expect) {
		ComputerModule module = module(entity, index, false);
		if(module == null) {
			return 0;
		}
		JsonObject json = expect.json();
		String measure = json.has("measure") ? json.get("measure").getAsString() : "console";
		return switch(measure) {
			case "console" -> {
				String text = json.has("text") ? json.get("text").getAsString() : "";
				String console = module.getLastTextContent();
				if(console == null || !console.contains(text)) {
					logFailure(entity, index, console);
					yield 0;
				}
				yield 1;
			}
			case "manual_turrets" -> manualTurrets(entity.railController.getRoot());
			case "camera" -> module.getCameraFeed() != null ? 1 : 0;
			default -> throw new IllegalArgumentException("Unknown luamade measure: " + measure);
		};
	}

	/** The engine's failure line can't say why a script stopped, so log the console once when a script failed or errored. */
	private void logFailure(SegmentController entity, long index, String console) {
		if(console != null && (console.contains("FAIL ") || console.contains("rror")) && logged.add(entity.getUniqueIdentifier() + "@" + index + ":" + console.hashCode())) {
			LuaMade.getInstance().logWarning("[World test] computer " + ElementCollection.getPosFromIndex(index, new Vector3i()) + " on " + entity.getRealName() + " console:" + System.lineSeparator() + console);
		}
	}

	private static int manualTurrets(SegmentController node) {
		int count = node.railController.isTurretDocked() && ManualTurretControl.active(node.getId()) != null ? 1 : 0;
		for(var relation : node.railController.next) {
			count += manualTurrets(relation.docked.getSegmentController());
		}
		return count;
	}

	private static ComputerModule module(SegmentController entity, long index, boolean create) {
		if(!(entity instanceof ManagedUsableSegmentController<?> controller)) {
			return null;
		}
		SegmentPiece piece = entity.getSegmentBuffer().getPointUnsave(index);
		ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(piece == null || piece.getType() != ElementRegistry.COMPUTER.getId() || container == null) {
			return null;
		}
		return create ? container.getOrCreateModule(piece) : container.getModule(piece);
	}
}
