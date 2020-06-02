package net.dovtech.logiscript.gui;

import org.schema.schine.graphicsengine.forms.gui.newgui.GUIResizableGrabbableWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.config.WindowPaletteInterface;
import org.schema.schine.input.InputState;

public class TerminalGUI extends GUIResizableGrabbableWindow implements GUIWindowInterface {
    public TerminalGUI(InputState inputState, int i, int i1, String s) {
        super(inputState, i, i1, s);
    }

    public WindowPaletteInterface getWindowPalette() {
        return null;
    }

    protected int getMinWidth() {
        return 0;
    }

    protected int getMinHeight() {
        return 0;
    }

    public int getInnerCornerDistX() {
        return 0;
    }

    public int getInnerCornerTopDistY() {
        return 0;
    }

    public int getInnerCornerBottomDistY() {
        return 0;
    }

    public int getInnerHeigth() {
        return 0;
    }

    public int getInnerWidth() {
        return 0;
    }

    public int getInnerOffsetX() {
        return 0;
    }

    public int getInnerOffsetY() {
        return 0;
    }

    public int getInset() {
        return 0;
    }

    public int getTopDist() {
        return 0;
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void draw() {

    }
}
