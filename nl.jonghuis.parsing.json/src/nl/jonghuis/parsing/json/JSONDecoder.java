package nl.jonghuis.parsing.json;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JSONDecoder {
    public static Object parse(InputStream input) throws IOException {
        return parse(new InputStreamReader(input));
    }

    public static Object parse(Reader reader) throws IOException {
        return new JSONDecoder(reader).parseValue();
    }

    public static Object parse(String string) throws IOException {
        return parse(new StringReader(string));
    }

    private final Reader reader;
    private int lineNumber, charNumber;
    private char c;
    private boolean endOfFile;

    private final StringBuilder buffer = new StringBuilder(512);

    public JSONDecoder(Reader reader) throws IOException {
        this.reader = reader;

        lineNumber = 1;
        charNumber = 0;
        c = 0;
        endOfFile = false;

        // Read the first character and skip all the leading whitespace
        next();
        skipWhitespace();
    }

    public Object parseValue() throws IOException {
        checkEndOfFile();

        switch (c) {
        case '"':
            return parseString();
        case '{':
            return parseObject();
        case '[':
            return parseArray();
        case '-':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            return parseNumber();
        case 't':
            return parseTrue();
        case 'f':
            return parseFalse();
        case 'n':
            return parseNull();
        default:
            throw new JSONParseException("Unexpected character '" + c + "' found", lineNumber, charNumber);
        }
    }

    public Number parseNumber() throws IOException {
        buffer.setLength(0);
        if (c == '-') {
            buffer.append('-');
            next();
        }
        int integerLength = 0, fractionLength = 0, exponentialLength = 0;

        while ((c >= '0') && (c <= '9')) {
            buffer.append(c);
            integerLength++;
            next();
        }

        // Parse the fraction part, if found
        if (c == '.') {
            buffer.append('.');
            next();
            while ((c >= '0') && (c <= '9')) {
                buffer.append(c);
                fractionLength++;
                next();
            }

            if (fractionLength == 0) {
                throw new JSONParseException("Fraction part started, but no digits found", lineNumber, charNumber);
            }
        }

        // Parse the exponential part, if found
        if ((c == 'e') || (c == 'E')) {
            buffer.append('e');
            next();
            if (c == '-') {
                buffer.append('-');
                next();
            } else if (c == '+') {
                next();
            }

            while ((c >= '0') && (c <= '9')) {
                buffer.append(c);
                exponentialLength++;
                next();
            }

            if (exponentialLength == 0) {
                throw new JSONParseException("Exponential part started, but no digits found", lineNumber, charNumber);
            }
        }

        skipWhitespace();

        if ((fractionLength == 0) && (exponentialLength == 0)) {
            // Whole number
            if (integerLength <= 9) {
                return Integer.parseInt(buffer.toString());
            } else if (integerLength <= 18) {
                return Long.parseLong(buffer.toString());
            } else {
                return new BigInteger(buffer.toString());
            }
        } else {
            // Decimal numbers, try and parse as double
            BigDecimal result = new BigDecimal(buffer.toString());
            if (Math.abs(result.scale()) < 1024) {
                return result.doubleValue();
            } else {
                return result;
            }
        }
    }

    public String parseString() throws IOException {
        consume('"', "start of string");
        checkEndOfFile();

        buffer.setLength(0);
        while (true) {
            if ((c < 32) || (c == 127)) {
                throw new JSONParseException("Control character in string found", lineNumber, charNumber);
            }

            switch (c) {
            case '"':
                next();
                skipWhitespace();
                return buffer.toString();
            case '\\':
                next();
                switch (c) {
                case 'b':
                    buffer.append('\b');
                    break;
                case 'f':
                    buffer.append('\f');
                    break;
                case 'n':
                    buffer.append('\n');
                    break;
                case 'r':
                    buffer.append('\r');
                    break;
                case 't':
                    buffer.append('\t');
                    break;
                case 'u':
                    buffer.append(parseUnicodePoint());
                    break;
                default:
                    buffer.append(c);
                    break;
                }
                break;
            default:
                buffer.append(c);
                break;
            }

            next();
            checkEndOfFile();
        }
    }

    private char parseUnicodePoint() throws IOException {
        int unicode = 0;

        for (int ix = 0; ix < 4; ix++) {
            unicode <<= 4;

            next();

            switch (c) {
            case '0':
                unicode += 0;
                break;
            case '1':
                unicode += 1;
                break;
            case '2':
                unicode += 2;
                break;
            case '3':
                unicode += 3;
                break;
            case '4':
                unicode += 4;
                break;
            case '5':
                unicode += 5;
                break;
            case '6':
                unicode += 6;
                break;
            case '7':
                unicode += 7;
                break;
            case '8':
                unicode += 8;
                break;
            case '9':
                unicode += 9;
                break;
            case 'a':
            case 'A':
                unicode += 10;
                break;
            case 'b':
            case 'B':
                unicode += 11;
                break;
            case 'c':
            case 'C':
                unicode += 12;
                break;
            case 'd':
            case 'D':
                unicode += 13;
                break;
            case 'e':
            case 'E':
                unicode += 14;
                break;
            case 'f':
            case 'F':
                unicode += 15;
                break;
            default:
                throw new JSONParseException("Invalid character for unicode character \'"
                                             + c
                                             + "\'",
                                             lineNumber,
                                             charNumber);
            }
        }

        return (char) unicode;
    }

    public JSONArray parseArray() throws IOException {
        consume('[', "start of array");
        skipWhitespace();

        JSONArray array = new JSONArray();
        boolean first = true;
        while (true) {
            skipWhitespace();
            if (c == ']') {
                next();
                skipWhitespace();
                return array;
            } else {
                if (first) {
                    first = false;
                } else {
                    consume(',', "a comma");
                    skipWhitespace();
                }

                array.add(parseValue());
            }
        }
    }

    public JSONObject parseObject() throws IOException {
        consume('{', "start of object");
        skipWhitespace();

        JSONObject object = new JSONObject();
        boolean first = true;
        while (true) {
            if (c == '}') {
                next();
                skipWhitespace();
                return object;
            } else {
                if (first) {
                    first = false;
                } else {
                    consume(',', "a comma");
                    skipWhitespace();
                }

                int startLine = lineNumber;
                int startChar = charNumber;

                String name = parseString();
                consume(':', "colon");
                skipWhitespace();
                Object value = parseValue();

                if (object.put(name, value) != null) {
                    throw new JSONParseException("Duplicate key \"" + name + "\" in object", startLine, startChar);
                }
            }
        }
    }

    public Boolean parseTrue() throws IOException {
        expectedNext('t', 'r', 'u', 'e');
        return true;
    }

    public Boolean parseFalse() throws IOException {
        expectedNext('f', 'a', 'l', 's', 'e');
        return false;
    }

    public Object parseNull() throws IOException {
        expectedNext('n', 'u', 'l', 'l');
        return null;
    }

    private void checkEndOfFile() throws JSONParseException {
        if (endOfFile) {
            throw new JSONParseException("Premature end of file found", lineNumber, charNumber);
        }
    }

    private void expectedNext(char... expectedChars) throws IOException {
        for (char expectedChar : expectedChars) {
            checkEndOfFile();

            if (c != expectedChar) {
                throw new JSONParseException("Unexpected character '"
                                             + c
                                             + "', expected a '"
                                             + expectedChar
                                             + "'",
                                             lineNumber,
                                             charNumber);
            }
            next();
        }
        skipWhitespace();
    }

    private void consume(char expectedChar, String description) throws IOException {
        checkEndOfFile();
        if (c != expectedChar) {
            throw new JSONParseException("Unexpected character '"
                                         + c
                                         + "', expected a "
                                         + description,
                                         lineNumber,
                                         charNumber);
        }
        next();
    }

    private void next() throws IOException {
        int value = reader.read();
        charNumber++;

        if (value < 0) {
            endOfFile = true;
            c = 0;
            return;
        }

        c = (char) value;
        if (c == '\n') {
            lineNumber++;
            charNumber = 0;
        }
    }

    private void skipWhitespace() throws IOException {
        while (!endOfFile && Character.isWhitespace(c)) {
            next();
        }
    }
}
