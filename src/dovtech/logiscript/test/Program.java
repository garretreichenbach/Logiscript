package dovtech.logiscript.test;

//probably put this directly in the launcher class

public class Program {
    String program = ""
        + "dat g1; 123\n"
        + "mov g1, tmp\n"
        + "#loop:\n"
        + "   add 1\n"
        + "   mov acc, tmp\n"
        + "   deb tmp\n"
        + "   jmp #loop\n";
}
