package net.dovtech.logiscript.gui;

import org.schema.schine.graphicsengine.forms.gui.newgui.GUIResizableGrabbableWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.WindowPaletteInterface;
import org.schema.schine.input.InputState;

public class TerminalGUI extends GUIResizableGrabbableWindow {

    private WindowPaletteInterface windowPaletteInterface;
    private int minWidth;
    private int minHeight;
    private int innerCornerDistX;
    private int innerCornerTopDistY;
    private int innerCornerBottomDistY;
    private int innerHeight;
    private int innerWidth;
    private int innerOffsetX;
    private int innerOffsetY;
    private int inset;
    private int topDist;

    public TerminalGUI(InputState inputState, int i, int i1, String s) {
        super(inputState, i, i1, s);
    }

    public WindowPaletteInterface getWindowPalette() {
        return windowPaletteInterface;
    }

    protected int getMinWidth() {
        return minWidth;
    }

    protected int getMinHeight() {
        return minHeight;
    }

    public int getInnerCornerDistX() {
        return innerCornerDistX;
    }

    public int getInnerCornerTopDistY() {
        return innerCornerTopDistY;
    }

    public int getInnerCornerBottomDistY() {
        return innerCornerBottomDistY;
    }

    public int getInnerHeigth() {
        return innerHeight;
    }

    public int getInnerWidth() {
        return innerWidth;
    }

    public int getInnerOffsetX() {
        return innerOffsetX;
    }

    public int getInnerOffsetY() {
        return innerOffsetY;
    }

    public int getInset() {
        return inset;
    }

    public int getTopDist() {
        return topDist;
    }

    public void cleanUp() {
    }

    @Override
    public void draw() {

    }
}
