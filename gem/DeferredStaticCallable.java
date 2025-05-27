package com.interpreter.gem;

import java.util.List;

public class DeferredStaticCallable implements GemCallable {
    private final GemClass klass;
    private final String name;
    private final Token keyword;
    public String parent;

    public DeferredStaticCallable(GemClass instance, String name, Token keyword, String parent) {
        this.klass = instance;
        this.name = name;
        this.keyword = keyword;
        this.parent = parent;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(name, arguments.size());
        GemFunction method = klass.getStaticMethod(mangled);
        if (method == null) {
            Interpreter.runtimeError(keyword, "No method '" + name + "' with " + arguments.size() + " args.", "NameError");
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
        return "<DeferredMethod '" + name + "'>";
    }
}
