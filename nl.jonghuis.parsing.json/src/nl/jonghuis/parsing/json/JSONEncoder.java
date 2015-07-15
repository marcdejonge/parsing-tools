package nl.jonghuis.parsing.json;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;

public class JSONEncoder {
    public static enum Options {
                                PRETTY
    }

    private Appendable out;
    private boolean pretty;

    public JSONEncoder(Appendable out) {
        this(out, EnumSet.noneOf(Options.class));
    }

    public JSONEncoder(Appendable out, EnumSet<Options> options) {
        this.out = out;

        if (options.contains(Options.PRETTY)) {
            pretty = true;
        }
    }

    public void write(Object input) throws IOException, UnexpectedTypeException {
        write(input, 0);
    }

    private void write(Object input, int indent) throws IOException, UnexpectedTypeException {
        if (input == null) {
            out.append("null");
        } else if (input instanceof Number) {
            out.append(input.toString());
        } else if (input instanceof Boolean) {
            out.append(input.toString());
        } else if (input instanceof CharSequence) {
            write((CharSequence) input);
        } else if (input instanceof List) {
            write((List<?>) input, indent);
        } else {
            write(JSONObject.as(input), indent);
        }
    }

    private int write(CharSequence string) throws IOException {
        int length = 2;
        out.append('\"');
        for (int ix = 0; ix < string.length(); ix++) {
            char c = string.charAt(ix);
            switch (c) {
            case '"':
                length += 2;
                out.append("\\\"");
                break;
            case '\\':
                length += 2;
                out.append("\\\\");
                break;
            case '/':
                length += 2;
                out.append("\\/");
                break;
            case '\b':
                length += 2;
                out.append("\\b");
                break;
            case '\f':
                length += 2;
                out.append("\\f");
                break;
            case '\n':
                length += 2;
                out.append("\\n");
                break;
            case '\r':
                length += 2;
                out.append("\\r");
                break;
            case '\t':
                length += 2;
                out.append("\\t");
                break;
            default:
                if (c < 16) {
                    length += 6;
                    out.append("\\u000" + Integer.toHexString(c));
                } else if ((c < 32) || (c == 127)) {
                    length += 6;
                    out.append("\\u00" + Integer.toHexString(c));
                } else {
                    length += 1;
                    out.append(c);
                }
            }
        }
        out.append('\"');
        return length;
    }

    private void write(List<?> list, int indent) throws IOException, UnexpectedTypeException {
        if (pretty) {
            out.append("[ ");
            indent += 2;
        } else {
            out.append('[');
        }

        boolean first = true;
        for (Object object : list) {
            if (first) {
                first = false;
            } else if (pretty) {
                out.append(",\n");
                indent(indent);
            } else {
                out.append(',');
            }
            write(object, indent);
        }

        out.append(']');
    }

    private void write(JSONObject object, int indent) throws IOException, UnexpectedTypeException {
        if (pretty) {
            out.append("{ ");
            indent += 2;
        } else {
            out.append('{');
        }

        boolean first = true;
        for (Entry<String, Object> entry : object.entrySet()) {
            if (first) {
                first = false;
            } else if (pretty) {
                out.append(",\n");
                indent(indent);
            } else {
                out.append(',');
            }

            int keySize = write(entry.getKey());

            if (pretty) {
                keySize += 3;
                out.append(" : ");
            } else {
                out.append(':');
            }

            write(entry.getValue(), indent + keySize);
        }

        out.append('}');
    }

    private void indent(int indent) throws IOException {
        for (int ix = 0; ix < indent; ix++) {
            out.append(' ');
        }
    }
}
