package nl.jonghuis.parsing.json;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;

public class JSONArray extends ArrayList<Object> {
    private static final long serialVersionUID = 8521808387401671664L;

    public static final JSONArray as(Object value) throws UnexpectedTypeException {
        if (value instanceof JSONArray) {
            return (JSONArray) value;
        } else if (value instanceof Collection<?>) {
            return new JSONArray((Collection<?>) value);
        } else {
            throw new UnexpectedTypeException("a collection", value);
        }
    }

    public JSONArray() {
        super();
    }

    public JSONArray(Collection<?> collection) {
        super(collection);
    }

    public JSONObject getObject(int ix) throws UnexpectedTypeException {
        return JSONObject.as(get(ix));
    }

    public <T extends Collection<?>> T as(Class<T> clazz) throws UnexpectedTypeException {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(Collection.class);
            return constructor.newInstance(this);
        } catch (NoSuchMethodException
                 | SecurityException
                 | InstantiationException
                 | IllegalAccessException
                 | IllegalArgumentException
                 | InvocationTargetException e) {
            throw new UnexpectedTypeException("Class "
                                              + clazz.getName()
                                              + " does not have a public constructor that accepts a collection",
                                              e);
        }
    }

    public Number getNumber(int ix) throws UnexpectedTypeException {
        Object value = get(ix);
        if (!(value instanceof Number)) {
            throw new UnexpectedTypeException("a number", value.getClass().getName());
        }
        return (Number) value;
    }

    public int getInt(int ix) throws UnexpectedTypeException {
        return getNumber(ix).intValue();
    }

    public long getLong(int ix) throws UnexpectedTypeException {
        return getNumber(ix).longValue();
    }

    public double getDouble(int ix) throws UnexpectedTypeException {
        return getNumber(ix).longValue();
    }

    public String getString(int ix) throws UnexpectedTypeException {
        Object value = get(ix);
        if (value == null) {
            throw new UnexpectedTypeException("a string", value);
        }
        return value.toString();
    }

    public JSONArray getArray(int ix) throws UnexpectedTypeException {
        return JSONArray.as(get(ix));
    }

    public String toJson() throws UnexpectedTypeException {
        StringWriter w = new StringWriter();
        try {
            new JSONEncoder(w).write(this);
        } catch (IOException e) {
            // Should never be possible with a string writer
            throw new AssertionError(e);
        }
        return w.toString();
    }

    @Override
    public String toString() {
        try {
            return toJson();
        } catch (UnexpectedTypeException e) {
            return "<invalid object, contains unexpected types>";
        }
    }

    public JSONArray $(Object value) {
        add(value);
        return this;
    }
}
