package luamade.lua.util;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaError;

public class UtilApi extends LuaMadeUserdata {
	@FunctionalInterface
	public interface CancellationChecker {
		void check();
	}

	private final CancellationChecker cancellationChecker;

	public UtilApi() {
		this(null);
	}

	public UtilApi(CancellationChecker cancellationChecker) {
		this.cancellationChecker = cancellationChecker;
	}

	private void checkCancellation() {
		if(cancellationChecker != null) {
			cancellationChecker.check();
		}
	}

	@LuaMadeCallable
	public long now() {
		checkCancellation();
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public int sleep(int millis) {
		int clamped = Math.max(0, millis);
		int slept = 0;
		try {
			while(slept < clamped) {
				checkCancellation();
				int remaining = clamped - slept;
				int slice = Math.min(remaining, 50);
				Thread.sleep(slice);
				slept += slice;
			}
			checkCancellation();
			return clamped;
		} catch(InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new LuaError("Sleep interrupted");
		}
	}
}
