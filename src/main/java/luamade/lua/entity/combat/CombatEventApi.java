package luamade.lua.entity.combat;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Lua-facing combat event API exposed as the global {@code combat} inside scripts.
 * <p>
 * Events are queued from the game thread when the entity takes damage,
 * and consumed by Lua scripts via poll/waitFor.
 * <p>
 * Lua usage:
 * <pre>
 *   local e = combat.poll()          -- returns event or nil immediately
 *   local e = combat.waitFor(5000)   -- blocks up to 5 s
 *   combat.clear()                   -- discard pending events
 *   combat.setEnabled(true/false)    -- opt in/out of event capture
 * </pre>
 *
 * Each event is a Lua table:
 * <pre>
 *   { type="block_damage",
 *     damageType="PROJECTILE"|"BEAM"|"MISSILE"|"PULSE"|"EXPLOSIVE"|"GENERAL",
 *     damage=150,
 *     attackerName="EnemyShip",
 *     attackerFaction=2,
 *     isServer=true }
 *
 *   { type="block_killed",
 *     blockType=16,
 *     attackerName="EnemyShip",
 *     isServer=true }
 *
 *   { type="shield_hit",
 *     damage=200.0,
 *     damageType="PROJECTILE",
 *     isServer=true }
 * </pre>
 */
public class CombatEventApi extends LuaMadeUserdata {

	private static final int QUEUE_CAPACITY = 256;

	private final LinkedBlockingQueue<LuaTable> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	private volatile boolean enabled = true;

	/**
	 * Called from the game thread to push a damage event. Non-blocking; drops
	 * the event silently if the queue is full or capture is disabled.
	 */
	public void pushEvent(LuaTable event) {
		if(!enabled) return;
		eventQueue.offer(event);
	}

	@LuaMadeCallable
	public LuaTable poll() {
		LuaTable event = eventQueue.poll();
		return event != null ? event : null;
	}

	@LuaMadeCallable
	public LuaTable waitFor(Integer timeoutMs) {
		try {
			return eventQueue.poll(Math.max(0, timeoutMs), TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	@LuaMadeCallable
	public void clear() {
		eventQueue.clear();
	}

	@LuaMadeCallable
	public Boolean isEnabled() {
		return enabled;
	}

	@LuaMadeCallable
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
		if(!enabled) eventQueue.clear();
	}

	@LuaMadeCallable
	public Integer getPendingCount() {
		return eventQueue.size();
	}
}
