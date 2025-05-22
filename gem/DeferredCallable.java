package com.interpreter.gem;

import java.util.List;

public class DeferredCallable implements GemCallable {
    private final GemInstance instance;
    private final String name;
    private final Token keyword;
    public String parent;

    public DeferredCallable(GemInstance instance, String name, Token keyword, String parent) {
        this.instance = instance;
        this.name = name;
        this.keyword = keyword;
        this.parent = parent;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(name, arguments.size());
        GemFunction method = instance.klass.findMethod(mangled);
        if (method == null) {
            Interpreter.runtimeError(keyword, "No method '" + name + "' with " + arguments.size() + " args.");
        }
        return method.bind(instance).call(interpreter, arguments);
    }

    @Override
    public int arity() {
        return 0; // not meaningful — this wrapper defers arity handling
    }

    @Override
    public String name() {
        return "";
    }


    @Override
    public String toString() {
        return "<md " + name + "> (deferred)";
    }
}
