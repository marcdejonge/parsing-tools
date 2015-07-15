package nl.jonghuis.parsing.json;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class JSONObject extends LinkedHashMap<String, Object> {
    private static final long serialVersionUID = -6339827005039414976L;

    public static final JSONObject as(Object value) throws UnexpectedTypeException {
        if (value instanceof JSONObject) {
            return (JSONObject) value;
        } else if (value instanceof Map) {
            return new JSONObject((Map<?, ?>) value);
        } else {
            return new JSONObject(value);
        }
    }

    public JSONObject() {
    }

    public JSONObject(Map<?, ?> source) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            put(entry.getKey().toString(), entry.getValue());
        }
    }

    public JSONObject(Object source) throws UnexpectedTypeException {
        if (source == null) {
            throw new UnexpectedTypeException("a JavaBean object", "null");
        }

        Class<? extends Object> clazz = source.getClass();

        for (Method method : clazz.getMethods()) {
            if ((method.getReturnType() != Void.TYPE)
                && (method.getParameterTypes().length == 0)
                && Modifier.isPublic(method.getModifiers())) {
                String name = method.getName();
                if (name.equals("getClass")) {
                    continue;
                } else if ((name.length() >= 4) && name.startsWith("get") && Character.isUpperCase(name.charAt(3))) {
                    name = Character.toLowerCase(name.charAt(3)) + name.substring(4);
                    try {
                        Object value = method.invoke(source);
                        if ((value instanceof Number) || (value instanceof String)) {
                            put(name, value);
                        } else if (value instanceof Map) {
                            put(name, JSONObject.as(value));
                        } else if (value instanceof Collection) {
                            put(name, JSONArray.as(value));
                        } else {
                            put(name, JSONObject.as(value));
                        }
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        // Ignored
                    }
                } else if ((name.length() >= 3) && name.startsWith("is") && (method.getReturnType() == Boolean.TYPE)) {
                    try {
                        Object value = method.invoke(source);
                        put(name, value);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        // Ignored
                    }
                }
            }
        }

        if (isEmpty()) {
            throw new UnexpectedTypeException("a JavaBean object", clazz.getName());
        }
    }

    public JSONObject $(String key, Object value) {
        put(key, value);
        return this;
    }

    public <T> T asMap(Class<T> clazz) throws UnexpectedTypeException {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor(Map.class);
            return constructor.newInstance(this);
        } catch (NoSuchMethodException
                 | SecurityException
                 | InstantiationException
                 | IllegalAccessException
                 | IllegalArgumentException
                 | InvocationTargetException e) {
            throw new UnexpectedTypeException("Class "
                                              + clazz.getName()
                                              + " does not have a public constructor that accepts a map",
                                              e);
        }
    }

    public <T> T as(Class<T> clazz) throws UnexpectedTypeException {
        // Try to find a constructor that accepts a JSONObject
        try {
            Constructor<T> constructor = clazz.getConstructor(JSONObject.class);
            return constructor.newInstance(this);
        } catch (InstantiationException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof UnexpectedTypeException) {
                throw (UnexpectedTypeException) cause;
            }
            // Else it failed for some other reason...

            throw new UnexpectedTypeException("Could not find a way to create a "
                                              + clazz
                                              + " from this object: "
                                              + toJson(),
                                              ex);
        } catch (NoSuchMethodException
                 | SecurityException
                 | IllegalAccessException
                 | IllegalArgumentException
                 | InvocationTargetException ex) {
            // Failed, try the next case
            throw new UnexpectedTypeException("Could not find a way to create a "
                                              + clazz
                                              + " from this object: "
                                              + toJson(),
                                              ex);
        }
    }

    public JSONObject getObject(String key) throws UnexpectedTypeException {
        return JSONObject.as(get(key));
    }

    public Number getNumber(String key) throws UnexpectedTypeException {
        Object value = get(key);
        if (!(value instanceof Number)) {
            throw new UnexpectedTypeException("a number", value.getClass().getName());
        }
        return (Number) value;
    }

    public int getInt(String key) throws UnexpectedTypeException {
        return getNumber(key).intValue();
    }

    public long getLong(String key) throws UnexpectedTypeException {
        return getNumber(key).longValue();
    }

    public double getDouble(String key) throws UnexpectedTypeException {
        return getNumber(key).longValue();
    }

    public BigInteger getBigInteger(String key) throws UnexpectedTypeException {
        Number number = getNumber(key);
        if (number == null) {
            throw new UnexpectedTypeException("a big integer", "null object");
        } else if (number instanceof BigInteger) {
            return (BigInteger) number;
        } else {
            return new BigInteger(number.toString());
        }
    }

    public String getString(String key) throws UnexpectedTypeException {
        Object value = get(key);
        if (value == null) {
            throw new UnexpectedTypeException("a string", value);
        }
        return value.toString();
    }

    public JSONArray getArray(String key) throws UnexpectedTypeException {
        return JSONArray.as(get(key));
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
}
