package interp;
//32 bit ISA

/*modes:
 * m  op    c addr A   addr B   addr C   (abc format)
 * 00 00000 0 00000000 00000000 00000000
 * m  op    c addr A   addr B   seg      (abs format)
 * 01 00000 0 00000000 00000000 00000000
 * m  op    c addr A   imm               (aim format)
 * 10 00000 0 00000000 0000000000000000
 * m  op    c imm                        (imm format)
 * 11 00000 0 000000000000000000000000
 * 
 * m          - instruction mode
 * op         - instruction opcode
 * c          - instruction conditional (high indicates the instruction should be executed if the conditional flag results are true)
 * addr A/B/C - addresses of the words to use stored in data memory
 * seg        - specifies which byte(s) of which argument(s) to use
 * imm        - an immediately available data value
 */

/*word segments:
 * -only available in abs mode, with the exception of the cmp instruction which uses this segment for conditional flags
 * -the seg segment specifies which, if not all, segments of the argument words to use
 * 
 * seg format:
 *  A    B
 *  0000 0000
 * 
 * -each bit corresponds to a byte in the word it is grouped under. Setting bit 0 high allows the left-most byte in word A to be used
 * -it is possible to set only specific bytes in a word using this, but each argument must have the same number of bytes to use else the last specified byte in
 *  the first argument won't be used or the last specified byte in the second argument won't be set, etc.
 * 
 * -note: the [n] or [n-n] operator can be used with a variable name to specify the seg in the high level syntax
 */

/*opcodes:
 * -note that the names of these opcodes are meant for testing purposes and do not represent the final scripting language. For instance, add will be implemented
 *  as the + operator
 * -instructions which don't output to an argument will output to data address 0x01. This can be changed by specifying the address to output to in data address
 *  0x00. Address 0x01 is represented here as D
 * 0x00 - nop - does nothing
 * == math/logic ==
 * 0x01 - add
 *  abc - C = A + B
 *  abs - B[n - n] = A[n - n] + B[n - n]
 *  aim - D = A + imm
 *  imm - D = D + imm
 * 0x02 - sub
 *  abc - C = A - B
 *  abs - B[n - n] = A[n - n] - B[n - n]
 *  aim - D = A - imm
 *  imm - D = D - imm
 * 0x03 - mul
 *  abc - C = A * B
 *  abs - B[n - n] = A[n - n] * B[n - n]
 *  aim - D = A * imm
 *  imm - D = D * imm
 * 0x04 - div
 *  abc - C = A / B
 *  abs - B[n - n] = A[n - n] / B[n - n]
 *  aim - D = A / imm
 *  imm - D = D / imm
 * 0x05 - and
 *  abc - C = A & B
 *  abs - B[n - n] = A[n - n] & B[n - n]
 *  aim - D = A & imm
 *  imm - D = D & imm
 * 0x06 - or
 *  abc - C = A | B
 *  abs - B[n - n] = A[n - n] | B[n - n]
 *  aim - D = A | imm
 *  imm - D = D | imm
 * 0x07 - not
 *  abc - C = ~A
 *  abs - B[n - n] = ~A[n - n]
 *  aim - A = ~imm
 *  imm - D = ~imm
 * 0x08 - xor
 *  abc - C = A ^ B
 *  abs - B[n - n] = A[n - n] ^ B[n - n]
 *  aim - D = A ^ imm
 *  imm - D = D ^ imm
 * 0x09 - shl
 *  abc - C = A << B
 *  abs - B[n - n] = A[n - n] << B[n - n]
 *  aim - D = A << imm
 *  imm - D = D << imm
 * 0x0A - shr
 *  abc - C = A >> B
 *  abs - B[n - n] = A[n - n] >> B[n - n]
 *  aim - D = A >> imm
 *  imm - D = D >> imm
 * == flow control ==
 * 0x0B - cmp
 *  abs - compare A with B based on the conditions
 *  aim - compare A with imm based on the conditions
 * 0x0C - jmp
 *  abs - goes to program address A[n - n]
 *  imm - goes to program address imm
 * -memory
 * 0x0D - set
 *  abc - B = A
 *  abs - B[n - n] = A[n - n]
 *  aim - A = imm
 *  imm - D = imm
 * == IO ==
 * 0xE - in
 *  abc - set A to the received boolean logic value
 * 0xF - out
 *  abc - output the boolean logic value stored in A
 *  imm - output the boolean logic value specified by imm
 * 0x10 - call
 *  abc - call a system function A with input B and output C
 *  imm - call a system function imm
 * 0x11 - print
 *  abs - print A[n - n] to console
 *  aim - print A to console
 *  imm - print imm to console
 */

/*conditionals/flags:
 * -the conditional instruction segment specifies whether that instruction should be executed when the results from a cmp instruction are true or false
 * -the cmp instruction compares the A and B words (abs format) using the word segment segment to select the type(s) of condition(s) to check for
 * -the aim format can also be used with cmp, but the same bits used with seg are pulled from imm
 * segment format:
 *  i m cond
 *  0 0 000000
 * 
 *  i (invert) - performs the equivalent of a logical NOT (! operator) on the comparison result
 *  m (mode)   - setting this bit high requires that all conditions be true for the result to be true
 * 
 *  cond (conditions):
 *   000000 - word A is directly passed to the result
 *   000001 - A > B
 *   000010 - A >= B
 *   000100 - A == B
 *   001000 - A == 0
 *   010000 - A && B
 *   100000 - A || B
 */

public class Interpreter {
    public static void update() {}
}
