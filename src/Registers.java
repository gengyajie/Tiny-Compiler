public class Registers {
    public SingleReg r0;
    public SingleReg r1;
    public SingleReg r2;
    public SingleReg r3;

    public Registers() {
        this.r0 = new SingleReg("r0");
        this.r1 = new SingleReg("r1");
        this.r2 = new SingleReg("r2");
        this.r3 = new SingleReg("r3");
    }

    public SingleReg findVar(String varname) {
        if (r0.var.equals(varname)) return r0;
        else if (r1.var.equals(varname)) return r1;
        else if (r2.var.equals(varname)) return  r2;
        else if (r3.var.equals(varname)) return r3;
        else return new SingleReg(null);
    }

    public SingleReg findfree() {
        if (!r0.occupied) return r0;
        else if (!r1.occupied) return r1;
        else if (!r2.occupied) return r2;
        else if (!r3.occupied) return r3;
        else return new SingleReg(null);
    }

    public SingleReg findNondirty() {
        if (!r0.dirty) return r0;
        else if (!r1.dirty) return r1;
        else if (!r2.dirty) return r2;
        else if (!r3.dirty) return r3;
        else return new SingleReg(null);
    }

    public SingleReg chooseTofree(String opt1, String tinyop2) {
    //    System.out.println(";" + opt1 + " " + r0.var);

        if (!r0.var.equals(opt1) && !tinyop2.equals("r0")) return r0;
        else if (!r1.var.equals(opt1) && !tinyop2.equals("r1")) {
            return r1;
        }
        else if (!r2.var.equals(opt1) && !tinyop2.equals("r2")) {
            return r2;
        }
        else {
            return r3;
        }
    }

}
