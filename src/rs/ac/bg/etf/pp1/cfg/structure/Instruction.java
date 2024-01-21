package rs.ac.bg.etf.pp1.cfg.structure;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rs.ac.bg.etf.pp1.cfg.tab.TabLoader;
import rs.etf.pp1.symboltable.concepts.Obj;

public class Instruction { 
	private static Obj currentFunctionObj = null;

	public int adr;
	public int size; 
	public String instr;
	public String arg;
	public String arg2;
	
	Instruction(int adr, int size, String instr, String arg, String arg2) {
		this.adr = adr;// - off;
		this.size = size;
		this.instr = instr;
		this.arg = arg;
		this.arg2 = arg2;

		if (Parser.withTab) {
			if (instr.equals("load") || instr.equals("store")) localVariable();
			if (instr.equals("getstatic") || instr.equals("putstatic")) globalVariable();
			if (instr.equals("enter")) enterFunction();
			if (instr.equals("call")) callFunction();
		}
	}
	
	Instruction(int adr, int size, String instr, String arg) {
		this(adr, size, instr, arg, null);
	}
	
	Instruction(int adr, int size, String instr) {
		this(adr, size, instr, null);
	}
	
	private void localVariable() {
		try {
			String functionName = currentFunctionObj.getName();
			String localVarName = TabLoader.localVariables.get(functionName).get(Integer.parseInt(arg)).getName();
			arg = localVarName + " (" + functionName + ")";
		} catch (Exception e) {}
	}
	
	
	private void globalVariable() {
		try {
			arg = TabLoader.globalVariables.get(Integer.parseInt(arg)).getName();
		} catch (Exception e) {}
	}
	
	private void enterFunction() {
		currentFunctionObj = TabLoader.globalMethods.get(adr);		
	}
	
	private void callFunction() {		
		String pattern = "(-?\\d+)\\s*\\(=\\s*(\\d+)\\)";

        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(arg);

        if (matcher.matches()) {
            arg = TabLoader.globalMethods.get(Integer.parseInt(matcher.group(2))).getName() + " (= " + matcher.group(2) + ")";
        } else {
            System.out.println("Input string does not match the pattern.");
        }
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("" + adr);
        result.append(": ");
        result.append(instr);
        if (arg != null) {
        	result.append(" ");
        	result.append(arg);
        }
        if (arg2 != null) {
        	result.append(" ");
        	result.append(arg2);
        }
        
        return result.toString();
	}
}