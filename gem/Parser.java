package com.interpreter.gem;

import java.nio.file.Path;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static com.interpreter.gem.TokenType.*;

class Parser{
	private final List<Token> tokens;
	private int current = 0;
	private String currentClass = null;

	private static class ParseError extends RuntimeException {}
	Parser(List<Token> tokens, Path currentSourceFile){
		this.tokens = tokens;

    }

	private Expr expression(){
		return assignment();
	}

	private Expr assignment() {
		Expr expr = or();

		if (match(EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if (expr instanceof Expr.GetIndex getIndex) {
                return new Expr.SetIndex(getIndex.object, getIndex.indexStart, value, getIndex.bracket);
			}


			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			}

			if(expr instanceof Expr.Get){
				Expr.Get get = (Expr.Get)expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expr;
	}
	// Tools to help in grammar formation
	private boolean match(TokenType... types){
		for(TokenType type: types){
			if(check(type)){
				advance();
				return true;
			}
		}
		return false;
	}

	private Expr or(){
		Expr expr = and();

		while(match(OR)){
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr and(){
		Expr expr = equality();

		while(match(AND)){
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) return false;
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) current++;
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}


	//Context free grammer definations
	private Expr equality(){
		Expr expr = comparison();

		while(match(BANG_EQUAL, EQUAL_EQUAL)){
			Token operator = previous();
			Expr right = comparison();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr comparison() {
		Expr expr = term();

		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = term();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr term() {
		Expr expr = factor();

		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = factor();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr factor() {
		Expr expr = unary();

		while (match(SLASH, STAR, PERCEN, BACKSLASH)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	private Expr call(){
		Expr expr = primary();

		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(TokenType.LEFT_BRACKET)) {
				Expr indexStart = expression();
				Expr indexEnd = indexStart;

				if(match(COLON)){
					indexEnd = expression();
				}

				Token bracket = consume(TokenType.RIGHT_BRACKET, "Expect ']' after index.");
				expr = new Expr.GetIndex(expr, indexStart, indexEnd, bracket);
			} else if (match(TokenType.DOT)){
				Token name = consume(IDENTIFIER,
						"Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			}
			else
			{
				break;
			}
		}


		return expr;
	}

	private Expr finishCall(Expr callee){
		List<Expr> arguments = new ArrayList<>();
		if(!check(RIGHT_PAREN)){
			do{
				arguments.add(expression());
			}while(match(COMMA));
		}

		Token paren = consume(RIGHT_PAREN, "Expected ')' after function call.");

		return new Expr.Call(callee, paren, arguments);
	}

	private Expr primary() {
		if (match(FALSE)) return new Expr.Literal(false);
		if (match(TRUE)) return new Expr.Literal(true);
		if (match(NIL)) return new Expr.Literal(null);

		if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if(match(THIS)) return new Expr.This(previous());

		if(match(IDENTIFIER)){
			return new Expr.Variable(previous(), currentClass);
		}

		if(match(SUPER)){
			Token keyword = previous();
			consume(DOT, "Expect '.' after 'super'.");
			Token method = consume(IDENTIFIER, "Expected superclass methods.");
			return new Expr.Super(keyword, method);
		}

		if (match(TokenType.LEFT_BRACKET)) {
			List<Expr> elements = new ArrayList<>();
			if (!check(TokenType.RIGHT_BRACKET)) {
				do {
					elements.add(expression());
				} while (match(TokenType.COMMA));
			}
			consume(TokenType.RIGHT_BRACKET, "Expect ']' after list.");
			return new Expr.ListLiteral(elements);
		}


		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		throw error(peek(), "Expect expression.");
	}


	// Other tools
	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();

		throw error(peek(), message);
	}

	private ParseError error(Token token, String message) {
		Gem.error(token, message);
		return new ParseError();
	}

	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return;

			switch (peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case RETURN:
					return;
			}

			advance();
		}
	}

	//Parse Start Point
	List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		while(!isAtEnd()){
			statements.add(declaration());
		}

		return statements;
	}

	private Stmt statement(){
		if(match(CLASS)) return classDeclaration();
		if(match(IF)) return ifStatement();
		if(match(WHILE)) return whileStatement();
		if(match(FOR)) return forStatement();
		if(match(LEFT_BRACE)) return new Stmt.Block(block());
		if(match(RETURN)) return returnStatement();
		if(match(IMPORT)) return importStatement();
		if(match(THROW)) return throwStatement();
		if(match(TRY)) {return tryStatement();}
		if(match(BREAK)) {
			consume(SEMICOLON, "Expect ';' after break.");
			return new Stmt.Break(previous());
		}
		if(match(CONTINUE)){
			consume(SEMICOLON, "Expect ';' after continue.");
			return new Stmt.Continue(previous());
		}

		return expressionStatement();
	}

	private Stmt  tryStatement(){
		Stmt tryBlock = statement();

		Token catchToken = null;
		Expr.Variable errorVar = null;
		Stmt catchBlock = null;

		if (match(CATCH)) {
			consume(TokenType.LEFT_PAREN, "Expect '(' after 'catch'.");
			Token errorName = consume(TokenType.IDENTIFIER, "Expect Error Class.");
			errorVar = new Expr.Variable(errorName, currentClass);
			consume(TokenType.RIGHT_PAREN, "Expect ')' after error variable.");
			catchToken = errorName;
			catchBlock = statement();
		}

		Stmt finallyBlock = null;
		if (match(FINALLY)) {
			finallyBlock = statement();
		}
		if (catchBlock == null && finallyBlock == null) {
			Gem.error(previous(), "Expect 'catch' or 'finally' after 'try'.");
		}

		Stmt.Try res = new Stmt.Try(tryBlock, catchToken, errorVar, catchBlock, finallyBlock);
		return res;
	}

	private Stmt throwStatement() {
		Token name = previous();
		Expr expr = expression();

		consume(SEMICOLON, "Expect ';' after throw.");

		return new Stmt.Throw(name, expr);
	}

	private Stmt importStatement() {
		Token name = consume(IDENTIFIER, "Expect module name after 'import'.");
		StringBuilder moduleName = new StringBuilder(name.lexeme);

		while (match(DOT)) {
			Token next = consume(IDENTIFIER, "Expect identifier after '.'.");
			moduleName.append(".").append(next.lexeme);
		}

		consume(SEMICOLON, "Expected ';' after import.");
		return new Stmt.Import(moduleName.toString(), name);
	}


	private Stmt classDeclaration(){
		Token name = consume(IDENTIFIER, "Expected class name.");

		Expr.Variable superClass = null; //Add a universal super class here
		if(match(COLON)){
			consume(IDENTIFIER, "Expect ':' after class name.");
			superClass = new Expr.Variable(previous(), currentClass);
		}

		String prevClass = currentClass;
		currentClass = name.lexeme;

		consume(LEFT_BRACE, "Expect class body.");

		List<Stmt.Function> methods = new ArrayList<>();
		List<Stmt.Function> staticMethods = new ArrayList<>();
		List<Stmt.Var> staticFields = new ArrayList<>();
		while(!check(RIGHT_BRACE) && !isAtEnd()){
			if(match(VAR)){
				staticFields.add(varDeclaration());
			}
			else if(match(STATIC)){
				staticMethods.add(function("method"));
			}
			else
				methods.add(function("method"));
		}

		consume(RIGHT_BRACE, "Expect '}' after class body.");

		currentClass = prevClass;

		return new Stmt.Class(name, superClass, methods, staticMethods, staticFields);
	}

	private Stmt returnStatement(){
		Token token = previous();
		Expr value = null;
		if(!check(SEMICOLON)){
			value = expression();
		}

		consume(SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Return(token, value);
	}

	private Stmt forStatement(){
		consume(LEFT_PAREN, "Expected '(' after 'for'.");

		Stmt initializer;
		if(match(SEMICOLON)){
			initializer = null;
		}
		else if(match(VAR)){
			initializer = varDeclaration();
		}
		else{
			initializer = expressionStatement();
		}

		Expr condition = null;
		if(!check(SEMICOLON)){
			condition = expression();
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");

		Expr increment = null;
		if(!check(RIGHT_PAREN)){
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after 'for' clause.");

		Stmt body = statement();

		if(increment != null){
			body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
		}

		if(condition == null) condition = new Expr.Literal(true);

		if(increment != null)
			body = new Stmt.While(condition, body, new Stmt.Expression(increment));
		else
			body = new Stmt.While(condition, body, null);

		if(initializer != null){
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	private Stmt whileStatement(){
		consume(LEFT_PAREN, "Expected '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expected ')' after condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body, null);
	}
	
	private Stmt ifStatement(){
		consume(LEFT_PAREN, "Expected '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expected ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if(match(ELSE)){
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private Stmt expressionStatement(){
		Expr expr = expression();
		consume(SEMICOLON, "Expected ';' after value.");
		return new Stmt.Expression(expr);
	}

	private List<Stmt> block(){
		List<Stmt> statements = new ArrayList<>();

		while(!check(RIGHT_BRACE) && !isAtEnd()){
			statements.add(declaration());
		}

		consume(RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	private Stmt declaration(){
		try{
			if(match(FUN)) return function("function");
			if(match(VAR)) return varDeclaration();

			return statement();
		}catch(ParseError error){
			synchronize();
			return null;
		}
	}

	private Stmt.Var varDeclaration(){
		Token name = consume(IDENTIFIER, "Expected variable name.");

		Expr initializer = null;
		if(match(EQUAL)){
			initializer = expression();
		}

		consume(SEMICOLON, "Excepted ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt.Function function(String kind){
		Token name = consume(IDENTIFIER, "Expected " + kind + " name.");

		consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if(!check(RIGHT_PAREN)){
			do{
				parameters.add(consume(IDENTIFIER, "Expect parameter name."));
			}while(match(COMMA));
		}

		consume(RIGHT_PAREN, "Expected ')' after parameters.");

		consume(LEFT_BRACE, "Expected '{' after " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body, currentClass);
	}


}
