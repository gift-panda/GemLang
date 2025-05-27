package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.*;

import java.util.List;

public class Type implements GemCallable {
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Object value = arguments.getFirst();
        if(value instanceof String){
            return "<class 'String'>";
        }
        if(value instanceof Double){
            return "<inst 'Number'>";
        }
        if(value instanceof Boolean){
            return "<inst 'Boolean'>";
        }
        if(value instanceof GemList){
            return "<inst 'GemList'>";
        }
        if(value instanceof GemInstance instance){
            return "<inst '" + instance.klass.name() + "'>";
        }
        if(value instanceof GemClass clazz){
            return "<class '" + clazz.name() + "'>";
        }
        if(value instanceof GemCallable function){
            return function.toString();
        }
        if(value == null){
            return "<nil>";
        }

        return null;
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "type";
    }
}
