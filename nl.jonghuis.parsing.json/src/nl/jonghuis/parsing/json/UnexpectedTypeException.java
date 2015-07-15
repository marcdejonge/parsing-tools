package nl.jonghuis.parsing.json;

public class UnexpectedTypeException extends Exception {
    private static final long serialVersionUID = 4456179500435312084L;

    public UnexpectedTypeException(String message) {
        super(message);
    }

    public UnexpectedTypeException(String message, Throwable cause) {
        super(cause);
    }

    public UnexpectedTypeException(String expected, String seen) {
        super("Unexpected type, expected [" + expected + "], but got a [" + seen + "]");
    }

    public UnexpectedTypeException(String expected, Object seen) {
        super("Unexpected type, expected ["
              + expected
              + "], but got a ["
              + (seen == null ? "null object" : seen.getClass().getName())
              + "]");
    }
}
