package com.interpreter.gem;

import java.nio.file.Path;

public class Token{
	final TokenType type;
	final String lexeme;
	final Object literal;
	final int line;
	final Path sourceFile;

	Token(TokenType type, String lexeme, Object literal, int line, Path sourceFile){
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
        this.sourceFile = sourceFile;
    }
	public String toString(){
		 return type + " " + lexeme + " " + literal;
	 }
}
