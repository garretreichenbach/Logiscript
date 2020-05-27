package net.dovtech.logiscript.core;

public class Instance {

    //Moved Instance to it's own class file
    private boolean jumped; //Used to determine if an instruction modified the pc (mov doesn't count)
    private short[] regs; //Stores register values
    private byte[] rom;  //Stores this instance's program

    public Instance(int maxDataSize) {
        jumped = false;
        regs = new short[16];
        rom = new byte[maxDataSize];
    }

    public byte[] getRom() {
        return rom;
    }

    public void setRom(byte[] rom) {
        this.rom = rom;
    }
}
