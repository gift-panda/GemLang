package com.interpreter.gem;

import java.util.List;
import java.util.Map;

public class GemClass implements GemCallable{
    final private String name;
    private final Map<String, GemFunction> methods;

    GemClass(String name, Map<String, GemFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        GemInstance instance = new GemInstance(this);
        return instance;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String name() {
        return name;
    }

    public GemFunction findMethod(String name) {
        if(methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }
}