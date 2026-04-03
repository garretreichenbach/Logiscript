package luamade.lua.element.system.module;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.manager.JumpScriptTargetManager;
import org.luaj.vm2.LuaError;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;
import org.schema.game.common.controller.elements.jumpdrive.JumpDriveCollectionManager;
import org.schema.game.common.controller.elements.jumpdrive.JumpDriveElementManager;

import java.util.Collection;

/**
 * Lua wrapper for a ship's FTL jump drive system.
 *
 * <p>Scripts can read charge state, set a destination before engaging the drive,
 * and trigger the jump directly without any player input.
 *
 * <p>The target set by {@link #setTarget} is stored in
 * {@link JumpScriptTargetManager} and applied by {@link luamade.listener.JumpTargetListener}
 * when the {@link api.listener.events.entity.ShipJumpEngageEvent} fires. It is
 * consumed on the next jump, so one {@code setTarget} call affects exactly one jump.
 *
 * <h2>Example</h2>
 * <pre>
 * local jd = entity:getJumpDrive()
 * if jd:hasJumpDrive() and jd:isCharged() then
 *     jd:setTarget(100, 0, 100)   -- absolute sector coordinates
 *     jd:engage()
 * end
 * </pre>
 */
@LuaMadeClass("JumpDrive")
public class JumpDrive extends Module {

	public JumpDrive(SegmentController segmentController) {
		super(segmentController);
	}

	// -------------------------------------------------------------------------
	// Target control
	// -------------------------------------------------------------------------

	/**
	 * Sets the script-controlled jump destination to the given absolute sector
	 * coordinates. This overrides the default direction (player waypoint / forward)
	 * for the next jump only; the target is consumed once the jump fires.
	 *
	 * @param x absolute sector X
	 * @param y absolute sector Y
	 * @param z absolute sector Z
	 */
	@LuaMadeCallable
	public void setTarget(Integer x, Integer y, Integer z) {
		if(x == null || y == null || z == null) throw new LuaError("setTarget: x, y, z must not be nil");
		JumpScriptTargetManager.setTarget(segmentController, new Vector3i(x, y, z));
	}

	/**
	 * Removes any pending script jump target. The next jump will use the pilot's
	 * navigation waypoint or the ship's forward direction.
	 */
	@LuaMadeCallable
	public void clearTarget() {
		JumpScriptTargetManager.clearTarget(segmentController);
	}

	/**
	 * Returns the current script target as {@code {x, y, z}}, or {@code nil} when
	 * none is set.
	 */
	@LuaMadeCallable
	public int[] getTarget() {
		Vector3i target = JumpScriptTargetManager.getTarget(segmentController);
		if(target == null) return null;
		return new int[]{target.x, target.y, target.z};
	}

	// -------------------------------------------------------------------------
	// Jump execution
	// -------------------------------------------------------------------------

	/**
	 * Engages the first fully-charged jump drive group. Returns {@code true} when
	 * a jump was initiated, {@code false} if no group is charged or the ship has no
	 * jump drive.
	 *
	 * <p>Call {@link #setTarget} first to jump to a specific sector.
	 */
	@LuaMadeCallable
	public Boolean engage() {
		if(!segmentController.isOnServer()) return false;
		Collection<JumpDriveCollectionManager> cms = getCollectionManagers();
		if(cms == null) return false;
		for(JumpDriveCollectionManager cm : cms) {
			if(cm.isCharged()) {
				cm.jump();
				return true;
			}
		}
		return false;
	}

	// -------------------------------------------------------------------------
	// Status queries
	// -------------------------------------------------------------------------

	/**
	 * Returns {@code true} when at least one jump drive group is fully charged.
	 */
	@LuaMadeCallable
	public Boolean isCharged() {
		Collection<JumpDriveCollectionManager> cms = getCollectionManagers();
		if(cms == null) return false;
		for(JumpDriveCollectionManager cm : cms) {
			if(cm.isCharged()) return true;
		}
		return false;
	}

	/**
	 * Returns the charge of the most-charged jump drive group.
	 */
	@LuaMadeCallable
	public Float getCharge() {
		Collection<JumpDriveCollectionManager> cms = getCollectionManagers();
		if(cms == null) return 0f;
		float max = 0f;
		for(JumpDriveCollectionManager cm : cms) {
			if(cm.getCharge() > max) max = cm.getCharge();
		}
		return max;
	}

	/**
	 * Returns the charge required to jump for the most-charged group.
	 */
	@LuaMadeCallable
	public Float getChargeNeeded() {
		Collection<JumpDriveCollectionManager> cms = getCollectionManagers();
		if(cms == null) return 0f;
		float maxCharge = -1f;
		float needed = 0f;
		for(JumpDriveCollectionManager cm : cms) {
			if(cm.getCharge() > maxCharge) {
				maxCharge = cm.getCharge();
				needed = cm.getChargeNeededForJump();
			}
		}
		return needed;
	}

	/**
	 * Returns the base jump range in sectors (server configuration constant,
	 * typically 8).
	 */
	@LuaMadeCallable
	public Integer getDistance() {
		return JumpDriveElementManager.BASE_DISTANCE_SECTORS;
	}

	/**
	 * Returns {@code true} when the ship has jump drive modules installed.
	 */
	@LuaMadeCallable
	public Boolean hasJumpDrive() {
		Collection<JumpDriveCollectionManager> cms = getCollectionManagers();
		return cms != null && !cms.isEmpty();
	}

	@LuaMadeCallable
	@Override
	public Integer getSize() {
		ShipManagerContainer container = getContainer();
		if(container == null) return 0;
		return container.getJumpDrive().getElementManager().totalSize;
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private Collection<JumpDriveCollectionManager> getCollectionManagers() {
		ShipManagerContainer container = getContainer();
		if(container == null) return null;
		return container.getJumpDrive().getCollectionManagers();
	}

	private ShipManagerContainer getContainer() {
		try {
			return ((Ship) segmentController).getManagerContainer();
		} catch(Exception e) {
			return null;
		}
	}
}
