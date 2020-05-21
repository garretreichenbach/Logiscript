package net.dovtech.logiscript.core;

//Primary emulator module interface

/*ToDo:
 * -Implement assembler, instance management, and emulator
 * -Figure out network side and client side stuff
 * -Write Logiscript ASM tutorial
 * -Plan out GUI
 */

/*Notes:
 * -HamX16 and its tools can be ported and modified
 * -ternary operators are epic
 */

import java.util.ArrayList;

class Instance {
	Instance(int maxDataSize) {
		jumped = false;
		regs = new short[16];
		rom = new byte[maxDataSize];
	}
	
    private boolean jumped; //Used to determine if an instruction modified the pc (mov doesn't count)
    private short[] regs; //Stores register values
    private byte[] rom;  //Stores this instance's program

	public byte[] getRom() {
		return rom;
	}

	void setRom(byte[] rom) {
		this.rom = rom;
	}
}

public class Core {
	private int maxDataSize = 512; //specifies how large a program can be. 512 bytes by default

	private Assembler asm = new Assembler(); //Assembler functions
	//UI	      ui  = new UI();        //Unimplemented module
	
	private ArrayList<Instance> insts = new ArrayList<>(); //contains instance data
	private ArrayList<Integer> inst_alloc = new ArrayList<>();  //keep track of unused elements. Contains unused indices
	private ArrayList<Integer> inst_queue = new ArrayList<>();  //loop through only the used array elements. Contains used indices

	public Core(int maxProgramDataSize) {
		maxDataSize = maxProgramDataSize;
    }

    public void update() {
    	//ToDo: figure out a way to save performance by only updating instances which apply to structures in range?
		//update each instance
		//0x00 = nop and 0x01 = hlt
		//hlt automatically calls removeInstance
		for(int i = 0; i < inst_queue.size(); i ++) {
			//get instruction component data from the instance's rom
			//requires mode, opcode, arg1, arg2, and imm
			byte opcode = 0; //= inst[i].rom & 0b...
			//determine which instruction to call for the instance
			//might be faster to use an array of *fps somehow? Dunno if Java has them
			switch(opcode) {
				case 0x00: { //nop
					//Doesn't do anything except prevent needless opcode checks
					System.out.println("Test");
					break;
				}
				case 0x01: { //hlt
					System.out.println("Emulation halted");
					//remove instance
					break;
				}
			}
		}
    }
    
    public int createInstance(String program) { //used to create an emulation instance when a program is executed in a microcontroller block
    	if(maxDataSize > 0) {
    		if(inst_alloc.size() > 0) { //C++ vectors are nicer
    			int idx = inst_alloc.size() - 1;
    			insts.set(inst_alloc.get(idx), new Instance(maxDataSize));
    			inst_alloc.remove(idx);
    		} else insts.add(new Instance(maxDataSize));
    		
    		int ID = insts.size() - 1;
    		
    		insts.get(ID).setRom(asm.assemble(program, maxDataSize));
		    
	    	return ID; //assign the returned ID to the block which calls this
        }
        
        return -1;
    }

    public void removeInstance(int ID) { //removes an instance after it finishes execution (hlt marks it as finished)
		if(ID > -1 && ID < insts.size()) {
			inst_alloc.add(ID);
			int idx = inst_queue.indexOf(ID);
			if(idx > -1) inst_queue.remove(idx);
		}
    }
}
