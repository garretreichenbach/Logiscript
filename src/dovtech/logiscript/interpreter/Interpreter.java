package dovtech.logiscript.interpreter;

public class Interpreter { //add some Starmade block API integration stuff. Specific integration in comments near bottom of code
    private static byte m, op, c, A, B, C, seg;
    private static int  imm;
    private static int[] Aseg = new int[4], Bseg = new int[4];
    private static boolean lastCondResult, currentCondResult;
    private static boolean jumped = false;

    //divides an integer into 4 bytes
    private static int[] segment(int data) {
        int[] tmp = new int[4];

        tmp[0] = (seg & 0b0001) != 0? (data & 0x000000FF)          : 0;
        tmp[1] = (seg & 0b0010) != 0? (data & 0x0000FF00) >> 8     : 0;
        tmp[2] = (seg & 0b0100) != 0? (data & 0x00FF0000) >> 8 * 2 : 0;
        tmp[3] = (seg & 0b1000) != 0? (data & 0xFF000000) >> 8 * 3 : 0;

        return tmp;
    }

    //returns an integer mask of bytes which are selected by seg. The returned value is ANDed with a data integer
    private static int clearBits() {
        return (((seg & 0b1000) != 0? 0x00 : 0xFF) << 24) |
               (((seg & 0b0100) != 0? 0x00 : 0xFF) << 16) |
               (((seg & 0b0010) != 0? 0x00 : 0xFF) << 8)  |
                ((seg & 0b0001) != 0? 0x00 : 0xFF);
    }

    //updates an InterpeterInstance
    public static void update(InterpreterInstance instance) {
        //Set up mode, opcode, and conditional bits
        m  = (byte)((instance.instr[instance.counter] & 0b11000000000000000000000000000000) >>> 30);
        op = (byte)((instance.instr[instance.counter] & 0b00111110000000000000000000000000) >>> 25);
        c  = (byte)((instance.instr[instance.counter] & 0b00000001000000000000000000000000) >>> 24);
        
        //Set up relevant argument fields
        switch(m) {
            case 0b00: //ABC
                A = (byte)((instance.instr[instance.counter] & 0b00000000111111110000000000000000) >>> 16);
                B = (byte)((instance.instr[instance.counter] & 0b00000000000000001111111100000000) >>> 8);
                C = (byte)(instance.instr[instance.counter]  & 0b00000000000000000000000011111111);
                break;
            case 0b01: //ABS - could just reuse C
                A   = (byte)((instance.instr[instance.counter] & 0b00000000111111110000000000000000) >>> 16);
                B   = (byte)((instance.instr[instance.counter] & 0b00000000000000001111111100000000) >>> 8);
                seg = (byte)(instance.instr[instance.counter]  & 0b00000000000000000000000000001111);
                break;
            case 0b10: //AIM
                A   = (byte)((instance.instr[instance.counter] & 0b00000000111111110000000000000000) >>> 16);
                imm = instance.instr[instance.counter]         & 0b00000000000000001111111111111111;
                break;
            case 0b11: //IMM
                imm = instance.instr[instance.counter]  & 0b00000000111111111111111111111111;
        }

        //Process opcode
        if(op != 0 && (c != 0? currentCondResult : true)) { //make sure the instruction isn't a nop and the conditional is true if used
            if(op < 0x0B) { //ALU instructions
                switch(m) {
                    case 0b00: //ABC
                        instance.data[C] = ALU.call(op - 1, instance.data[A], instance.data[B]);
                        break;
                    case 0b01: //ABS
                        Aseg = segment(instance.data[A]);
                        Bseg = segment(instance.data[B]);
                        instance.data[instance.data[0]] &= clearBits();
                        for(int i = 0; i < 4; i++) instance.data[instance.data[0]] |= (ALU.call(op - 1, Aseg[i], Bseg[i])) << 8 * i;
                        break;
                    case 0b10: //AIM
                        instance.data[instance.data[0]] = ALU.call(op - 1, instance.data[A], imm);
                        break;
                    case 0b11: //IMM
                        instance.data[instance.data[0]] = ALU.call(op - 1, instance.data[instance.data[0]], imm);
                }
                return; //minor optimization
            }

            switch(op) { //other instructions
                case 0x0B: //cmp
                    currentCondResult = Comparator.compare(instance.data[A], instance.data[B], C & 0b00000111);
                    if((C & 0b01000000) != 0) currentCondResult = Comparator.compare(currentCondResult? 1 : 0, lastCondResult? 1 : 0, (C & 0b00111000) >> 3);
                    if((C & 0b10000000) != 0) currentCondResult = !currentCondResult;
                    lastCondResult = currentCondResult;
                    break;
                case 0x0C: //jmp
                    if(m == 0) instance.counter = instance.data[A];
                    else if(m == 3) instance.counter = imm;
                    jumped = true;
                    break;
                case 0x0D: //set
                    switch(m) {
                        case 0b00: //ABC
                            instance.data[B] = instance.data[A];
                            break;
                        case 0b01: //ABS
                            instance.data[B] &= clearBits();
                            Aseg = segment(instance.data[A]);
                            for(int i = 0; i < 4; i++) instance.data[B] |= Aseg[i] << 8 * i;
                            break;
                        case 0b10: //AIM
                            instance.data[A] = imm;
                            break;
                        case 0b11: //IMM
                            instance.data[instance.data[0]] = imm;
                    }
                    break;
                case 0x0E: //in
                    instance.data[A] = 0xFFFFFFFF; //replace this with the input logic signal from this instance's "owner" block
                    break;
                case 0x0F: //out
                    //set the output logic signal for this instance's "owner" block
                    break;
                case 0x10: //call
                    //call a function or get data from the structure entity to which this instance's "owner" block belongs
                    break;
                case 0x11: //print
                    //output to a gui terminal or using the chat/server messages
            }
        }

        //increment program counter
        if(jumped) jumped = false;
        else instance.counter++;
    }
}
