package com.interpreter.gem;

import java.util.List;

public class DeferredSuperCallable implements GemCallable {
    private final GemClass superclass;
    private final GemInstance instance;
    private final String methodName;
    private final Token keyword;

    public DeferredSuperCallable(GemClass superclass, GemInstance instance, String methodName, Token keyword) {
        this.superclass = superclass;
        this.instance = instance;
        this.methodName = methodName;
        this.keyword = keyword;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        String mangled = Interpreter.mangleName(methodName, arguments.size());

        GemFunction method = superclass.findMethod(mangled);
        if (method == null) {
            Interpreter.runtimeError(
                    keyword,
                    "Undefined method '" + methodName + "' with " + arguments.size() + " arguments in superclass.",
                    "NameError"
            );
        }

        return method.bind(instance).call(interpreter, arguments);
    }


    @Override
    public int arity() {
        // Optional: return -1 if you want dynamic checking, or some default
        return 0;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public String toString() {
        return "<DeferredMethod '" + methodName + "'>";
    }
}

