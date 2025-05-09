package com.interpreter.gem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GemInstance {
    public GemClass klass;
    final Map<String, Object> fields = new HashMap<>();

    GemInstance(GemClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        Object string = get("toString");
        if(string != null) {
            return (String) ((GemFunction)string).call(new Interpreter(), List.of());
        }
        return klass.name() + " instance";
    }

    public Object get(Token name) {
        Object value = fields.get(name.lexeme);
        if (value != null) return value;

        if (klass.hasOverloadedMethod(name.lexeme)) {
            return new DeferredCallable(this, name.lexeme, name);
        }


        GemFunction method = klass.findMethod(Interpreter.mangleName(name.lexeme, 0));
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
    }

    public Object get(String name) {
        Object value = fields.get(name);
        if(value != null) return value;


        return null;
    }


    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
