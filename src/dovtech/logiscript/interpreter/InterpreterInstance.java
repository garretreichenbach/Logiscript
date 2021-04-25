package dovtech.logiscript.interpreter;

public class InterpreterInstance {
    public int[] data;
    public int[] instr;
    public int counter;

    public InterpreterInstance(int dataLen, int instrLen) {
        data    = new int[dataLen];
        instr   = new int[instrLen];
        data[0] = 1; //specify the default output address
    }

    public InterpreterInstance() { //test constructor
        data    = new int[512];
        instr   = new int[1024];
        data[0] = 1;
    }
}
