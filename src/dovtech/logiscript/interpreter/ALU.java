package dovtech.logiscript.interpreter;

public class ALU {
    public static int call(int index, int x, int y) {
        switch(index) {
            case 0: return  x + y;
            case 1: return  x - y;
            case 2: return  x * y;
            case 3: return  x / y;
            case 4: return  x & y;
            case 5: return  x | y;
            case 6: return ~x;
            case 7: return  x ^ y;
            case 8: return  x <<  y;
            case 9: return  x >>> y;
            default: return 0;
        }
    }
}
