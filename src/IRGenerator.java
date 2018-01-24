import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

public class IRGenerator {
    public HashMap<Integer, ArrayList<Integer>> regMap;
    public ArrayList<Integer> lblist;
    public HashMap<Integer, ArrayList<IRCode>> scopecode; //<currScope, List of IRCode>
    public HashMap<Integer, Integer> offset; //<currScope, offset>
    public SymbolTable symtab;
    public HashMap<Integer, Integer> loopMap; //<loopdominator, tail>

    public IRGenerator () {
        regMap = new HashMap<Integer, ArrayList<Integer>>();
        lblist = new ArrayList<Integer>();
        scopecode = new HashMap<Integer, ArrayList<IRCode>>();
        offset = new HashMap<Integer, Integer>();
        symtab = new SymbolTable();
        loopMap = new HashMap<Integer, Integer>();
    }

    public HashMap<Integer, ArrayList<IRCode>> getScopecode() {
        return scopecode;
    }

    public ArrayList<Integer> getLblist() {
        return lblist;
    }

    public void addoffset (Integer k, Integer v) {
        this.offset.put(k, v);
    }

    public void addscope (Integer k, ArrayList<IRCode> v) {
        this.scopecode.put(k, v);
    }

    public void addcode (Integer currScope, IRCode c) {
        if (this.scopecode.containsKey(currScope)) {
            Integer set = this.offset.get(currScope);
            this.scopecode.get(currScope).add(this.scopecode.get(currScope).size() - set, c);
        }
    }

    public void addAllcode (Integer currScope, ArrayList<IRCode> ir) {
        if (this.scopecode.containsKey(currScope) && this.offset.containsKey(currScope)) {
            Integer set = this.offset.get(currScope);
            this.scopecode.get(currScope).addAll(this.scopecode.get(currScope).size() - set, ir);
        }
    }

    private static int Priority (String op) {
        int i;
        switch(op) {
            case "=" :i=0;break;
            case "(" :i=1;break;
            case "+" :i=2;break;
            case "-" :i=2;break;
            case "*" :i=4;break;
            case "/" :i=4;break;
            case ")" :i=5;break;
            default : i=-1;break;
        }
        return i;
    }

    public static ArrayList<String> toRPlist (ArrayList<String> list) {
        ArrayList<String> rplist = new ArrayList<String>();
        Stack<String> st = new Stack<String>();
        for (int i = 0; i < list.size(); i++) {
            if (!(list.get(i).equals("+")) && !(list.get(i).equals("-")) && !(list.get(i).equals("*")) && !(list.get(i).equals("/")) && !(list.get(i).equals("(")) && !(list.get(i).equals(")")) && !(list.get(i).equals("="))) {
                rplist.add(list.get(i));
            }
            else if (list.get(i).equals("(")) st.push(list.get(i));
            else if (list.get(i).equals(")")) {
                while(!st.peek().equals("(")) rplist.add(st.pop());
                if(st.peek().equals("(")) st.pop();
            }
            else if(st.isEmpty() || Priority(list.get(i)) > Priority(st.peek())) st.push(list.get(i));
            else {
                while(Priority(list.get(i)) <= Priority(st.peek()))
                {
                    rplist.add(st.pop());
                    if(st.isEmpty()) break;
                }
                st.push(list.get(i));
            }
        }
        while (!st.isEmpty()) rplist.add(st.pop());
        return rplist;
    }

    public ArrayList<IRCode> generateCode (String op1, String op, String op2, String type, int funcScope) {
        ArrayList<IRCode> code = new ArrayList<IRCode>();
        String newop1 = op1;
        if (type.equals("INT")) {
            if (op1.matches("-?[0-9]+")) {
                code.add(new IRCode("STOREI", op1, "", "$T" + Integer.toString(regMap.get(funcScope).size())));
                newop1 = "$T" + Integer.toString(regMap.get(funcScope).size());
                regMap.get(funcScope).add(regMap.get(funcScope).size());
            }
            if (op.equals("+")) code.add(new IRCode("ADDI", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("-")) code.add(new IRCode("SUBI", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("*")) code.add(new IRCode("MULTI", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("/")) code.add(new IRCode("DIVI", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("=")) {
                code.add(new IRCode("STOREI", op2, "", newop1));
                return code;
            }
        }
        else {
            if (op1.matches("-?([0-9]+)?(\\.[0-9]*)+")) {
                code.add(new IRCode("STOREF", op1, "", "$T" + Integer.toString(regMap.get(funcScope).size())));
                newop1 = "$T" + Integer.toString(regMap.get(funcScope).size());
                regMap.get(funcScope).add(regMap.get(funcScope).size());
            }
            if (op.equals("+")) code.add(new IRCode("ADDF", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("-")) code.add(new IRCode("SUBF", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("*")) code.add(new IRCode("MULTF", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("/")) code.add(new IRCode("DIVF", newop1, op2, "$T" + Integer.toString(regMap.get(funcScope).size())));
            else if (op.equals("=")) {
                code.add(new IRCode("STOREF", op2, "", newop1));
                return code;
            }
        }
        regMap.get(funcScope).add(regMap.get(funcScope).size());
        return code;
    }

    public ArrayList<IRCode> generateExpr (ArrayList<String> str, String type, Stack<Integer> funcScope, int currScope) {
        ArrayList<String> rplist = toRPlist(str);
        ArrayList<IRCode> code = new ArrayList<IRCode>();
        Stack<String> stack = new Stack<String>();
        for (int i = 0; i < rplist.size(); i++) {
            if (!rplist.get(i).equals("+") && !rplist.get(i).equals("-") && !rplist.get(i).equals("*") && !rplist.get(i).equals("/") && !rplist.get(i).equals("="))
                stack.push(rplist.get(i));
            else {
                String op2 = stack.pop();
                String op1 = stack.pop();
                if (!funcScope.isEmpty()) {
                    if (symtab.content.get(funcScope.peek()).varMap.containsKey(op2))
                        op2 = symtab.content.get(funcScope.peek()).varMap.get(op2);
                    if (symtab.content.get(funcScope.peek()).varMap.containsKey(op1))
                        op1 = symtab.content.get(funcScope.peek()).varMap.get(op1);
                }
                ArrayList<IRCode> c = generateCode(op1, rplist.get(i), op2, type, funcScope.peek());
                code.addAll(c);
                stack.push(c.get(c.size()-1).result);
            }
        }
        return code;
    }

    public ArrayList<IRCode> generateRW (String op, String id_list, String type, int CurrScope, Stack<Integer> funcScope) {
        String idlist = id_list.replaceAll(" ", "");
        ArrayList<IRCode> code = new ArrayList<IRCode>();
        String[] names = idlist.split(",");
        for (String tmpName : names) {
            type = symtab.findVarType(tmpName, CurrScope, funcScope);
            if (!funcScope.isEmpty()) {
                if (symtab.content.get(funcScope.peek()).varMap.containsKey(tmpName))
                    tmpName = symtab.content.get(funcScope.peek()).varMap.get(tmpName);
            }
            IRCode c = new IRCode();
            if (op.equals("READ")) {
                if (type.equals("INT")) c.setIRCode(op + 'I', "", "", tmpName);
                else if (type.equals("FLOAT")) c.setIRCode(op + 'F', "", "", tmpName);
                else c.setIRCode(op + 'S', "", "", tmpName);
            }
            if (op.equals("WRITE")) {
                if (type.equals("INT")) c.setIRCode(op + 'I', tmpName, "", "");
                else if (type.equals("FLOAT")) c.setIRCode(op + 'F', tmpName, "", "");
                else c.setIRCode(op + 'S', tmpName, "", "");
            }
            code.add(c);
        }
        return code;
    }

    public IRCode generateLB (boolean lpheader) {
        IRCode c = new IRCode();
        c.setIRCode("LABEL", "", "", "label" + Integer.toString(lblist.size()));
        lblist.add(lblist.size());
        c.loopHeader = (lpheader);
        return c;
    }

    public IRCode generateLB1 (String lb) {
        IRCode c = new IRCode();
        c.setIRCode("LABEL", "", "", lb);
        return c;
    }

    public IRCode generateJMP (Integer lb, boolean lptail) {
        IRCode c = new IRCode();
        c.setIRCode("JUMP", "", "", "label" + Integer.toString(lb));
        c.loopTail = lptail;
        return c;
    }

    public ArrayList<IRCode> generateBasicCond (ArrayList<String> expr1, String compop, ArrayList<String> expr2, String type, int gtlb, Stack<Integer> funcScope, int currScope) {
        ArrayList<IRCode> code1 = new ArrayList<IRCode>();
        code1.addAll(generateExpr(expr1, type, funcScope, currScope));
        ArrayList<IRCode> code2 = new ArrayList<IRCode>();
        code2.addAll(generateExpr(expr2, type, funcScope, currScope));
        ArrayList<IRCode> code = new ArrayList<IRCode>();
        String op = "";
        switch (compop) {
            case "<": op = "GE"; break;
            case ">": op = "LE"; break;
            case "=": op = "NE"; break;
            case "!=": op = "EQ"; break;
            case "<=": op = "GT"; break;
            case ">=": op = "LT"; break;
            default: break;
        }
        if (type.equals("INT")) {
            String op1;
            String op2;
            if (code1.isEmpty()) {
                if (symtab.content.get(funcScope.peek()).varMap.containsKey(expr1.get(0))) {
                    expr1.set(0, symtab.content.get(funcScope.peek()).varMap.get(expr1.get(0)));
                }
                op1 = expr1.get(0);
            }
            else{
                op1 = code1.get(code1.size() - 1).result;
            }
            if (code2.isEmpty()) {
                if (symtab.content.get(funcScope.peek()).varMap.containsKey(expr2.get(0))) {
                    expr2.set(0, symtab.content.get(funcScope.peek()).varMap.get(expr2.get(0)));
                }
                op2 = expr2.get(0);
            }
            else{
                op2 = code2.get(code2.size() - 1).result;
            }
            if (op2.matches("-?[0-9]+")) {
                code2.add(new IRCode("STOREI", op2, "", "$T" + Integer.toString(regMap.get(funcScope.peek()).size())));
                op2 = "$T" + Integer.toString(regMap.get(funcScope.peek()).size());
                regMap.get(funcScope.peek()).add(regMap.get(funcScope.peek()).size());
            }
            code.addAll(code1);
            code.addAll(code2);
            code.add(new IRCode(op + "I", op1, op2, "label" + Integer.toString(gtlb)));
        }
        else {
            String op1;
            String op2;
            if (code1.isEmpty()) {
                if (symtab.content.get(funcScope.peek()).varMap.containsKey(expr1.get(0))) {
                    expr1.set(0, symtab.content.get(funcScope.peek()).varMap.get(expr1.get(0)));
                }
                op1 = expr1.get(0);
            }
            else{
                op1 = code1.get(code1.size() - 1).result;
            }
            if (code2.isEmpty()) {
                if (symtab.content.get(funcScope.peek()).varMap.containsKey(expr2.get(0))) {
                    expr2.set(0, symtab.content.get(funcScope.peek()).varMap.get(expr2.get(0)));
                }
                op2 = expr2.get(0);
            }
            else{
                op2 = code2.get(code2.size() - 1).result;
            }
            if (op2.matches("-?[0-9]+")) {
                code2.add(new IRCode("STOREF", op2, "", "$T" + Integer.toString(regMap.get(funcScope.peek()).size())));
                op2 = "$T" + Integer.toString(regMap.get(funcScope.peek()).size());
                regMap.get(funcScope.peek()).add(regMap.get(funcScope.peek()).size());
            }
            code.addAll(code1);
            code.addAll(code2);
            code.add(new IRCode(op + "F", op1, op2, "label" + Integer.toString(gtlb)));
        }
        return code;
    }

    public IRCode generateLink () {
        IRCode c = new IRCode();
        c.setIRCode("LINK", "", "", "");
        return c;
    }

    public ArrayList<IRCode> generateReturn (ArrayList<String> expr, int currScope, Stack<Integer> funcScope, Stack<Integer> lastScope) {
        ArrayList<IRCode> code = new ArrayList<IRCode>();
        String type = symtab.content.get(funcScope.peek()).returnType;
        expr.add(0, "=");
        expr.add(0, "$R");
        boolean b = code.addAll(generateExpr(expr, type, funcScope, currScope));
        return code;
    }

    public ArrayList<IRCode> generateAllIRCode (int n) {
        ArrayList<IRCode> ir = new ArrayList<IRCode>();
        ArrayList<IRCode> scope = new ArrayList<IRCode>(scopecode.get(n));
        while (!scope.isEmpty()) {
            if (scope.get(0).opcode.equals("GTScope")) {
                ir.addAll(generateAllIRCode(Integer.valueOf(scope.get(0).op1)));
                scope.remove(0);
            }
            else {
                ir.add(scope.get(0));
                scope.remove(0);
            }
        }
        return ir;
    }

    public void idLoop (ArrayList<IRCode> ir) {
        Stack<Integer> lpheader = new Stack<Integer>();
        for (int i = 0; i < ir.size(); i++) {
            if (ir.get(i).loopHeader) lpheader.push(i);
            if (ir.get(i).loopTail) loopMap.put(lpheader.pop(), i);
        }
    }

}
