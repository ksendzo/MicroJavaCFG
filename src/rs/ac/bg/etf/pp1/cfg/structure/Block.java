package rs.ac.bg.etf.pp1.cfg.structure;

import java.util.ArrayList;

public class Block {
	public int startAdr;
	public int size;
	public ArrayList<Instruction> instructions;
	
	public Block (ArrayList<Instruction> instructions, int start, int size) {
		this.startAdr = start;
		this.size = size;
		this.instructions = instructions;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
        for (Instruction i: instructions) {
        	result.append(i.toString());
        	result.append("\\l");
        }
        return result.toString();
	}
}
