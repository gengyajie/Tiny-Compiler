import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Handler;

public class ScopeSymTab {
    public String scope_name;
    public Vector<VarEntry> content;
    public HashMap<String, String> varMap; // <name, local/parameter>
    public ArrayList<Integer> LocVar;
    public ArrayList<Integer> Parameter;
    public boolean isFunc;
    public String returnType;
    public int tempNum;

    public class VarEntry {
        public String name;
        public String type;
        public String value;

        public VarEntry() {
            name = "";
            type = "";
            value = "";
        }

        public VarEntry(String name, String type) {
            this.name = name;
            this.type = type;
            this.value = "";
        }

        public VarEntry(String name, String type, String value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }

        public void printEntry() {
            System.out.print("name " + this.name + " type " + this.type);
            if (this.value != null) System.out.println(" value " + this.value);
            else System.out.println();
        }
    }

    public ScopeSymTab () {
        scope_name = "";
        content = new Vector<VarEntry> ();
        varMap = new HashMap<String, String>();
        LocVar = new ArrayList<Integer>();
        Parameter = new ArrayList<Integer>();
        isFunc = false;
        returnType = "";
    }

    public ScopeSymTab (String scope_name, boolean isfunc, String r) {
        this.scope_name = scope_name;
        content = new Vector<VarEntry> ();
        varMap = new HashMap<String, String>();
        LocVar = new ArrayList<Integer>();
        Parameter = new ArrayList<Integer>();
        isFunc = isfunc;
        returnType = r;
    }

    public boolean isExisted (String varName) {
        if (this.content.size() > 0) {
            for (int i = 0; i < this.content.size(); i++) {
                if (this.content.get(i).name.equals(varName)) {
                    return true;
                }
            }
            return false;
        }
        else return false;
    }

    public void addString (String name, String type, String value) {
        if (!this.isExisted(name)) content.add(new VarEntry(name, type, value));
        else {
            System.out.println("DECLARATION ERROR " + name);
            System.exit(0);
        }
    }

    public void addVar (String name, String type) {
        String[] names = name.split(",");
        for (String tmpName : names){
            if (!this.isExisted(name)) content.add(new VarEntry(tmpName, type));
            else {
                System.out.println("DECLARATION ERROR " + name);
                System.exit(0);
            }
        }
    }

    public void addPrmtr (String id) {
        varMap.put(id, "$P" + Integer.toString(Parameter.size()));
        Parameter.add(Parameter.size());
    }

    public void addLocal (String name) {
        String[] names = name.split(",");
        for (String tmpName : names){
            varMap.put(tmpName, "$L" + Integer.toString(LocVar.size()));
            LocVar.add(LocVar.size());
        }
    }

    public void printScopeSymTab() {
        if (this.scope_name != "") {
            System.out.println("Symbol table " + this.scope_name);
            if (this.content.size() > 0) {
                for (int i = 0; i < this.content.size(); i++) {
                    this.content.get(i).printEntry();
                }
            }
        }
    }
}
