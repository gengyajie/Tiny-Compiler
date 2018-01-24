import java.util.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SemanticListener extends MicroBaseListener{
    private Stack<Integer> lastScope = new Stack<Integer>();
    private int currScope = -1;
    private static IRGenerator IR = new IRGenerator();
    private int codesize = 0;
    private Stack<Integer> endiflb = new Stack<Integer>();
    private Stack<Integer> ifscope = new Stack<Integer>();
    private static int gtlb = -1;
    private Stack<Integer> funcScope = new Stack<Integer>();
    private Queue<String> callResult = new LinkedList<String>();
    private Queue<String> rtType = new LinkedList<String>();
    private Stack<Integer> endfor = new Stack<Integer>();

    public IRGenerator getIR() {
        return IR;
    }

    @Override
    public void enterProgram(MicroParser.ProgramContext ctx) {
        IR.symtab.addScope("GLOBAL", false, "");
        currScope = 0;
        IR.regMap.put(currScope, new ArrayList<Integer>());
        IR.addscope(currScope, new ArrayList<IRCode>());
        IR.addoffset(currScope, 0);
    }

    //Variable Declaration
    @Override
    public void enterString_decl(MicroParser.String_declContext ctx) {
        IR.symtab.content.get(currScope).addString(ctx.id().getText(), "STRING", ctx.str().getText());
        if (IR.symtab.content.get(currScope).isFunc) IR.symtab.content.get(currScope).addLocal(ctx.id().getText());
    }

    @Override
    public void enterVar_decl(MicroParser.Var_declContext ctx) {
        IR.symtab.content.get(currScope).addVar(ctx.id_list().getText(), ctx.var_type().getText());
        if (IR.symtab.content.get(currScope).isFunc) IR.symtab.content.get(currScope).addLocal(ctx.id_list().getText());
    }

    @Override
    public void enterParam_decl(MicroParser.Param_declContext ctx) {
        IR.symtab.content.get(currScope).addVar(ctx.id().getText(), ctx.var_type().getText());
        IR.symtab.content.get(currScope).addPrmtr(ctx.id().getText());
        IR.symtab.FuncType.get(IR.symtab.content.get(currScope).scope_name).add(ctx.var_type().getText());
    }

    //Function Declaration
    @Override
    public void enterFunc_decl(MicroParser.Func_declContext ctx) {
        lastScope.push(currScope);
        IR.symtab.addScope(ctx.id().getText(), true, ctx.any_type().getText());
        IR.symtab.FuncType.put(ctx.id().getText(), new ArrayList<String>());
        IR.symtab.FuncType.get(ctx.id().getText()).add(ctx.any_type().getText());
        currScope = IR.symtab.content.size() - 1;
        IR.regMap.put(currScope, new ArrayList<Integer>());
        funcScope.push(currScope);
        IR.addcode(lastScope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
        IR.addscope(currScope, new ArrayList<IRCode>());
        IR.addoffset(currScope, 0);
        IR.addcode(currScope, IR.generateLB1(ctx.id().getText()));
    }

    @Override
    public void exitFunc_decl(MicroParser.Func_declContext ctx) {
        IR.symtab.content.get(currScope).tempNum = IR.regMap.get(currScope).size();
        IR.addoffset(currScope, IR.getScopecode().get(currScope).size()-1);
        IR.addcode(currScope, IR.generateLink());
        IR.addoffset(currScope, 0);
        if (!IR.scopecode.get(currScope).get(IR.scopecode.get(currScope).size()-1).opcode.equals("RET"))
            IR.addcode(currScope, new IRCode("RET", "", "", ""));
        currScope = lastScope.pop();
        funcScope.pop();
    }

    @Override
    public void exitAssign_expr(MicroParser.Assign_exprContext ctx) {
        ParseTree tree_ex = ctx.expr();
        ArrayList<String> tlist = getExprTerminal(tree_ex);
        tlist.add(0, "=");
        tlist.add(0, ctx.id().getText());
        String type = IR.symtab.findVarType(ctx.id().getText(), currScope, funcScope);
        IR.addAllcode(currScope, IR.generateExpr(tlist, type, funcScope, currScope));
    }

    public ArrayList<String>  getExprTerminal(ParseTree tree_ex) {
        ArrayList<String> tlist = new ArrayList<String>();
        int n = tree_ex.getChildCount();
        for (int i = 0; i < n; i++) {
            if (!tree_ex.getChild(i).getText().isEmpty()) {
                if (tree_ex.getChild(i).getClass().getTypeName().equals("MicroParser$Call_exprContext")) {
                    tlist.add(callResult.poll());
                } else {
                    if (tree_ex.getChild(i).getChildCount() == 0) tlist.add(tree_ex.getChild(i).getText());
                    else tlist.addAll(getExprTerminal(tree_ex.getChild(i)));
                }
            }
        }
        return tlist;
    }

    @Override
    public void exitRead_stmt(MicroParser.Read_stmtContext ctx) {
        String type = "";
        IR.addAllcode(currScope, IR.generateRW("READ", ctx.id_list().getText(), type, currScope, funcScope));
    }

    @Override
    public void exitWrite_stmt(MicroParser.Write_stmtContext ctx) {
        String type = "";
        IR.addAllcode(currScope, IR.generateRW("WRITE", ctx.id_list().getText(), type, currScope, funcScope));
    }

    @Override
    public void exitReturn_stmt(MicroParser.Return_stmtContext ctx) {
        ArrayList<String> tlist = getExprTerminal(ctx.expr());
        IR.addAllcode(currScope, IR.generateReturn(tlist, currScope, funcScope, lastScope));
        IR.addcode(currScope, new IRCode("RET", "", "", ""));
    }

    //Function Call
    @Override
    public void exitCall_expr(MicroParser.Call_exprContext ctx) {
        MicroParser.Expr_listContext ctx_el = ctx.expr_list();
        ArrayList<String> pushList = new ArrayList<String>();
        int i = 1;
        if (!ctx_el.getText().isEmpty()) {
            ArrayList<IRCode> c;
            String type = IR.symtab.FuncType.get(ctx.id().getText()).get(i++);
            ArrayList<String> tlist = getExprTerminal(ctx_el.expr());
            c = IR.generateExpr(tlist, type, funcScope, currScope);
            if (c.isEmpty()) {
                pushList.add(IR.symtab.content.get(funcScope.peek()).varMap.getOrDefault(tlist.get(0), tlist.get(0)));
            }
            else {
                IR.addAllcode(currScope, c);
                pushList.add(c.get(c.size()-1).result);
            }
        }
        MicroParser.Expr_list_tailContext ctx_tail = ctx_el.expr_list_tail();
        while (!ctx_tail.getText().isEmpty()) {
            ArrayList<IRCode> c;
            String type = IR.symtab.FuncType.get(ctx.id().getText()).get(i++);
            ArrayList<String> tlist = getExprTerminal(ctx_tail.expr());
            c = IR.generateExpr(tlist, type, funcScope, currScope);
            if (c.isEmpty()) {
                pushList.add(IR.symtab.content.get(funcScope.peek()).varMap.getOrDefault(tlist.get(0), tlist.get(0)));
            }
            else {
                IR.addAllcode(currScope, c);
                pushList.add(c.get(c.size()-1).result);
            }
            ctx_tail = ctx_tail.expr_list_tail();
        }
        IR.addcode(currScope, new IRCode("PUSH", "", "", ""));
        for (String s : pushList) {
            IR.addcode(currScope, new IRCode("PUSH", s, "", ""));
        }
        IR.addcode(currScope, new IRCode("JSR", "", "", ctx.id().getText()));
        for (int j = 0; j < pushList.size(); j++) {
            IR.addcode(currScope, new IRCode("POP", "", "", ""));
        }
        IR.addcode(currScope, new IRCode("POP", "", "", "$T" + Integer.toString(IR.regMap.get(currScope).size())));
        callResult.offer("$T" + Integer.toString(IR.regMap.get(currScope).size()));
        rtType.offer(IR.symtab.FuncType.get(ctx.id().getText()).get(0));
        IR.regMap.get(currScope).add(IR.regMap.get(currScope).size());
    }

    //Basic Condition
    @Override
    public void exitBasic_cond(MicroParser.Basic_condContext ctx) {
        if (ctx.getText().equals("FALSE")) IR.addcode(currScope, IR.generateJMP(gtlb, false));
        else if (!ctx.getText().equals("TRUE")) {
            ArrayList<String> tlist1 = getExprTerminal(ctx.expr(0));
            String type = null;
            for (String s : tlist1) {
                if (!s.equals("(")) {
                    if (tlist1.get(0).matches("-?[0-9]+")) type = "INT";
                    else if (tlist1.get(0).matches("-?([0-9]+)?(\\.[0-9]*)+")) type = "FLOAT";
                    else if (tlist1.get(0).matches("[A-Za-z][A-Za-z0-9]*")) type = IR.symtab.findVarType(tlist1.get(0), currScope, funcScope);
                    else type = rtType.poll();
                    break;
                }
            }
            ArrayList<String> tlist2 = getExprTerminal(ctx.expr(1));
            IR.addAllcode(currScope, IR.generateBasicCond(tlist1, ctx.compop().getText(), tlist2, type, gtlb, funcScope, currScope));
        }
    }

    //IF Statement
    @Override
    public void enterIf_stmt(MicroParser.If_stmtContext ctx) {
        lastScope.push(currScope);
        IR.symtab.addScope("BLOCK", false, "");
        currScope = IR.symtab.content.size() - 1;
        IR.regMap.put(currScope, new ArrayList<Integer>());
        ifscope.push(currScope);
        IR.addcode(lastScope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
        IR.addscope(currScope, new ArrayList<IRCode>());
        IR.addoffset(currScope, 0);
        gtlb = IR.getLblist().size();
        IR.addcode(currScope, IR.generateLB(false)); //elif_part
        IR.addcode(currScope, IR.generateLB(false)); //ENDIF
        endiflb.push(IR.getLblist().size() - 1);
        IR.addoffset(currScope, 2);
    }

    @Override
    public void exitIf_stmt(MicroParser.If_stmtContext ctx) {
        endiflb.pop();
        ifscope.pop();
    }

    @Override
    public void enterElif_part(MicroParser.Elif_partContext ctx) {
        IR.addcode(currScope, IR.generateJMP(endiflb.peek(), false));
        IR.addoffset(currScope, 1);
        currScope = lastScope.pop();
        if (ctx.getChildCount() > 1) {
            lastScope.push(currScope);
            IR.symtab.addScope("BLOCK", false, "");
            currScope = IR.symtab.content.size() - 1;
            IR.regMap.put(currScope, new ArrayList<Integer>());
            IR.addcode(ifscope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
            IR.addscope(currScope, new ArrayList<IRCode>());
            IR.addoffset(currScope, 0);
            gtlb = IR.getLblist().size();
            IR.addcode(currScope, IR.generateLB(false)); //elif_part
            IR.addoffset(currScope, 1);
        }
    }

    @Override
    public void enterElse_part(MicroParser.Else_partContext ctx) {
        if (!ctx.getText().isEmpty()) {
            lastScope.push(currScope);
            IR.symtab.addScope("BLOCK", false, "");
            currScope = IR.symtab.content.size() - 1;
            IR.regMap.put(currScope, new ArrayList<Integer>());
            IR.addcode(ifscope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
            IR.addscope(currScope, new ArrayList<IRCode>());
            IR.addoffset(currScope, 0);
        }
    }

    @Override
    public void exitElse_part(MicroParser.Else_partContext ctx) {
        if (!ctx.getText().isEmpty()) {
            IR.addoffset(currScope, 0);
            currScope = lastScope.pop();
        }
    }

    //For Statement
    @Override
    public void enterFor_stmt(MicroParser.For_stmtContext ctx) {
        lastScope.push(currScope);
        IR.symtab.addScope("BLOCK", false, "");
        currScope = IR.symtab.content.size() - 1;
        IR.regMap.put(currScope, new ArrayList<Integer>());
        IR.addcode(lastScope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
        IR.addscope(currScope, new ArrayList<IRCode>());
        IR.addoffset(currScope, 0);
    }

    @Override public void exitInit_stmt(MicroParser.Init_stmtContext ctx) {
        IR.addcode(currScope, IR.generateLB(true)); //FOR
        gtlb = IR.getLblist().size();
    }

    @Override
    public void enterIncr_stmt(MicroParser.Incr_stmtContext ctx) {
        codesize = IR.getScopecode().get(currScope).size();
    }

    @Override
    public void exitIncr_stmt(MicroParser.Incr_stmtContext ctx) {
        IR.addcode(currScope, IR.generateJMP(IR.getLblist().size()-1, true));
        IR.addcode(currScope, IR.generateLB(false)); //ENDFOR
        endfor.push(IR.getLblist().size()-1);
        IR.addoffset(currScope, IR.getScopecode().get(currScope).size() - codesize);
    }

    @Override
    public void exitFor_stmt(MicroParser.For_stmtContext ctx) {
        IR.addoffset(currScope, 0);
        currScope = lastScope.pop();
        endfor.pop();
    }

    @Override
    public void enterAug_if_stmt(MicroParser.Aug_if_stmtContext ctx) {
        lastScope.push(currScope);
        IR.symtab.addScope("BLOCK", false, "");
        currScope = IR.symtab.content.size() - 1;
        IR.regMap.put(currScope, new ArrayList<Integer>());
        ifscope.push(currScope);
        IR.addcode(lastScope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
        IR.addscope(currScope, new ArrayList<IRCode>());
        IR.addoffset(currScope, 0);
        gtlb = IR.getLblist().size();
        IR.addcode(currScope, IR.generateLB(false)); //elif_part
        IR.addcode(currScope, IR.generateLB(false)); //ENDIF
        endiflb.push(IR.getLblist().size() - 1);
        IR.addoffset(currScope, 2);
    }

    @Override
    public void exitAug_if_stmt(MicroParser.Aug_if_stmtContext ctx) {
        endiflb.pop();
        ifscope.pop();
    }

    @Override
    public void enterAug_elif_part(MicroParser.Aug_elif_partContext ctx) {
        IR.addcode(currScope, IR.generateJMP(endiflb.peek(), false));
        IR.addoffset(currScope, 1);
        currScope = lastScope.pop();
        if (ctx.getChildCount() > 1) {
            lastScope.push(currScope);
            IR.symtab.addScope("BLOCK", false, "");
            currScope = IR.symtab.content.size() - 1;
            IR.regMap.put(currScope, new ArrayList<Integer>());
            IR.addcode(ifscope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
            IR.addscope(currScope, new ArrayList<IRCode>());
            IR.addoffset(currScope, 0);
            gtlb = IR.getLblist().size();
            IR.addcode(currScope, IR.generateLB(false)); //elif_part
            IR.addoffset(currScope, 1);
        }
    }

    @Override
    public void enterAug_else_part(MicroParser.Aug_else_partContext ctx) {
        if (!ctx.getText().isEmpty()) {
            lastScope.push(currScope);
            IR.symtab.addScope("BLOCK", false, "");
            currScope = IR.symtab.content.size() - 1;
            IR.regMap.put(currScope, new ArrayList<Integer>());
            IR.addcode(ifscope.peek(), new IRCode("GTScope", Integer.toString(currScope), "", ""));
            IR.addscope(currScope, new ArrayList<IRCode>());
            IR.addoffset(currScope, 0);
        }
    }

    @Override
    public void exitAug_else_part(MicroParser.Aug_else_partContext ctx) {
        if (!ctx.getText().isEmpty()) {
            IR.addoffset(currScope, 0);
            currScope = lastScope.pop();
        }
    }

    @Override
    public void enterAug_stmt (MicroParser.Aug_stmtContext ctx) {
        if (ctx.getText().equals("BREAK;")) {
            IR.addcode(currScope, IR.generateJMP(endfor.peek(), false));
        }
    }

}
