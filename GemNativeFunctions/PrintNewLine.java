package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.io.Serializable;
import java.util.List;

public class PrintNewLine implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.println();
        return null;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String name() {
        return "println";
    }

    public String toString(){
        return "<native 'println'>";
    }
}
