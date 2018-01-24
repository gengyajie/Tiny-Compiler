import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;

public class Definition {
    public String var;
    public Integer stat;

    public Definition() {
        var = "";
        stat = -1;
    }

    public Definition(String v, Integer s) {
        var = v;
        stat = s;
    }

    public boolean Redef (HashSet<Definition> gen) {
        for (Definition d : gen) {
            if (this.var.equals(d.var)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "<" + var + ", " + Integer.toString(stat) + ">";
    }
}
