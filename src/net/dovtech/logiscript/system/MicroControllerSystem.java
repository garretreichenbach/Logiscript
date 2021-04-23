package net.dovtech.logiscript.system;

import net.dovtech.logiscript.Logiscript;
import java.util.UUID;

public class MicroControllerSystem extends ComputerSystem {

    public MicroControllerSystem() {
        this.uuid = UUID.randomUUID().toString();
        setType((byte) 0);
    }

    @Override
    public int getMaxComplexity() {
        return Logiscript.getInstance().getConfig("config").getInt("micro-controller-max-complexity");
    }

    @Override
    public float getPowerConsumption() {
        return 0;
    }

    @Override
    public int getMaxInputs() {
        return 0;
    }

    @Override
    public int getMaxOutputs() {
        return 0;
    }

    @Override
    public int getMaxStorage() {
        return 0;
    }

    @Override
    public short[] getMaxSlots() {
        return new short[0];
    }
}
