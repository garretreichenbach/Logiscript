package Interpreter;

public class Comparator {
    public static boolean compare(int A, int B, int flags) {
        switch(flags) {
            case 0: return A > B;
            case 1: return A < B;
            case 2: return A >= B;
            case 3: return A <= B;
            case 4: return A == B;
            case 5: return A != B;
            case 6: return (A != 0) && (B != 0);
            case 7: return (A != 0) || (B != 0);
            default: return false;
        }
    }
}
