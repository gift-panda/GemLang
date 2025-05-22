package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.GemCallable;
import com.interpreter.gem.Interpreter;

import java.util.List;
import java.util.Scanner;

public class Input implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String name() {
        return "input";
    }

    public String toString(){
        return "<native 'input'>";
    }
}
