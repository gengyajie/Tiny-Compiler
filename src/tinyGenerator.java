import jdk.nashorn.internal.ir.FunctionCall;

import java.util.ArrayList;
import java.util.HashMap;

public class tinyGenerator {
    private HashMap<Integer, HashMap<String, String>> tinyRegMap;
    private int FuncScope;
    public SymbolTable symtab;
    public ArrayList<tinyCode> tiny;
    public regAlloc regAllocator;
    public Registers registers;
    public HashMap<Integer, Registers> registersHashMap;

    public tinyGenerator(ArrayList<IRCode> ircode, SymbolTable tab) {
        tinyRegMap = new HashMap<Integer, HashMap<String, String>>();
        FuncScope = -1;
        symtab = tab;
        tiny = new ArrayList<tinyCode>();
        regAllocator = new regAlloc(ircode);
        regAllocator.init(tab);
        regAllocator.liveAnalysis();
        registers = new Registers();
        registersHashMap = new HashMap<Integer, Registers>();
    }

    public void regAllocation() {
        for (int i = 0; i < regAllocator.IR.size(); i++) {
        //    System.out.println(registers.r0.var + " " + registers.r1.var + " " + registers.r2.var + " " + registers.r3.var);
            IRCode ircode = regAllocator.IR.get(i);
            switch (ircode.opcode) {
                case "LABEL":
                    tiny.add(new tinyCode("label", ircode.result, ""));
                    if (symtab.ScopeNameMap.containsKey(ircode.result)) {
                        FuncScope = symtab.ScopeNameMap.get(ircode.result);
                        tinyRegMap.put(FuncScope, initTinyRegMap(FuncScope));
                    }
                    break;

                case "JUMP":
                    tiny.add(new tinyCode("jmp", ircode.result, ""));
                    break;

                case "LINK":
                    tiny.add(new tinyCode("link", Integer.toString(symtab.content.get(FuncScope).LocVar.size() + symtab.content.get(FuncScope).tempNum), ""));
                    break;

                case "JSR":
                    tiny.add((new tinyCode("push", "r0", "")));
                    tiny.add((new tinyCode("push", "r1", "")));
                    tiny.add((new tinyCode("push", "r2", "")));
                    tiny.add((new tinyCode("push", "r3", "")));
                    tiny.add(new tinyCode("jsr", ircode.result, ""));
                    tiny.add((new tinyCode("pop", "r3", "")));
                    tiny.add((new tinyCode("pop", "r2", "")));
                    tiny.add((new tinyCode("pop", "r1", "")));
                    tiny.add((new tinyCode("pop", "r0", "")));
                    break;

                case "RET":
                    tiny.add(new tinyCode("unlnk", ircode.result, ""));
                    tiny.add(new tinyCode("ret", ircode.result, ""));
                    break;

                case "STOREI":
                case "STOREF":
                    String tinyop1;
                    String tinyop2;
                    SingleReg rx;
                    SingleReg rz;

                    if (regAllocator.globalVar.contains(ircode.op1) || ircode.op1.matches("\\$[TLP][0-9]+")) {
                        rx = ensure(ircode.op1, ircode.result, i, FuncScope, "");
                        tinyop1 = rx.id;
                        if (!regAllocator.out.get(i).contains(ircode.op1)) free(rx, i, FuncScope);
                    }
                    else tinyop1 = ircode.op1;

                    if (regAllocator.globalVar.contains(ircode.result) || ircode.result.matches("\\$[TLP][0-9]+")) {
                        rz = allocate(ircode.result, ircode.op1, i, FuncScope, "");
                        tinyop2 = rz.id;
                        rz.dirty = true;
                    } else if (ircode.result.equals("$R")) {
                        tinyop2 = tinyRegMap.get(FuncScope).get("$R");
                    } else tinyop2 = ircode.result;

                    if (!tinyop1.equals(tinyop2)) tiny.add(new tinyCode("move", tinyop1, tinyop2));
                    break;

                case "WRITEI":
                    if (regAllocator.globalVar.contains(ircode.op1) || ircode.op1.matches("\\$[TLP][0-9]+")) {
                        rx = ensure(ircode.op1, "", i, FuncScope, "");
                        tinyop2 = rx.id;
                        if (!regAllocator.out.get(i).contains(ircode.op1)) free(rx, i, FuncScope);
                    } else tinyop2 = ircode.op1;
                    tiny.add(new tinyCode("sys", "writei", tinyop2));
                    break;

                case "WRITEF":
                    if (regAllocator.globalVar.contains(ircode.op1) || ircode.op1.matches("\\$[TLP][0-9]+")) {
                        rx = ensure(ircode.op1, "", i, FuncScope, "");
                        tinyop2 = rx.id;
                        if (!regAllocator.out.get(i).contains(ircode.op1)) free(rx, i, FuncScope);
                    } else tinyop2 = ircode.op1;
                    tiny.add(new tinyCode("sys", "writer", tinyop2));
                    break;

                case "WRITES":
                    if (ircode.result.contains("$"))
                        tiny.add(new tinyCode("sys", "writes", tinyRegMap.get(FuncScope).get(ircode.result)));
                    else tiny.add(new tinyCode("sys", "writes", ircode.op1));
                    break;

                case "READI":
                    if (regAllocator.globalVar.contains(ircode.result) || ircode.result.matches("\\$[TLP][0-9]+")) {
                        rz = allocate(ircode.result, "", i, FuncScope, "");
                        tinyop2 = rz.id;
                        rz.dirty = true;
                    } else if (ircode.result.equals("$R")) {
                        tinyop2 = tinyRegMap.get(FuncScope).get("$R");
                    } else tinyop2 = ircode.result;
                    tiny.add(new tinyCode("sys", "readi", tinyop2));
                    break;

                case "READF":
                    if (regAllocator.globalVar.contains(ircode.result) || ircode.result.matches("\\$[TLP][0-9]+")) {
                        rz = allocate(ircode.result, "", i, FuncScope, "");
                        tinyop2 = rz.id;
                        rz.dirty = true;
                    } else if (ircode.result.equals("$R")) {
                        tinyop2 = tinyRegMap.get(FuncScope).get("$R");
                    } else tinyop2 = ircode.result;
                    tiny.add(new tinyCode("sys", "readr", tinyop2));
                    break;

                case "READS":
                    if (ircode.result.contains("$"))
                        tiny.add(new tinyCode("sys", "writes", tinyRegMap.get(FuncScope).get(ircode.result)));
                    else tiny.add(new tinyCode("sys", "writes", ircode.result));
                    break;

                case "PUSH":
                    if (ircode.op1.isEmpty()) tiny.add(new tinyCode("push", "", ""));
                    else {
                        if (regAllocator.globalVar.contains(ircode.op1) || ircode.op1.matches("\\$[TLP][0-9]+")) {
                            rx = ensure(ircode.op1, "", i, FuncScope, "");
                            tinyop1 = rx.id;
                            if (!regAllocator.out.get(i).contains(ircode.op1)) free(rx, i, FuncScope);
                        } else tinyop1 = ircode.op1;
                        tiny.add(new tinyCode("push", tinyop1, ""));
                    }
                    break;

                case "POP":
                    if (ircode.result.isEmpty()) tiny.add(new tinyCode("pop", "", ""));
                    else {
                        if (regAllocator.globalVar.contains(ircode.result) || ircode.result.matches("\\$[TLP][0-9]+")) {
                            rz = allocate(ircode.result, "", i, FuncScope, "");
                            tinyop1 = rz.id;
                            rz.dirty = true;
                        } else if (ircode.result.equals("$R")) {
                            tinyop1 = tinyRegMap.get(FuncScope).get("$R");
                        } else tinyop1 = ircode.result;
                        tiny.add(new tinyCode("pop", tinyop1, ""));
                    }
                    break;

                case "ADDI":
                case "ADDF":
                case "SUBI":
                case "SUBF":
                case "MULTI":
                case "MULTF":
                case "DIVI":
                case "DIVF":
                    String newOpcode = "";
                    switch (ircode.opcode) {
                        case "ADDI":
                            newOpcode = "addi";
                            break;
                        case "ADDF":
                            newOpcode = "addr";
                            break;
                        case "SUBI":
                            newOpcode = "subi";
                            break;
                        case "SUBF":
                            newOpcode = "subr";
                            break;
                        case "MULTI":
                            newOpcode = "muli";
                            break;
                        case "MULTF":
                            newOpcode = "mulr";
                            break;
                        case "DIVI":
                            newOpcode = "divi";
                            break;
                        case "DIVF":
                            newOpcode = "divr";
                            break;
                        default:
                            break;
                    }
                    rx = ensure(ircode.op1, ircode.op2, i, FuncScope, "");
                    tinyop2 = rx.id;
                    free(rx, i, FuncScope);
                    rx.putVar(ircode.result);
                    rx.dirty = true;

                    if (regAllocator.globalVar.contains(ircode.op2) || ircode.op2.matches("\\$[TLP][0-9]+")) {
                        if (ircode.op1.equals(ircode.op2)) tinyop1 = tinyop2;
                        else {
                            rz = ensure(ircode.op2, ircode.op1, i, FuncScope, tinyop2);
                            tinyop1 = rz.id;
                            if (!regAllocator.out.get(i).contains(ircode.op2)) free(rz, i, FuncScope);
                        }
                    }
                    else tinyop1 = ircode.op2;

                    tiny.add(new tinyCode(newOpcode, tinyop1, tinyop2));
                    break;

                default:
                    String cmpType = "";
                    String jmpType = "";
                    switch (ircode.opcode) {
                        case "GEI":
                            cmpType = "cmpi";
                            jmpType = "jge";
                            break;
                        case "LEI":
                            cmpType = "cmpi";
                            jmpType = "jle";
                            break;
                        case "NEI":
                            cmpType = "cmpi";
                            jmpType = "jne";
                            break;
                        case "EQI":
                            cmpType = "cmpi";
                            jmpType = "jeq";
                            break;
                        case "GTI":
                            cmpType = "cmpi";
                            jmpType = "jgt";
                            break;
                        case "LTI":
                            cmpType = "cmpi";
                            jmpType = "jlt";
                            break;
                        case "GEF":
                            cmpType = "cmpr";
                            jmpType = "jge";
                            break;
                        case "LEF":
                            cmpType = "cmpr";
                            jmpType = "jle";
                            break;
                        case "NEF":
                            cmpType = "cmpr";
                            jmpType = "jne";
                            break;
                        case "EQF":
                            cmpType = "cmpr";
                            jmpType = "jeq";
                            break;
                        case "GTF":
                            cmpType = "cmpr";
                            jmpType = "jgt";
                            break;
                        case "LTF":
                            cmpType = "cmpr";
                            jmpType = "jlt";
                            break;
                        default:
                            break;
                    }

                    rx = ensure(ircode.op2, ircode.op1, i, FuncScope, "");
                    tinyop2 = rx.id;

                    if (regAllocator.globalVar.contains(ircode.op1) || ircode.op1.matches("\\$[TLP][0-9]+")) {
                        rz = ensure(ircode.op1, ircode.op2, i, FuncScope, tinyop2);
                    //    System.out.println(";"+tinyop2 + " " + rz.id);
                        tinyop1 = rz.id;
                        if (!regAllocator.out.get(i).contains(ircode.op1)) free(rz, i, FuncScope);
                    } else tinyop1 = ircode.op1;
                    tiny.add(new tinyCode(cmpType, tinyop1, tinyop2));
                    tiny.add(new tinyCode(jmpType, ircode.result, ""));


                    if (!regAllocator.out.get(i).contains(ircode.op2)) free(rx, i, FuncScope);

                    break;
            }
            if (regAllocator.leaders.contains(i+1)) {
                tiny.add(new tinyCode(";BLOCK END, spill all", "", ""));
                free(registers.r0, i, FuncScope);
                free(registers.r1, i, FuncScope);
                free(registers.r2, i, FuncScope);
                free(registers.r3, i, FuncScope);
            }
        }
    }

    public SingleReg ensure(String opt1, String opt2, int stat, int funcScope, String tinyop2) {
        if (registers.findVar(opt1).id!=null) return registers.findVar(opt1);
        else {
        //    System.out.println(opt1 + " " + opt2);
            SingleReg allocate = allocate(opt1, opt2, stat, funcScope, tinyop2);
            SingleReg r = allocate;
            tiny.add(new tinyCode("move", tinyRegMap.get(funcScope).getOrDefault(opt1, opt1), r.id));
            return r;
        }
    }

    public void free(SingleReg r, int stat, int funcScope) {
//        System.out.println(";111 " + r.dirty + " " + regAllocator.out.get(stat).contains(r.var) + " " + r.var);
        if (r.dirty && regAllocator.out.get(stat).contains(r.var)) {
            tiny.add(new tinyCode("move", r.id, tinyRegMap.get(funcScope).getOrDefault(r.var, r.var)));
        }
        r.spillVar();
    }

    public SingleReg allocate(String opt1, String opt2, int stat, int funcScope, String tinyop2) {
        SingleReg r;
        if (registers.findfree().id!=null) {
            r = registers.findfree();
        }
        else {
        //    System.out.println(";opt1" + opt1 + " " + opt2);
            r = registers.chooseTofree(opt2, tinyop2);
        //    System.out.println("; r " + r.id + " " +r.var);
            free(r, stat, funcScope);
        }
        r.putVar(opt1);
        return r;
    }

    public HashMap<String, String> initTinyRegMap(int funcScope) {
        int LocVarNum = symtab.content.get(FuncScope).LocVar.size();
        int ParaNum = symtab.content.get(FuncScope).Parameter.size();
        int TempNum = symtab.content.get(FuncScope).tempNum;

        HashMap<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < LocVarNum; i++) {
            m.put("$L" + Integer.toString(i), "$" + Integer.toString(0 - i - 1));
        }
        for (int i = 0; i < ParaNum; i++) {
            m.put("$P" + Integer.toString(i), "$" + Integer.toString(5 + ParaNum - i));
        }
        for (int i = 0; i < TempNum; i++) {
            m.put("$T" + Integer.toString(i), "$" + Integer.toString(0 - i - 1 - LocVarNum));
        }
        m.put("$R", "$" + Integer.toString(6 + ParaNum));
        return m;
    }

    public ArrayList<tinyCode> generateTiny () {
        for (ScopeSymTab.VarEntry v : symtab.content.get(0).content) {
            if (!v.type.equals("STRING")) {
                tiny.add(new tinyCode("var", v.name, ""));
            }
            else {
                tiny.add(new tinyCode("str", v.name, v.value));
            }
        }
        tiny.add(new tinyCode("push", "", ""));
        tiny.add(new tinyCode("push", "r0", ""));
        tiny.add(new tinyCode("push", "r1", ""));
        tiny.add(new tinyCode("push", "r2", ""));
        tiny.add(new tinyCode("push", "r3", ""));
        tiny.add(new tinyCode("jsr", "main", ""));
        tiny.add(new tinyCode("sys", "halt", ""));

        this.regAllocation();

        tiny.add(new tinyCode("end", "", ""));

        return tiny;
    }
}
