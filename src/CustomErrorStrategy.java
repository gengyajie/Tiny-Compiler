import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.IntervalSet;

public class CustomErrorStrategy extends DefaultErrorStrategy {
    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        throw e;
    }
}
