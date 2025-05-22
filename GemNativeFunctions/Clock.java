package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.GemCallable;
import com.interpreter.gem.GemNative;
import com.interpreter.gem.Interpreter;

import java.io.Serializable;
import java.util.List;

public class Clock implements GemCallable{
    @Override
    public int arity(){return 0;}

    @Override
    public String name() {
        return "clock";
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments){
        return (double)System.currentTimeMillis()/1000.0;
    }

    @Override
    public String toString(){return "<native 'clock'>";}
}
