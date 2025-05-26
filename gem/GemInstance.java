package com.interpreter.gem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GemInstance {
    public GemClass klass;
    public final Map<String, Object> fields = new HashMap<>();

    GemInstance(GemClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        Object string = get("toString");
        if(string != null) {
            return (String) ((GemFunction)string).call(new Interpreter(), List.of());
        }
        return "inst '"+klass+"'";
    }

    public Object get(Token name) {
        Object value = fields.get(name.lexeme);
        if (value != null) return value;

        if (klass.hasOverloadedMethod(name.lexeme)) {
            return new DeferredCallable(this, name.lexeme, name, klass.name());
        }

        GemFunction method = klass.findMethod(Interpreter.mangleName(name.lexeme, 0));
        if (method != null) return method.bind(this);

        Interpreter.runtimeError(name, "Undefined property '" + name.lexeme + "'.");
        return null;
    }

    public Object get(String name) {
        return fields.get(name);
    }


    public void set(Token name, Object value) {
        Interpreter.scopes.get(klass.name()).add(name.lexeme);
        fields.put(name.lexeme, value);
    }

    public void set(String name, Object value) {
        Interpreter.scopes.get(klass.name()).add(name);
        fields.put(name, value);
    }

    public boolean isError(){
        return klass.isError();
    }
}
