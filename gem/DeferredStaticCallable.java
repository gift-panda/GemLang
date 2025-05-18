package com.interpreter.gem;

import java.util.List;

public class DeferredStaticCallable implements GemCallable {
    private final GemClass klass;
    private final String name;
    private final Token keyword;

    public DeferredStaticCallable(GemClass instance, String name, Token keyword) {
        this.klass = instance;
        this.name = name;
        this.keyword = keyword;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(name, arguments.size());
        GemFunction method = klass.getStaticMethod(mangled);
        if (method == null) {
            throw new RuntimeError(keyword, "No method '" + name + "' with " + arguments.size() + " args.");
        }
        return method.staticBind().call(interpreter, arguments);
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
