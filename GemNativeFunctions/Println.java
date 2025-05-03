package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.io.Serializable;
import java.util.List;

public class Println implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if(arguments.getFirst() instanceof Double){
            if(arguments.getFirst().toString().endsWith(".0")){
                System.out.println(arguments.getFirst().toString().replace(".0", ""));
                return null;
            }
        }
        System.out.println(arguments.getFirst());
        return null;
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "println";
    }
}
