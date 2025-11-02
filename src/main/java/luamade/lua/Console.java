package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.Varargs;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.util.PriorityQueue;
import java.util.Queue;

public class Console extends LuaMadeUserdata {

	private int VERTICAL = 0;
	private int HORIZONTAL = 1;

	private final ComputerModule module;
	private final Queue<Object[]> printQueue = new PriorityQueue<>();
	private StringBuilder textContents = new StringBuilder();
	private int[] cursorPos = {0, 0};

	public Console(ComputerModule module) {
		this.module = module;
		startPrintThread();
	}

	@LuaMadeCallable
	public Long getTime() {
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public Block getBlock() {
		return new Block(module.getSegmentPiece()); //Block is basically a wrapper class for SegmentPiece
	}

	@LuaMadeCallable
	public void print(Varargs vargs) {
		printQueue.add(new Object[] {new Double[] {1.0, 1.0, 1.0, 1.0}, vargs});
	}

	@LuaMadeCallable
	public void display(Varargs vargs) {
		printQueue.add(new Object[] {new Double[] {1.0, 1.0, 1.0, 1.0}, true, vargs});
	}

	@LuaMadeCallable
	public void printColor(Double[] color, Varargs vargs) {
		printQueue.add(new Object[] {color, vargs});
	}

	@LuaMadeCallable
	public void displayColor(Double[] color, Varargs vargs) {
		printQueue.add(new Object[] {color, true, vargs});
	}

	@LuaMadeCallable
	public void printError(Varargs vargs) {
		printQueue.add(new Object[] {new Double[] {1.0, 0.3, 0.3, 1.0}, vargs});
	}

	@LuaMadeCallable
	public void displayError(Varargs vargs) {
		printQueue.add(new Object[] {new Double[] {1.0, 0.3, 0.3, 1.0}, true, vargs});
	}

	public SegmentPiece getSegmentPiece() {
		return module.getSegmentPiece();
	}

	public void sendOutput(String string) {
		try {
			if(getSegmentPiece().getSegmentController() instanceof ManagedSegmentController) {
				ManagedSegmentController<?> managedSegmentController = (ManagedSegmentController<?>) getSegmentPiece().getSegmentController();
				//Todo: output to computer log or something
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	public static void sendError(SegmentPiece segmentPiece, String text) {
		try {
			System.err.println(text);
			//Todo
//			LuaManager.getModule(segmentPiece).getData(segmentPiece).lastOutput += text;
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	private void startPrintThread() {
		Thread printThread = new Thread() {
			@Override
			public void run() {
				try {
					while(true) {
						if(!printQueue.isEmpty()) {
							boolean display = false;
							Object[] objects = printQueue.poll();
							Double[] color = (Double[]) objects[0];
							if(objects[1] instanceof Boolean) display = (Boolean) objects[1];
							Varargs vargs = (Varargs) objects[1];
							StringBuilder string = new StringBuilder();
							for(int i = 1; i <= vargs.narg() && i <= 16; ++i) string.append(vargs.arg(i).toString()).append("\n");
							sendOutput(string.toString());
							if(display) {
								Transform transform = new Transform();
								getSegmentPiece().getTransform(transform);
								RaisingIndication raisingIndication = new RaisingIndication(transform, string.toString(), color[0].floatValue(), color[1].floatValue(), color[2].floatValue(), color[3].floatValue());
								raisingIndication.speed = 0.1f;
								raisingIndication.lifetime = 15.0f;
								HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
							}
						}
						sleep(2000);
					}
				} catch(Exception exception) {
					exception.printStackTrace();
					startPrintThread();
				}
			}
		};
		printThread.start();
	}

	public String getTextContents() {
		return textContents.toString();
	}

	public void setTextContents(String textContents) {
		this.textContents = new StringBuilder(textContents);
		cursorPos[VERTICAL] = getLineNumber();
		cursorPos[HORIZONTAL] = getLinePos();
	}

	public int[] getCursorPos() {
		return cursorPos;
	}

	public int getLineNumber() {
		return textContents.toString().split("\n").length;
	}

	public void setLineNumber(int lineNumber) {
		cursorPos[VERTICAL] = lineNumber;
		trimCursorPos();
	}

	public int getLinePos() {
		String[] lines = textContents.toString().split("\n");
		if(cursorPos[VERTICAL] >= lines.length) return 0;
		return lines[cursorPos[VERTICAL]].length();
	}

	public void setLinePos(int linePos) {
		String[] lines = textContents.toString().split("\n");
		if(cursorPos[VERTICAL] >= lines.length) return;
		cursorPos[HORIZONTAL] = linePos;
		trimCursorPos();
		if(cursorPos[HORIZONTAL] > lines[cursorPos[VERTICAL]].length()) cursorPos[HORIZONTAL] = lines[cursorPos[VERTICAL]].length();
		trimCursorPos();
	}

	private void trimCursorPos() {
		if(cursorPos[VERTICAL] < 0) cursorPos[VERTICAL] = 0;
		if(cursorPos[HORIZONTAL] < 0) cursorPos[HORIZONTAL] = 0;
		if(cursorPos[VERTICAL] >= getLineNumber()) cursorPos[VERTICAL] = getLineNumber() - 1;
		if(cursorPos[HORIZONTAL] >= getLinePos()) cursorPos[HORIZONTAL] = getLinePos() - 1;
	}
}
