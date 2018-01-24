import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.Stack;


public class SymbolTable {
    public Vector<ScopeSymTab> content;
    private int blockNum;
    public HashMap<String, ArrayList<String>> FuncType; //return type, parameter type
    public HashMap<String, Integer> ScopeNameMap;

    public SymbolTable () {
        content = new Vector<ScopeSymTab> ();
        blockNum = 0;
        FuncType = new HashMap<String, ArrayList<String>>();
        ScopeNameMap = new HashMap<String, Integer>();
    }

    public void addScope (String scope_name, boolean isfunc, String r) {
        if (scope_name == "BLOCK") {
            this.content.add(new ScopeSymTab("BLOCK " + Integer.toString(++this.blockNum), isfunc, r));
            ScopeNameMap.put("BLOCK " + Integer.toString(++this.blockNum), content.size()-1);
        }
        else {
            this.content.add(new ScopeSymTab(scope_name, isfunc, r));
            ScopeNameMap.put(scope_name, content.size()-1);
        }
    }

    public void printSymTab () {
        if (!this.content.isEmpty()) {
            for (int i = 0; i < this.content.size() - 1; i++) {
                this.content.get(i).printScopeSymTab();
                System.out.println();
            }
            this.content.get(this.content.size() - 1).printScopeSymTab();
        }
    }

    public String findVarType (String VarName, int CurrScope, Stack<Integer> funcScope) {
        for (int i = 0; i < this.content.get(CurrScope).content.size(); i++) {
            if (this.content.get(CurrScope).content.get(i).name.equals(VarName)) {
                return this.content.get(CurrScope).content.get(i).type;
            }
        }
        for (int i = 0; i < this.content.get(0).content.size(); i++) {
            if (this.content.get(0).content.get(i).name.equals(VarName)) {
                return this.content.get(0).content.get(i).type;
            }
        }
        if (!funcScope.isEmpty()) {
            for (int i = 0; i < this.content.get(funcScope.peek()).content.size(); i++) {
                if (this.content.get(funcScope.peek()).content.get(i).name.equals(VarName)) {
                    return this.content.get(funcScope.peek()).content.get(i).type;
                }
            }
        }
        return null;
    }
}
