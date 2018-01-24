public class SingleReg {
    public String id;
    public boolean occupied;
    public String var;
    public boolean dirty;

    public SingleReg() {
        this.id = "";
        this.occupied = false;
        this.var = "";
        this.dirty = false;
    }

    public SingleReg(String ID) {
        this.id = ID;
        this.occupied = false;
        this.var = "";
        this.dirty = false;
    }

    public void putVar(String varname) {
        this.var = varname;
        this.occupied = true;
    }

    public void setDirty(boolean Dirty) {
        this.dirty = Dirty;
    }

    public void spillVar() {
        this.occupied = false;
        this.var = "";
        this.dirty = false;
    }

    @Override
    public String toString() {
        return "id " + id + " occupied " + occupied + " var " + var + " dirty " + dirty;
    }
}
