package com.interpreter.GemNativeFunctions;

import com.interpreter.gem.GemCallable;
import com.interpreter.gem.Interpreter;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;
import java.util.Scanner;

public class Input implements GemCallable{
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Scanner sc = new Scanner(System.in);
        String line =  sc.nextLine();

        return line;
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
