package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;
import com.interpreter.gem.Environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Println implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if(arguments.getFirst() == null){
            System.out.println("nil");
            return null;
        }
        if(arguments.getFirst() instanceof GemInstance instance){
            GemFunction function = instance.klass.findMethod(Interpreter.mangleName("toString",0));



            if(function != null) {
                function = function.bind(instance);
                Object result = function.call(interpreter, new ArrayList<>());

                if(result.toString().endsWith(".0")) {
                    System.out.println(result.toString().substring(0, result.toString().length()-2));
                    return null;
                }
                System.out.println(result);
                return null;
            }
        }

        if(arguments.getFirst().toString().endsWith(".0")) {
            System.out.println(arguments.getFirst().toString().substring(0, arguments.getFirst().toString().length()-2));
            return null;
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


    public String toString(){
        return "<native fn>";
    }
}
