package rs.ac.bg.etf.pp1.cfg.structure;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import rs.ac.bg.etf.pp1.cfg.tab.TabLoader;
import rs.ac.bg.etf.pp1.cfg.ui.MainWindow;

public class Parser {	

	private static byte[] code = new byte[8192+14];;		// code buffer
	private static int cur=0;			// address of next byte to decode
	private static int off;    // size of the header of mj obj file

	private static int jumpDestination;
	public static boolean withTab = false;

	// function -> (calledFunction -> place in code)
	public static HashMap<Integer, HashMap<Integer, Integer>> calledFunctionFromByFunction = new HashMap<>();
	public static HashMap<Integer, HashMap<Integer, String>> invokevirtualByFunction = new HashMap<>();

	public static ArrayList<Instruction> instructions = new ArrayList<>();
	
	public static Set<Integer> borders = new HashSet<>();
	public static HashMap<Integer, Set<Integer>> connections = new HashMap<>();
	
	public static ArrayList<ArrayList<Block>> functions = new ArrayList<>();
	
	public static HashMap<Integer, Integer> functionStartEnd = new HashMap<>();
	private static int currentFunctionStart = 0;
	
	public static ArrayList<Block> blocks = new ArrayList<>();
	
	private static void makeBlocks() {
		ArrayList<Instruction> blockInstr = new ArrayList<Instruction>();
		int blockAdr = instructions.get(0).adr;
		int blockSize = 0;
		for (Instruction i: instructions) {
			if (borders.contains(i.adr) && blockInstr.size() > 0) {
				blocks.add(new Block(blockInstr, blockAdr, blockSize));
				blockAdr = i.adr;
				blockSize = 0;
				blockInstr = new ArrayList<>();
			}
			blockSize += i.size;
			blockInstr.add(i);
		}
		if (blockInstr.size() > 0) {
			blocks.add(new Block(blockInstr, blockAdr, blockSize));
		}
	}
	
	private static void refitConnections() { 
		ArrayList<Integer> borderList = new ArrayList<Integer>(borders);
		Collections.sort(borderList);
		
		borderList.sort(null);
		for (int i = 1; i < borderList.size(); i++) {
			Integer endAdr = borderList.get(i);
			if (connections.containsKey(endAdr)) {
				Set<Integer> to = connections.get(endAdr);
				connections.remove(endAdr);
				connections.put(borderList.get(i-1), to);
			}
		}
	}
	
	private static void makeFunctions() { 
		ArrayList<Block> inFunction = new ArrayList<Block>();
		for (int i = 0; i < blocks.size();) {
			Block b = blocks.get(i);
			if (functionStartEnd.containsKey(b.startAdr)){ // ovaj blok je pocetak funkcije
				inFunction.add(b);
				int sizeLeft = functionStartEnd.get(b.startAdr) - b.startAdr;
				sizeLeft -= blocks.get(i).size;
				for (i++; i < blocks.size() && sizeLeft > 0; i++) { // dodaje sve blokove koji pripadaju ovoj funkciji
					inFunction.add(blocks.get(i));
					sizeLeft -= blocks.get(i).size;
				}
				functions.add(inFunction);
				inFunction = new ArrayList<Block>();
			}
			else {
				i++;
			}
		}
		
	}
	
	private static int get() {
		return ((int)code[cur++ + off])<<24>>>24;
	}
	
	private static int get2() {
		return (get()*256 + get())<<16>>16;
	}
	
	private static int get4() {
		return (get2()<<16) + (get2()<<16>>>16);
	}
	
	private static void setBorder(int addr) {
		borders.add(addr);
	}
	
	private static void setConnection(int from, int to) {
		if (!connections.containsKey(from)) {
			connections.put(from, new HashSet<>());
		}
		connections.get(from).add(to);
	}
	
	private static void removeForwardOnMe() {
		if (connections.containsKey(cur) && connections.get(cur).contains(cur)) {
			connections.get(cur).remove(cur);
		}
	}
	
	private static void startFunction() {
		currentFunctionStart = cur - 1;
		setBorder(cur - 1); 
	}
	
	private static void endFunction() {
		setBorder(cur);
		functionStartEnd.put(currentFunctionStart, cur);
	}
	
	private static String callFunction() {		
		int dist = get2();
		jumpDestination = cur - 3 + dist;
		
		if (!calledFunctionFromByFunction.containsKey(currentFunctionStart)) {
			calledFunctionFromByFunction.put(currentFunctionStart, new HashMap<>());
		}
		
		calledFunctionFromByFunction.get(currentFunctionStart).put(cur, jumpDestination);
		
		setBorder(cur);
		
		removeForwardOnMe();

		return String.valueOf(dist)+" (="+String.valueOf(jumpDestination)+")";
	}
	
	private static String conditionalJump() {
		int dist = get2();
		jumpDestination = cur - 3 + dist;
		
		setConnection(cur, jumpDestination);
		setConnection(cur, cur);
		
		if (jumpDestination > cur) {
			setConnection(jumpDestination, jumpDestination);
		}
		else { 
			if (!borders.contains(jumpDestination)) {
				borders.add(jumpDestination);
				setConnection(jumpDestination, jumpDestination);
			}
		}
		
		setBorder(cur);
		setBorder(jumpDestination);
		
		return String.valueOf(dist)+" (="+String.valueOf(jumpDestination)+")";
	}
	
	private static String unconditionalJump() {
		int dist = get2();
		jumpDestination = cur - 3 + dist;
		
		setConnection(cur, jumpDestination);
		
		if (jumpDestination > cur) {
			setConnection(jumpDestination, jumpDestination);
		}
		else { 
			if (!borders.contains(jumpDestination)) {
				setConnection(jumpDestination, jumpDestination);
			}
		}
		setBorder(cur);
		setBorder(jumpDestination);
		
		removeForwardOnMe();

		return String.valueOf(dist)+" (="+String.valueOf(jumpDestination)+")";
	}
	
	
	private static void addVirtual() {
		Instruction lastInstructin = instructions.get(instructions.size()-1);
		if (!invokevirtualByFunction.containsKey(currentFunctionStart)) {
			invokevirtualByFunction.put(currentFunctionStart, new HashMap<>());
		}
		invokevirtualByFunction.get(currentFunctionStart).put(cur, lastInstructin.arg);
	}
	
	private static void makeBasicBlocks(byte[] c, int len) {
		code = c;
		cur = 0;
		setBorder(cur);
		while (cur < len - off) {
			switch(get()) {
				case  1: { instructions.add(new Instruction(cur - 1, 2, "load", "" + get())); break;}
				case  2: { instructions.add(new Instruction(cur - 1, 1, "load", "0")); break;}
				case  3: { instructions.add(new Instruction(cur - 1, 1, "load", "1")); break;}
				case  4: { instructions.add(new Instruction(cur - 1, 1, "load", "2")); break;}
				case  5: { instructions.add(new Instruction(cur - 1, 1, "load", "3")); break;}
				case  6: { instructions.add(new Instruction(cur - 1, 2, "store", "" + get())); break;}
				case  7: { instructions.add(new Instruction(cur - 1, 1, "store", "0")); break;}
				case  8: { instructions.add(new Instruction(cur - 1, 1, "store", "1")); break;}
				case  9: { instructions.add(new Instruction(cur - 1, 1, "store", "2")); break;}
				case 10: { instructions.add(new Instruction(cur - 1, 1, "store", "3")); break;}
				case 11: { instructions.add(new Instruction(cur - 1, 3, "getstatic", "" + get2())); break;}
				case 12: { instructions.add(new Instruction(cur - 1, 3, "putstatic", "" + get2())); break;}
				case 13: { instructions.add(new Instruction(cur - 1, 3, "getfield", "" + get2())); break;}
				case 14: { instructions.add(new Instruction(cur - 1, 3, "putfield", "" + get2())); break;}
				case 15: { instructions.add(new Instruction(cur - 1, 1, "const", "0")); break;}
				case 16: { instructions.add(new Instruction(cur - 1, 1, "const", "1")); break;}
				case 17: { instructions.add(new Instruction(cur - 1, 1, "const", "2")); break;}
				case 18: { instructions.add(new Instruction(cur - 1, 1, "const", "3")); break;}
				case 19: { instructions.add(new Instruction(cur - 1, 1, "const", "4")); break;}
				case 20: { instructions.add(new Instruction(cur - 1, 1, "const", "5")); break;}
				case 21: { instructions.add(new Instruction(cur - 1, 1, "const", "-1")); break;}
				case 22: { instructions.add(new Instruction(cur - 1, 5, "const", "" + get4())); break;}
				case 23: { instructions.add(new Instruction(cur - 1, 1, "add")); break;}
				case 24: { instructions.add(new Instruction(cur - 1, 1, "sub")); break;}
				case 25: { instructions.add(new Instruction(cur - 1, 1, "mul")); break;}
				case 26: { instructions.add(new Instruction(cur - 1, 1, "div")); break;}
				case 27: { instructions.add(new Instruction(cur - 1, 1, "rem")); break;}
				case 28: { instructions.add(new Instruction(cur - 1, 1, "neg")); break;}
				case 29: { instructions.add(new Instruction(cur - 1, 1, "shl")); break;}
				case 30: { instructions.add(new Instruction(cur - 1, 1, "shr")); break;}
				case 31: { instructions.add(new Instruction(cur - 1, 3, "inc", "" + get(), "" + get())); break;}
				case 32: { instructions.add(new Instruction(cur - 1, 3, "new", "" + get2())); break;}
				case 33: { instructions.add(new Instruction(cur - 1, 2, "newarray", "" + get())); break;}
				case 34: { instructions.add(new Instruction(cur - 1, 1, "aload")); break;}
				case 35: { instructions.add(new Instruction(cur - 1, 1, "astore")); break;}
				case 36: { instructions.add(new Instruction(cur - 1, 1, "baload")); break;}
				case 37: { instructions.add(new Instruction(cur - 1, 1, "bastore")); break;}
				case 38: { instructions.add(new Instruction(cur - 1, 1, "arraylength")); break;}
				case 39: { instructions.add(new Instruction(cur - 1, 1, "pop")); break;}
				case 40: { instructions.add(new Instruction(cur - 1, 1, "dup")); break;}
				case 41: { instructions.add(new Instruction(cur - 1, 1, "dup2")); break;}
				case 42: { instructions.add(new Instruction(cur - 1, 3, "jmp", "" + unconditionalJump()));  break;}
				case 43: { instructions.add(new Instruction(cur - 1, 3, "jeq", "" + conditionalJump())); break;}
				case 44: { instructions.add(new Instruction(cur - 1, 3, "jne", "" + conditionalJump())); break;}
				case 45: { instructions.add(new Instruction(cur - 1, 3, "jlt", "" + conditionalJump())); break;}
				case 46: { instructions.add(new Instruction(cur - 1, 3, "jle", "" + conditionalJump())); break;}
				case 47: { instructions.add(new Instruction(cur - 1, 3, "jgt", "" + conditionalJump())); break;}
				case 48: { instructions.add(new Instruction(cur - 1, 3, "jge", "" + conditionalJump())); break;}
				case 49: { instructions.add(new Instruction(cur - 1, 3, "call", "" + callFunction())); break;}
				case 50: { instructions.add(new Instruction(cur - 1, 1, "return")); endFunction(); break;}
				case 51: { startFunction();
				 		   instructions.add(new Instruction(cur - 1, 3, "enter", "" + get(), "" + get())); break;}
				case 52: { instructions.add(new Instruction(cur - 1, 1, "exit")); break;}
				case 53: { instructions.add(new Instruction(cur - 1, 1, "read")); break;}
				case 54: { instructions.add(new Instruction(cur - 1, 1, "print")); break;}
				case 55: { instructions.add(new Instruction(cur - 1, 1, "bread")); break;}
				case 56: { instructions.add(new Instruction(cur - 1, 1, "bprint")); break;}
				case 57: { instructions.add(new Instruction(cur - 1, 2, "trap ", ""+get())); break;}
				case 58: { 	String name=new String(); 
							int invSize = 1;
			                int a=get4();
			                invSize += 4;
			                while (a!=-1) { name+=(char)a; a=get4(); invSize += 4;}
			                instructions.add(new Instruction(cur - invSize, invSize, "invokevirtual ", name));
			                addVirtual();
	                		setBorder(cur); 
	                		break;
						} 
			}
		}
		
		makeBlocks();
		refitConnections();
	}
	
	public static void runCfgApp(String dotPath, String objPath, String mjTabPath, String folderPath) {
		if (!mjTabPath.isEmpty()) {
			TabLoader.decode(mjTabPath);
			withTab = true;
		}
		
		try {
			InputStream s = new FileInputStream(objPath); 
			int len = s.read(code);
			if (get()!='M' || get()!='J')
			    System.out.println("-- invalid microjava object file");
			else {
			    get4();get4();get4();
			    off=cur;
			    makeBasicBlocks(code, len);
			    makeFunctions();
			    s.close();
		  }
		} catch (IOException e) {
			System.out.println("-- could not open file " + "test/program.obj");
		}
		
		dumpToDotFile(folderPath, dotPath);
		MainWindow.runApp(folderPath);
	}
	
	static void dumpToDotFile(String directoriumName, String dotPath) {
		Path dirPath = Paths.get(directoriumName);

		// Check if the directory exists
        if (!Files.exists(dirPath)) {
            try {
                // Create the directory
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                // Handle the exception, e.g., print an error message
                System.err.println("Error creating folder: " + e.getMessage());
            }
        } 
		
		try {
			
			for (int functionToWrite = 0; functionToWrite < functions.size(); functionToWrite++) {
				int functionAdr = functions.get(functionToWrite).get(0).startAdr;
				String functionName;
				if (withTab) functionName = TabLoader.globalMethods.get(functionAdr).getName();
				else functionName = "" + functionAdr;
	            BufferedWriter writer = new BufferedWriter(new FileWriter(directoriumName + "\\" + functionName + ".dot"));
	            writer.append("digraph G {\r\n"
	            		+ "  graph [fontsize=16, bgcolor=\"#eeeeee\"]\r\n"
	            		+ "  edge [fontsize=16]\r\n"
	            		+ "  node [fontsize=16]\r\n"
	            		+ "  node [width=3 shape=box]\r\n"
	            		+ "  graph [dpi=300]\r\n"
	            		+ "  ranksep = 0.75\r\n"
	            		+ "  nodesep = .5\r\n"
	            		);
	            
	            writer.append("start");
            	writer.append(" [nojustify=true label=\"");
            	writer.append(functionName);
            	writer.append("\" shape=ellipse style=\"rounded,filled\" fillcolor=\"#fafafa\" fontcolor=\"#323232\" color=\"#feb800\" penwidth=2];\r\n");
            
            	writer.append("start");
            	writer.append(":s -> ");
            	writer.append("" + functions.get(functionToWrite).get(0).startAdr);
    			writer.append(":n [color=\"#feb800\"  penwidth=2 style=\"dotted\"");
    			writer.append("];\r\n");
            	
	            
	            for (Block b: functions.get(functionToWrite)) {
	            	writer.append("" + b.startAdr);
	            	writer.append(" [nojustify=true label=\"");
	            	writer.append(b.toString());
	            	writer.append("\" style=\"rounded,filled\" fillcolor=\"#fafafa\" fontcolor=\"#323232\" color=\"#00a3ee\" penwidth=2];\r\n");
	            
	            	if (connections.containsKey(b.startAdr)) {
		            	for (int to: connections.get(b.startAdr)) {
			            	writer.append("" + b.startAdr);
		                	writer.append(":s -> ");
		                	writer.append("" + to);
		        			writer.append(":n [color=\"#7eb900\"  penwidth=2");
		        			writer.append("];\r\n");
		            	}
		            }
	            }
	            	         
	            if (calledFunctionFromByFunction.containsKey(functionAdr))
	            for (Map.Entry<Integer, Integer> pair: calledFunctionFromByFunction.get(functionAdr).entrySet()) {
	            	String calledFunctionName = getFunctionName(pair.getValue());
	            	writer.append("" + getStart(functions.get(functionToWrite), pair.getKey()));
	            	writer.append(":s -> ");
	            	writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
	    			writer.append(":n [color=\"#7eb900\"  penwidth=2 style=dashed];\r\n");
	    			
	    			writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
	            	writer.append(":s -> ");
	            	writer.append("" + pair.getKey());
	    			writer.append(":n [color=\"#7eb900\"  penwidth=2 style=dashed];\r\n");
	    			
	    			writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
	            	writer.append(" [nojustify=true label=\"");
	            	writer.append(calledFunctionName);
	            	writer.append("\" style=\"rounded,filled\" fillcolor=\"#fafafa\" fontcolor=\"#323232\" color=\"#f14f21\" penwidth=2];\r\n");
//	            	
	            }
	            
	            if (invokevirtualByFunction.containsKey(functionAdr))
		            for (Map.Entry<Integer, String> pair: invokevirtualByFunction.get(functionAdr).entrySet()) {
		            	String calledFunctionName = pair.getValue();
		            	writer.append("" + getStart(functions.get(functionToWrite), pair.getKey()));
		            	writer.append(":s -> ");
		            	writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
		    			writer.append(":n [color=\"#7eb900\"  penwidth=2 style=dashed];\r\n");
		    			
		    			writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
		            	writer.append(":s -> ");
		            	writer.append("" + pair.getKey());
		    			writer.append(":n [color=\"#7eb900\"  penwidth=2 style=dashed];\r\n");
		    			
		    			writer.append("\"" + pair.getValue() + " (" + pair.getKey() + ")\"");
		            	writer.append(" [nojustify=true label=< <i>");
		            	writer.append(calledFunctionName);
		            	writer.append("</i>> style=\"rounded,filled\" fillcolor=\"#fafafa\" fontcolor=\"#323232\" color=\"#f14f21\" penwidth=2];\r\n");
		            }
	            
	            writer.append("}");
	            writer.close();
	            	            
	            try {
		        	String command = "\"" + dotPath+ "\" -Tpng " + directoriumName + "\\" + functionName + ".dot -o "+ directoriumName + "\\" + functionName + ".png";

		            // Create a ProcessBuilder for the command
		            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);

		            // Redirect the error stream to the output stream
		            processBuilder.redirectErrorStream(true);

		            // Start the process
		            Process process = processBuilder.start();

		            // Wait for the process to complete
		            process.waitFor();

		        } catch (IOException | InterruptedException e) {
		            e.printStackTrace();
		        }
			}
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("An error occurred while writing to the file.");
        }
	}
	
	private static String getFunctionName(int adr) {
		if (withTab) {
			return TabLoader.globalMethods.get(adr).getName();
		}
		else {
			return "" + adr;
		}
	}
	
	private static int getStart(ArrayList<Block> blocks, int adr) {
		for (Block b: blocks) {
			if (b.startAdr <= adr && b.startAdr + b.size >= adr) {
				return b.startAdr;
			}
		}
		
		return 0;
	}

}
