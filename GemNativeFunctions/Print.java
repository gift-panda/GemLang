package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.util.List;

public class Print implements GemNative.Native {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.println(arguments.getFirst());
        return null;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String name() {
        return "print";
    }
}
