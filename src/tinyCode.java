public class tinyCode {
    protected String opcode;
    protected String op1;
    protected String op2;

    public tinyCode () {
        opcode = "";
        op1 = "";
        op2 = "";
    }

    public tinyCode (String s1, String s2, String s3) {
        opcode = s1;
        op1 = s2;
        op2 = s3;
    }

    public void settinyCode (String s1, String s2, String s3) {
        this.opcode = s1;
        this.op1 = s2;
        this.op2 = s3;
    }

    public void printtinycode () {
        if (this.opcode != "") {
            System.out.print(this.opcode + " ");
            if (this.op1 != "") System.out.print(this.op1);
            if (this.op2 != "") System.out.print(" " + this.op2);
        }
        System.out.println();
    }

}
