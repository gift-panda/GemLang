package com.interpreter.gem;

import java.util.HashMap;
import java.util.Map;

public class GemInstance {
    private GemClass klass;
    final Map<String, Object> fields = new HashMap<>();

    GemInstance(GemClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name() + " instance";
    }

    public Object get(Token name) {
        if(fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        GemFunction method = klass.findMethod(name.lexeme);
        if(method != null) {
            return method.bind(this);
        }

        Gem.error(name, "Trying to access undefined property '" + name.lexeme + "'.");
        return null;
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}
