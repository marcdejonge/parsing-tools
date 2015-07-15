package nl.jonghuis.parsing.json;

import java.io.IOException;

public class JSONParseException extends IOException {
    private static final long serialVersionUID = -7836090288074585431L;

    private final int lineNumber;
    private final int charNumber;

    public JSONParseException(String message, int lineNumber, int charNumber) {
        super(message + " @ line " + lineNumber + " character " + charNumber);
        this.lineNumber = lineNumber;
        this.charNumber = charNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getCharNumber() {
        return charNumber;
    }
}
