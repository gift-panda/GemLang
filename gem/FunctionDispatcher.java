package com.interpreter.gem;

import java.util.List;

public class FunctionDispatcher implements GemCallable {
    private final String baseName;
    private final Environment closure;

    public FunctionDispatcher(String baseName, Environment closure) {
        this.baseName = baseName;
        this.closure = closure;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(baseName, arguments.size());
        Object target = closure.get(new Token(TokenType.IDENTIFIER, mangled, null, -1));

        if (!(target instanceof GemCallable callable)) {
            throw new RuntimeError(null, "No matching overload for '" + baseName + "' with " + arguments.size() + " arguments.");
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

