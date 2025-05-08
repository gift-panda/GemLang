package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Print implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        if(arguments.getFirst() == null){
            System.out.println("nil");
            return null;
        }

        if(arguments.getFirst() instanceof GemInstance instance){
            GemFunction function = instance.klass.findMethod(Interpreter.mangleName("toString",0));

            if(function != null) {
                function = function.bind(instance);
                Object result = function.call(interpreter, new ArrayList<>());

                System.out.print(result);
                return null;
            }
        }

        if(arguments.getFirst() instanceof Double){
            if(arguments.getFirst().toString().endsWith(".0")){
                System.out.print(arguments.getFirst().toString().replace(".0", ""));
                return null;
            }
        }
        System.out.print(arguments.getFirst());
        return null;
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "print";
    }
}
