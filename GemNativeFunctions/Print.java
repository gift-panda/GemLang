package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;
import com.interpreter.gem.Environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Print implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        if(arguments.getFirst() == null){
            System.out.print("nil");
            return null;
        }

        cleanList(arguments);

        if(arguments.getFirst() instanceof GemInstance instance){
            GemFunction function = instance.klass.findMethod(Interpreter.mangleName("toString",0));

            if(function != null) {
                function = function.bind(instance);
                Object result = Interpreter.unwrapAll(function.call(interpreter, new ArrayList<>()));

                if(result.toString().endsWith(".0")) {
                    System.out.println(result.toString().substring(0, result.toString().length()-2));
                    return null;
                }
                System.out.print(result);
                return null;
            }
        }
        System.out.print(arguments.getFirst());
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void cleanList(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object elem = list.get(i);
            if (elem instanceof List<?>) {
                cleanList((List<Object>) elem); // Recursive step
            } else if (elem instanceof Double element) {
                if (element % 1 == 0) {
                    list.set(i, (element.toString().replace(".0", "")));
                }
            }
        }
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "print";
    }

    public String toString(){
        return "<native 'print'>";
    }
}
