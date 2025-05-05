package com.interpreter.gem;

import java.util.List;

public class DeferredCallable implements GemCallable {
    private final GemInstance instance;
    private final String name;

    public DeferredCallable(GemInstance instance, String name) {
        this.instance = instance;
        this.name = name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(name, arguments.size());
        GemFunction method = instance.klass.findMethod(mangled);
        if (method == null) {
            throw new RuntimeError(null, "No method '" + name + "' with " + arguments.size() + " args.");
        }
        return method.bind(instance).call(interpreter, arguments);
    }

    @Override
    public int arity() {
        return 0; // not meaningful — this wrapper defers arity handling
    }

    @Override
    public String name() {
        return null;
    }
}
