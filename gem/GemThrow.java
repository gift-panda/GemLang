package com.interpreter.gem;

public class GemThrow extends RuntimeException {
    public final GemInstance errorObject;
    public final int line;
    public final String msg;
    public final String file;
    public final String name;

    public GemThrow(Token token, GemInstance errorObject, String name) {
        super();
        this.errorObject = errorObject;
        this.line = token.line;
        this.file = token.sourceFile.getFileName().toString();
        this.msg = Interpreter.unwrap(errorObject.get("message")) +  "\n" + Interpreter.unwrap(errorObject.get("stackTrace"));
        //System.err.println(msg);
        this.name = name;
    }
}
