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

	/**
	 * When true, the EventManager cancels StarMade key events so the terminal
	 * text bar never receives keystrokes. Events are still pushed to the Lua
	 * queue so the consuming script can handle them.
	 */
	private volatile boolean keyboardConsumed;

	/**
	 * Convenience flag scripts can use to signal exclusive mouse ownership.
	 * Does not change event routing on its own; query it with {@link #isMouseConsumed()}.
	 */
	private volatile boolean mouseConsumed;

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
	 * @param cellX    mapped canvas cell X (1-based), or -1 when unavailable
	 * @param cellY    mapped canvas cell Y (1-based), or -1 when unavailable
	 * @param uiX      x position relative to terminal gfx canvas, or -1 when unavailable
	 * @param uiY      y position relative to terminal gfx canvas, or -1 when unavailable
	 * @param insideCanvas true when the pointer is inside the terminal gfx canvas bounds
	 * @param dragging true while any mouse button is currently held down
	 * @param dragButton active drag button name: left/right/middle/none
	 */
	public void pushMouseEvent(int button, boolean pressed,
	                           int x, int y, int dx, int dy, int wheel, int cellX, int cellY,
	                           int uiX, int uiY, boolean insideCanvas, boolean dragging, String dragButton) {
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
		t.set("cellX", cellX > 0 ? valueOf(cellX) : NIL);
		t.set("cellY", cellY > 0 ? valueOf(cellY) : NIL);
		t.set("uiX", uiX >= 0 ? valueOf(uiX) : NIL);
		t.set("uiY", uiY >= 0 ? valueOf(uiY) : NIL);
		t.set("insideCanvas", valueOf(insideCanvas));
		t.set("dragging", valueOf(dragging));
		t.set("dragButton", valueOf(dragButton == null ? "none" : dragButton));
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
	 * Claims exclusive keyboard control. While consumed, StarMade key events
	 * are cancelled so the terminal text bar never receives keystrokes. All
	 * key events are still forwarded to the Lua input queue.
	 */
	@LuaMadeCallable
	public void consumeKeyboard() {
		keyboardConsumed = true;
	}

	/**
	 * Releases exclusive keyboard control, restoring normal terminal input
	 * behaviour.
	 */
	@LuaMadeCallable
	public void releaseKeyboard() {
		keyboardConsumed = false;
	}

	/**
	 * Returns true while a script holds exclusive keyboard control.
	 */
	@LuaMadeCallable
	public boolean isKeyboardConsumed() {
		return keyboardConsumed;
	}

	/**
	 * Signals that a script is handling mouse input exclusively.
	 * Query with {@link #isMouseConsumed()} from other parts of your script.
	 */
	@LuaMadeCallable
	public void consumeMouse() {
		mouseConsumed = true;
	}

	/**
	 * Releases the exclusive mouse signal.
	 */
	@LuaMadeCallable
	public void releaseMouse() {
		mouseConsumed = false;
	}

	/**
	 * Returns true while a script has signalled exclusive mouse ownership.
	 */
	@LuaMadeCallable
	public boolean isMouseConsumed() {
		return mouseConsumed;
	}

	/**
	 * Returns the number of events currently waiting in the queue.
	 */
	@LuaMadeCallable
	public int pending() {
		return eventQueue.size();
	}

	/**
	 * Full teardown used when a script is forcibly killed (Ctrl+C / reset).
	 * Clears the event queue and unconditionally releases any keyboard or
	 * mouse locks the script may have held, so the terminal returns to normal
	 * input behaviour immediately.
	 */
	public void reset() {
		eventQueue.clear();
		keyboardConsumed = false;
		mouseConsumed = false;
	}
}

