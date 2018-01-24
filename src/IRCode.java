public class IRCode {
    protected String opcode;
    protected String op1;
    protected String op2;
    protected String result;
    public boolean loopHeader;
    public boolean loopTail;
    public boolean loopInvar;

    public IRCode () {
        opcode = "";
        op1 = "";
        op2 = "";
        result = "";
        loopHeader = false;
        loopTail = false;
    }

    public IRCode (String s1, String s2, String s3, String s4) {
        this.opcode = s1;
        this.op1 = s2;
        this.op2 = s3;
        this.result = s4;
        loopHeader = false;
        loopTail = false;
        loopInvar = false;
    }

    public void setIRCode (String s1, String s2, String s3, String s4) {
        this.opcode = s1;
        this.op1 = s2;
        this.op2 = s3;
        this.result = s4;
        loopHeader = false;
        loopTail = false;
        loopInvar = false;
    }

    public void printIRcode () {
        if (this.opcode != "") {
            System.out.print(this.opcode + " ");
            if (this.op1 != "") System.out.print(this.op1 + " ");
            if (this.op2 != "") System.out.print(this.op2 + " ");
            if (this.result != "") System.out.print(this.result);
        }
    }

}
