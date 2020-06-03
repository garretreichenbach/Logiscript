package net.dovtech.logiscript.interp;

/*example code:
 * 0x00 = in //gets state of logic input
 * 0x01 = 1
 * if 0x00 == 0x01
 *  +jump 0x0
 *  -jump @modify
 * @modify
 * call "sys.reactor.boost"
 */

/*operators:
 * assignment/bitwise: =, +, -, *, /, &, |, ~, ^, <<, >>
 * comparison/logical: !=, ==, &&, ||, !, >, <, <=, >=
 */

public class Assembler {
    public static byte[] assemble(String[] src) {
        return null;
    }
}
