import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class regAlloc {
        public ArrayList<IRCode> IR;
        public HashMap<Integer, HashSet<Integer>> succ;
        public HashMap<Integer, HashSet<Integer>> pred;
        public HashSet<String> globalVar;
        public HashMap<String, Integer> labelMap;
        public HashSet<Integer> leaders;
        public HashMap<Integer, HashSet<String>> gen;
        public HashMap<Integer, HashSet<String>> kill;
        public HashMap<Integer, HashSet<String>> in;
        public HashMap<Integer, HashSet<String>> out;

        public regAlloc(ArrayList<IRCode> ircode) {
            this.IR = ircode;
            this.succ = new HashMap<Integer, HashSet<Integer>>();
            this.pred = new HashMap<Integer, HashSet<Integer>>();
            this.globalVar = new HashSet<String>();
            this.labelMap = new HashMap<String, Integer>();
            this.leaders = new HashSet<Integer>();
            this.leaders.add(0);
            this.gen = new HashMap<Integer, HashSet<String>>();
            this.kill = new HashMap<Integer, HashSet<String>>();
            this.in = new HashMap<Integer, HashSet<String>>();
            this.out = new HashMap<Integer, HashSet<String>>();
        }


        public void init(SymbolTable symtab) {
            for (ScopeSymTab.VarEntry v : symtab.content.get(0).content) {
                if (!v.type.equals("STRING")) this.globalVar.add(v.name);
            }
            if (!IR.isEmpty()) {
                for (int i = 0; i < IR.size(); i++) {
                    if (IR.get(i).opcode.equals("LABEL")) {
                        labelMap.put(IR.get(i).result, i);
                    }
                    succ.put(i, new HashSet<Integer>());
                    pred.put(i, new HashSet<Integer>());
                    gen.put(i, new HashSet<String>());
                    kill.put(i, new HashSet<String>());
                    in.put(i, new HashSet<String>());
                    out.put(i, new HashSet<String>());
                }
                for (int i = 0; i < IR.size(); i++) {
                    IRCode ir = IR.get(i);
                    switch (ir.opcode) {
                        case "JUMP":
                            succ.get(i).add(labelMap.get(ir.result));
                            pred.get(labelMap.get(ir.result)).add(i);
                            break;
                        case "GEI":
                        case "LEI":
                        case "NEI":
                        case "EQI":
                        case "GTI":
                        case "LTI":
                        case "GEF":
                        case "LEF":
                        case "NEF":
                        case "EQF":
                        case "GTF":
                        case "LTF":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            succ.get(i).add(labelMap.get(ir.result));
                            pred.get(labelMap.get(ir.result)).add(i);
                            if (globalVar.contains(ir.op1) || ir.op1.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op1);
                            if (globalVar.contains(ir.op2) || ir.op2.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op2);
                            break;
                        case "WRITEI":
                        case "WRITEF":
                        case "WRITES":
                        case "PUSH":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            if (!ir.op1.isEmpty()) {
                                if (globalVar.contains(ir.op1) || ir.op1.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op1);
                            }
                            break;
                        case "READI":
                        case "READF":
                        case "READS":
                        case "POP":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            if (!ir.result.isEmpty()) {
                                kill.get(i).add(ir.result);
                            }
                            break;
                        case "STOREI":
                        case "STOREF":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            if (globalVar.contains(ir.op1) || ir.op1.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op1);
                            kill.get(i).add(ir.result);
                            break;
                        case "JSR":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            gen.get(i).addAll(globalVar);
                            break;
                        case "RET":
                            out.get(i).addAll(globalVar);
                            break;
                        case "LABEL":
                        case "LINK":
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            break;
                        default:
                            succ.get(i).add(i + 1);
                            pred.get(i + 1).add(i);
                            if (globalVar.contains(ir.op1) || ir.op1.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op1);
                            if (globalVar.contains(ir.op2) || ir.op2.matches("\\$[TLP][0-9]+")) gen.get(i).add(ir.op2);
                            kill.get(i).add(ir.result);
                            break;
                    }
                }
                for (int i = 0; i < IR.size(); i++) {
                    if (pred.get(i).size() > 1 || succ.get(i).size() > 1) leaders.add(i);
                    else if (!pred.get(i).contains(i-1) || !succ.get(i).contains(i+1)) leaders.add(i);
                }
            }
        }

        public void liveAnalysis() {
            while (true) {
                boolean update = false;
                for (int i = IR.size() - 1; i >= 0; i--) {
                    HashSet<String> tmp = new HashSet<String>(out.get(i));
                    tmp.removeAll(kill.get(i));
                    tmp.addAll(gen.get(i));
                    boolean upin = in.get(i).addAll(tmp);
                    tmp.clear();
                    for (int j : succ.get(i)) {
                        tmp.addAll(in.get(j));
                    }
                    boolean upout = out.get(i).addAll(tmp);
                    update = update || upin || upout;
                }
                if (!update) break;
            }
        }
}
