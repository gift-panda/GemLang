package com.interpreter.GemNativeFunctions;

import java.io.Serializable;
import java.util.List;
import com.interpreter.gem.*;

public class Asc implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if(arguments.getFirst() instanceof String){
            return (double)(((String)arguments.getFirst()).charAt(0));
        }

        return arguments.getFirst();
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String toString(){return "<native 'asc'>";}

    @Override
    public String name() {
        return "asc";
    }
}
