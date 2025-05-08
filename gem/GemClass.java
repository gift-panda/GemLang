package com.interpreter.gem;

import java.util.List;
import java.util.Map;

public class GemClass implements GemCallable{
    final private String name;
    public final Map<String, GemFunction> methods;
    final GemClass superclass;

    GemClass(String name, GemClass superclass, Map<String, GemFunction> methods) {
        this.name = name;
        this.methods = methods;
        this.superclass = superclass;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        GemInstance instance = new GemInstance(this);

        String mangled = Interpreter.mangleName("init", arguments.size());
        GemFunction initializer = findMethod(mangled);

        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    public boolean hasOverloadedMethod(String baseName) {
        for (String methodName : methods.keySet()) {
            if (methodName.equals(baseName) || methodName.startsWith(baseName + "$")) {
                return true;
            }
        }
        return false;
    }



    @Override
    public int arity() {
        GemFunction initializer = methods.get("init");
        if(initializer == null) {
            return 0;
        }
        return initializer.arity();
    }

    @Override
    public String name() {
        return name;
    }

    public GemFunction findMethod(String name) {
        if(methods.containsKey(name)) {
            return methods.get(name);
        }
        if(superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }
}