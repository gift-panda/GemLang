package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.GemCallable;
import com.interpreter.gem.Interpreter;

import java.util.List;
import java.util.Scanner;

public class InputWithMsg implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        System.out.print(arguments.getFirst());
        Scanner sc = new Scanner(System.in);
        return sc.nextLine();
    }

    @Override
    public int arity() {
        return 1;
    }

    @Override
    public String name() {
        return "input";
    }
}
