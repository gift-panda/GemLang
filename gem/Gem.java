package com.interpreter.gem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.interpreter.gem.Scanner;

public class Gem {
	private static Interpreter interpreter = new Interpreter();
	static boolean hadError = false;
	static boolean hadRuntimeError = false;
	private final static List<String> autoImports = List.of("gem.String", "gem.Number", "gem.Boolean", "gem.List");
	public static Path currentSourceFile;
	public static boolean isTakingInput = false;

	public static long pid;

    public static void main(String[] args) throws IOException {
		pid = ProcessHandle.current().pid();
		System.out.println("JAR_PID:" + pid);

		if (args.length > 1) {
			System.err.println("Usage: gem [script]");
			System.exit(64);
		} else if (args.length == 1) {
			currentSourceFile = Paths.get(args[0]);
			runFile(args[0]);
		} else {
			runPrompt();
		}

		System.exit(0);
	}

	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

		if (hadError) System.exit(65);
		if(hadRuntimeError) System.exit(70);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		for (;;) {
			System.out.print("> ");
			String line = reader.readLine();
			if (line == null) break;
			run(line);
			hadError = false;
		}
	}

	private static void run(String source) {
		interpreter.currentSourceFile = currentSourceFile;

		//report(1, "hi", "Just Internal", interpreter.currentSourceFile);

		StringBuilder imports = new StringBuilder();
		for(String imp: autoImports){
			imports.append("import ").append(imp).append(";\n");
		}

		Scanner importScanner = new Scanner(imports.toString(), currentSourceFile);
		List<Token> importTokens = importScanner.scanTokens();

		Parser importParser = new Parser(importTokens, currentSourceFile);
		List<Stmt> importStmts = importParser.parse();

		interpreter.interpret(importStmts);
		Interpreter.setWrappers(
				(GemClass) interpreter.globals.get("String"),
				(GemClass) interpreter.globals.get("Number"),
				(GemClass) interpreter.globals.get("Boolean"),
				(GemClass) interpreter.globals.get("List"),
				(GemClass) interpreter.globals.get("RuntimeError")
		);


		Scanner sc = new Scanner(source, currentSourceFile);
		List<Token> tokens = sc.scanTokens();

		Parser parser = new Parser(tokens, currentSourceFile);
		List<Stmt> statements = parser.parse();

		if (hadError) {
			return;
		}

		Resolver resolver = new Resolver(interpreter, currentSourceFile);
		resolver.resolve(statements);

		if (hadError){
			return;
		}

		//NOW COMES THE PROTAGONIST
		interpreter.interpret(statements);
	}
	public static void error(int line, String message, Path file) {
		report(line, "", message, file);
	}

	private static void report(int line, String where, String message, Path file) {
		System.err.println("[Line " + line + "] Syntax Error" + where + ": " + message + "\nIn file " + file.getFileName());
		System.exit(2);
	}

	public static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message, token.sourceFile);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message, token.sourceFile);
		}
	}
}