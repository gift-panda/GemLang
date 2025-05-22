package com.interpreter.gem;

public class GemThrow extends RuntimeException {
    public final GemInstance errorObject;
    public final int line;
    public final String msg;
    public final String file;

    public GemThrow(Token token, GemInstance errorObject) {
        super();
        this.errorObject = errorObject;
        this.line = token.line;
        this.file = token.sourceFile.getFileName().toString();
        this.msg = (String)Interpreter.unwrap(errorObject.get("msg"));
    }
}
