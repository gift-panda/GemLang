package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.util.List;

public class Int implements GemCallable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        try{
            double value = Integer.parseInt(arguments.get(0).toString());
            return value;
        }
        catch(Exception e){
            throw new RuntimeException("Cannot convert to integer type");
            //Gem.error((Token)arguments.getLast(), "Cannot convert too integer type.");
        }
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "int";
    }
}
