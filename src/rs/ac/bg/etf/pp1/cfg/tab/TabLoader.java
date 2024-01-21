package rs.ac.bg.etf.pp1.cfg.tab;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.concepts.Struct;

public class TabLoader {

	protected static StringBuilder output = new StringBuilder();
	protected static final String indent = "   ";
	protected static StringBuilder currentIndent = new StringBuilder();

	public static ArrayList<Obj> globalVariables = new ArrayList<Obj>();
	public static HashMap<String, ArrayList<Obj>> localVariables = new HashMap<>();
	public static HashMap<String, ArrayList<Obj>> fieldVariables = new HashMap<>();
	public static HashMap<Integer, Obj> globalMethods = new HashMap<>(); 
	public static HashMap<Integer, Obj> methodOwner = new HashMap<>();
	
	private static Obj currentOwner = null;
	
	private static HashMap<String, Struct> typeNameMap = new HashMap<String, Struct>();
	
	private static HashMap<String, Integer> kindMap = new HashMap<String, Integer>() {{
	    put("Var", Obj.Var);
	    put("Con", Obj.Con);
	    put("Type", Obj.Type);
	    put("Meth", Obj.Meth);
	    put("Fld", Obj.Fld);
	    put("Elem", Obj.Elem);
	    put("Prog", Obj.Prog);
	}};
	
	private static Stack<Obj> openedScopeObjStack = new Stack<Obj>();
	private static int scopeLevel = 0;
	
	protected static void nextIndentationLevel() {
		currentIndent.append(indent);
	}
	
	private static boolean isEmpty(String line) {
		String regex = "^\\s*$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);

        return matcher.matches();
	}
	
	public static void decode(String filePath) {
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			
			Tab.currentScope = new Scope(null);
			openedScopeObjStack.push(null);			
            
            String currentFunction = "";
            while ((line = br.readLine()) != null) {                
                if (isEmpty(line)) continue;
                
                String regex = "^(\\s*)(\\w+)\\s+([#\\$\\w]+):\\s*(\\w*\\[?\\]?),\\s*(-?\\d+),\\s*(-?\\d+)\\s*$";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(line);

                if (matcher.find()) {
                    int tabCount = matcher.group(1).length() / 3;
                    
                    for(int i = scopeLevel - tabCount; i > 0; i--) {
                    	if (openedScopeObjStack.peek().getKind() == Obj.Type) {
                    		currentOwner = null;
                    	}
                    	if (openedScopeObjStack.peek().getKind() != Obj.Prog) {
	                    	Tab.chainLocalSymbols(openedScopeObjStack.pop());
	                    	Tab.closeScope();
	                    	scopeLevel--;
                    	}
                    }
                    
                    String kindString = matcher.group(2);
                    int kind = kindMap.get(kindString);
                   
                    String name = matcher.group(3);
                    String type = matcher.group(4);
                    int adr = Integer.parseInt(matcher.group(5));
                    int level = Integer.parseInt(matcher.group(6));

                    switch (kind) {
					case Obj.Con: 
						Tab.insert(Obj.Con, name, typeNameMap.get(type));
						break;
					case Obj.Var:
						Obj oVar = Tab.insert(Obj.Var, name, typeNameMap.get(type));
						if (level == 0 || level == -1) {
							globalVariables.add(oVar);
						}
						else {
							if (!localVariables.containsKey(currentFunction)) {
								localVariables.put(currentFunction, new ArrayList<Obj>());
							}
							localVariables.get(currentFunction).add(oVar);
						}
						break;
					case Obj.Type:
						Struct newStruct = new Struct(Struct.None); 
						typeNameMap.put(name, newStruct);
						Obj oType = Tab.insert(Obj.Type, name, newStruct);
						Tab.openScope();
						scopeLevel++;
						openedScopeObjStack.push(oType);
						currentOwner = oType;
						break;
					case Obj.Meth:
						if (currentOwner != null) {
							name = currentOwner.getName() + "." + name;
						}
						currentFunction = name;
						Obj oMeth = Tab.insert(Obj.Meth, name, typeNameMap.get(type));
						
						if (globalMethods.get(adr) == null) {
							globalMethods.put(adr, oMeth);
							methodOwner.put(adr, currentOwner);
						}
						Tab.openScope();
						openedScopeObjStack.push(oMeth);
						scopeLevel++;
						break;
					case Obj.Fld:
						Tab.insert(Obj.Fld, name, typeNameMap.get(type));
						break;
					case Obj.Elem:
						break;
					case Obj.Prog:
						Obj oProg = Tab.insert(Obj.Prog, name, null);
						Tab.openScope();
						openedScopeObjStack.push(oProg);
						scopeLevel++;
						break;

					default:
						break;
					}
                }
            }
            
            
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
}
