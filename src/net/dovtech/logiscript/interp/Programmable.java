package net.dovtech.logiscript.interp;

public class Programmable {
    private String[] src;
    private int[] dat;
    private int[] mem;
    private boolean assembled = false;

    public Programmable(int datSize, int memSize) { //The ISA only allows up to 256 bytes of data and memory capacity
        dat = new int[datSize > 0 && datSize < 257? datSize : 256];
        mem = new int[memSize > 0 && memSize < 257? memSize : 256];
    }
}
