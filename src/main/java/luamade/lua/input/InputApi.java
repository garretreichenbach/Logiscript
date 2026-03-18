package luamade.lua.input;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Lua-facing input API exposed as the global {@code input} inside scripts.
 * <p>
 * Events are queued on the UI/event thread and consumed by Lua scripts.
 * The queue is bounded to prevent memory growth if a script never reads events.
 * <p>
 * Lua usage:
 * <pre>
 *   local e = input.poll()          -- returns event or nil immediately
 *   local e = input.waitFor(2000)   -- blocks up to 2 s, returns event or nil
 *   input.clear()                   -- discard all pending events
 *   input.setEnabled(true/false)    -- opt in/out of event capture
 * </pre>
 *
 * Each event is a Lua table:
 * <pre>
 *   { type="key",   key=65,  char="a", down=true,  shift=false, ctrl=false, alt=false }
 *   { type="mouse", button="left"|"right"|"middle",
 *     pressed=true, released=false,
 *     x=120, y=45, dx=0, dy=0, wheel=0 }
 * </pre>
 */
public class InputApi extends LuaMadeUserdata {

	/** Maximum events held in the queue before older ones are dropped. */
	private static final int QUEUE_CAPACITY = 256;

	private final LinkedBlockingQueue<LuaTable> eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
	private volatile boolean enabled = true;

	// ------------------------------------------------------------------
	// Package-private: called from EventManager / ComputerDialog on the
	// UI/game thread.  These must be non-blocking and thread-safe.
	// ------------------------------------------------------------------

	private static String buttonName(int button) {
		switch(button) {
			case 0:
				return "left";
			case 1:
				return "right";
			case 2:
				return "middle";
			default:
				return "none";
		}
	}

	/**
	 * Enqueues a keyboard event. Silently drops the event when the queue is
	 * full or input capture is disabled.
	 */
	public void pushKeyEvent(int glfwKey, char character, boolean down,
	                         boolean shift, boolean ctrl, boolean alt) {
		if(!enabled) return;
		LuaTable t = new LuaTable();
		t.set("type", valueOf("key"));
		t.set("key", valueOf(glfwKey));
		t.set("char", valueOf(character == 0 ? "" : String.valueOf(character)));
		t.set("down", valueOf(down));
		t.set("shift", valueOf(shift));
		t.set("ctrl", valueOf(ctrl));
		t.set("alt", valueOf(alt));
		eventQueue.offer(t); // non-blocking; drops if full
	}

	// ------------------------------------------------------------------
	// Lua-callable API
	// ------------------------------------------------------------------

	/**
	 * Enqueues a mouse event. Silently drops when queue is full or disabled.
	 *
	 * @param button   0=left, 1=right, 2=middle, -1=move/scroll only
	 * @param pressed  true on button-down, false on button-up
	 * @param x        absolute x position
	 * @param y        absolute y position
	 * @param dx       delta x since last event
	 * @param dy       delta y since last event
	 * @param wheel    scroll wheel delta
	 */
	public void pushMouseEvent(int button, boolean pressed,
	                           int x, int y, int dx, int dy, int wheel) {
		if(!enabled) return;
		LuaTable t = new LuaTable();
		t.set("type", valueOf("mouse"));
		t.set("button", valueOf(buttonName(button)));
		t.set("pressed", valueOf(pressed));
		t.set("released", valueOf(!pressed && button >= 0));
		t.set("x", valueOf(x));
		t.set("y", valueOf(y));
		t.set("dx", valueOf(dx));
		t.set("dy", valueOf(dy));
		t.set("wheel", valueOf(wheel));
		eventQueue.offer(t);
	}

	/**
	 * Returns the next event from the queue, or nil if none are pending.
	 */
	@LuaMadeCallable
	public LuaValue poll() {
		LuaTable event = eventQueue.poll();
		return event != null ? event : NIL;
	}

	/**
	 * Blocks the calling Lua thread until an event arrives or {@code timeoutMs}
	 * milliseconds elapse.  Returns the event table, or nil on timeout.
	 *
	 * @param timeoutMs maximum wait in milliseconds (≤ 0 means return immediately)
	 */
	@LuaMadeCallable
	public LuaValue waitFor(int timeoutMs) {
		if(timeoutMs <= 0) {
			return poll();
		}
		try {
			LuaTable event = eventQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
			return event != null ? event : NIL;
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			return NIL;
		}
	}

	/**
	 * Discards all pending events.
	 */
	@LuaMadeCallable
	public void clear() {
		eventQueue.clear();
	}

	/**
	 * Returns whether event capture is currently enabled.
	 */
	@LuaMadeCallable
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables event capture.  When disabled, new events are not
	 * enqueued (existing queued events remain).
	 */
	@LuaMadeCallable
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/**
	 * Returns the number of events currently waiting in the queue.
	 */
	@LuaMadeCallable
	public int pending() {
		return eventQueue.size();
	}
}

