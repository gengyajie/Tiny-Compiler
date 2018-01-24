import java.util.*;

public class loopOpt {
    public ArrayList<IRCode> IR;
    public HashMap<Integer, HashSet<Integer>> succ;
    public HashMap<Integer, HashSet<Integer>> pred;
    public HashMap<String, Integer> labelMap;
    public HashSet<Integer> leaders;
    public HashMap<Integer, Integer> loopMap; //<loopdominator, tail>

    //reaching definition analysis
    public HashMap<Integer, HashSet<Definition>> gen1;
    public HashMap<Integer, HashSet<Definition>> kill1;
    public HashMap<Integer, HashSet<Definition>> in1;
    public HashMap<Integer, HashSet<Definition>> out1;


    public regAlloc regalloc;

    public loopOpt(ArrayList<IRCode> ircode, HashMap<Integer, Integer> lpmap, SymbolTable symtab) {
        this.IR = ircode;
        this.succ = new HashMap<Integer, HashSet<Integer>>();
        this.pred = new HashMap<Integer, HashSet<Integer>>();
        this.labelMap = new HashMap<String, Integer>();
        this.leaders = new HashSet<Integer>();
        this.loopMap = lpmap;
        this.gen1 = new HashMap<Integer, HashSet<Definition>>();
        this.kill1 = new HashMap<Integer, HashSet<Definition>>();
        this.in1 = new HashMap<Integer, HashSet<Definition>>();
        this.out1 = new HashMap<Integer, HashSet<Definition>>();
        this.regalloc = new regAlloc(ircode);
        this.regalloc.init(symtab);
        this.regalloc.liveAnalysis();
    }

    public void init() {
        if (!IR.isEmpty()) {
            for (int i = 0; i < IR.size(); i++) {
                if (IR.get(i).opcode.equals("LABEL")) {
                    labelMap.put(IR.get(i).result, i);
                }
                succ.put(i, new HashSet<Integer>());
                pred.put(i, new HashSet<Integer>());
                gen1.put(i, new HashSet<Definition>());
                kill1.put(i, new HashSet<Definition>());
                in1.put(i, new HashSet<Definition>());
                out1.put(i, new HashSet<Definition>());
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
                        break;
                    case "WRITEI":
                    case "WRITEF":
                    case "WRITES":
                    case "PUSH":
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        break;
                    case "READI":
                    case "READF":
                    case "READS":
                    case "POP":
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        if (!ir.result.isEmpty()) {
                            gen1.get(i).add(new Definition(ir.result, i));
                        }
                        break;
                    case "STOREI":
                    case "STOREF":
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        gen1.get(i).add(new Definition(ir.result, i));
                        break;
                    case "JSR":
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        break;
                    case "RET":
                        break;
                    case "LABEL":
                    case "LINK":
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        break;
                    default:
                        succ.get(i).add(i + 1);
                        pred.get(i + 1).add(i);
                        gen1.get(i).add(new Definition(ir.result, i));
                        break;
                }
            }
            for (int i = 0; i < IR.size(); i++) {
                if (pred.get(i).size() > 1 || succ.get(i).size() > 1) leaders.add(i);
                else if (!pred.get(i).contains(i-1) || !succ.get(i).contains(i+1)) leaders.add(i);
            }
        }
    }

    public void defAnalysis() {
        while (true) {
            boolean update = false;
            for (int i = 0; i < IR.size(); i++) {
                HashSet<Definition> tmp = new HashSet<Definition>(in1.get(i));
                HashSet<Definition> tmp1 = new HashSet<Definition>();
                for (Definition d : tmp) {
                    if (!d.Redef(gen1.get(i))) tmp1.add(d);
                }
                tmp1.addAll(gen1.get(i));
                boolean upout = out1.get(i).addAll(tmp1);
                tmp1.clear();
                for (int j : pred.get(i)) {
                    tmp1.addAll(out1.get(j));
                }
                boolean upin = in1.get(i).addAll(tmp1);
                update = update || upin || upout;
            }
            if (!update) break;
        }
    }

    public HashMap<Integer, Boolean> idLpInvar(Integer lpheader, Integer lptail) {
        HashSet<Integer> invar = new HashSet<Integer>();
        while (true) {
            int count = invar.size();
            for (int i = lpheader; i <= lptail; i++) {
                switch (IR.get(i).opcode) {
                    case "STOREI":
                    case "STOREF":
                        if (isConst(IR.get(i).op1)) invar.add(i);
                        else if (isCond2(IR.get(i).op1, i, lpheader, lptail)) invar.add(i);
                        else if (isCond3(IR.get(i).op1, i, invar)) invar.add(i);
                        break;
                    case "ADDI":
                    case "ADDF":
                    case "SUBI":
                    case "SUBF":
                    case "MULTI":
                    case "MULTF":
                    case "DIVI":
                    case "DIVF":
                        if (isConst(IR.get(i).op1) || isCond2(IR.get(i).op1, i, lpheader, lptail) || isCond3(IR.get(i).op1, i, invar)) {
                            if (isConst(IR.get(i).op2) || isCond2(IR.get(i).op2, i, lpheader, lptail) || isCond3(IR.get(i).op2, i, invar)) {
                                invar.add(i);
                            }
                        }
                }
            }
            if (invar.size() == count) break;
        }
        System.out.println(";"+ invar);
        HashMap<Integer, Boolean> movable = new HashMap<Integer, Boolean>();
        for (Integer i : invar) {
            int k;
            for (k = i; k <= lptail; k++) {
                if (leaders.contains(k) && pred.get(k).size() > 1) {
                    movable.put(i, false);
                    break;
                }
            }
            if (k > lptail) {
                if (regalloc.in.get(lpheader).contains(IR.get(i).result)) {
                    movable.put(i, false);
                } else {
                    int j;
                    for (j = lpheader; j <= lptail; j++) {
                        if (j != i && IR.get(j).result.equals(IR.get(i).result)) {
                            movable.put(i, false);
                            break;
                        }
                    }
                    if (j > lptail) movable.put(i, true);
                }
            }
        }
        return movable;
    }

    public ArrayList<IRCode> codeMotion() {
        ArrayList<IRCode> ir = new ArrayList<IRCode>();
        HashMap<Integer, Boolean> movable;
        HashSet<Integer> moved = new HashSet<Integer>();
        for (int i = 0; i < IR.size(); i++) {
            if (!IR.get(i).loopHeader && !moved.contains(i)) {
                ir.add(IR.get(i));
            }
            else if (IR.get(i).loopHeader) {
                movable = idLpInvar(i, loopMap.get(i));
                System.out.println(";" + movable);
                Iterator it = movable.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    if ((boolean)pair.getValue()) {
                        ir.add(IR.get((int)pair.getKey()));
                        moved.add((int)pair.getKey());
                    }
                    it.remove(); // avoids a ConcurrentModificationException
                }
                ir.add(IR.get(i));
            }
        }
        return ir;
    }

    public boolean isConst (String b) {
        return b.matches("-?[0-9]+") || b.matches("-?([0-9]+)?(\\.[0-9]*)+");
    }

    public boolean isCond2 (String b, int stat, int lpheader, int lptail) {
        for (Definition d : out1.get(stat)) {
            if (b.equals(d.var) && d.stat > lpheader && d.stat < lptail) return false;
        }
        return true;
    }

    public boolean isCond3 (String b, int stat, HashSet<Integer> invar) {
        int count = 0;
        ArrayList<Definition> def = new ArrayList<Definition>();
        for (Definition d : out1.get(stat)) {
            if (b.equals(d.var)) {
                def.add(d);
                count++;
            }
        }
        if (count != 1) return false;
        else return invar.contains(def.get(0).stat);
    }
}
