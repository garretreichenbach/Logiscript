package dovtech.logiscript.system;

import dovtech.logiscript.program.ScriptProgram;

import java.io.Serializable;

public abstract class ComputerSystem implements Serializable {

    private byte type;
    private int complexity;
    private short[] slots;
    public String uuid;

    /**
     * @return The type of computer system
     * 0 = Micro Controller
     * 1 = Standard Computer
     * 2 = Server
     */
    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getComplexity() {
        return complexity;
    }

    public void setComplexity(int complexity) {
        this.complexity = complexity;
    }

    public short[] getSlots() {
        return slots;
    }

    public void setSlots(short[] slots) {
        this.slots = slots;
    }

    public abstract int getMaxComplexity();

    public abstract float getPowerConsumption();

    public abstract int getMaxInputs();

    public abstract int getMaxOutputs();

    public abstract int getMaxStorage();

    public abstract short[] getMaxSlots();

    public abstract ScriptProgram getCurrentProgram();
}
