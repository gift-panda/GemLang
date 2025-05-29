package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.GemCallable;
import com.interpreter.gem.Interpreter;

import java.util.List;

public class Char implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if (arguments.get(0) instanceof Double ascii) {
            return String.valueOf((char)ascii.intValue());
        }
        return null;
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "char";
    }

    public String toString(){
        return "<native 'char'>";
    }
}
