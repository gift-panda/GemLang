package com.interpreter.gem;

import java.util.List;


public class FunctionDispatcher implements GemCallable {
    private final String baseName;
    private final Environment closure;
    private final Token token;

    public FunctionDispatcher(String baseName, Environment closure, Token token) {
        this.baseName = baseName;
        this.closure = closure;
        this.token = token;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(baseName, arguments.size());
        Object target = closure.get(mangled);

        if (!(target instanceof GemCallable callable)) {
            Interpreter.runtimeError(token, "No matching overload for '" + baseName + "' with " + arguments.size() + " arguments.");
            throw new RuntimeException();
        }

        return callable.call(interpreter, arguments);
    }

    @Override
    public int arity() {
        return 255; // allow any arity
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + baseName +">";
    }
}

