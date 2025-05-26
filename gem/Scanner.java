package com.interpreter.gem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.interpreter.gem.TokenType.*;

class Scanner {
	private final String source;
  	private final List<Token> tokens = new ArrayList<>();
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private static final Map<String, TokenType> keywords;
	private final Path currentSourceFile;

	static{
    		keywords = new HashMap<>();
    		keywords.put("and",    AND);
    		keywords.put("class",  CLASS);
    		keywords.put("else",   ELSE);
    		keywords.put("false",  FALSE);
    		keywords.put("for",    FOR);
    		keywords.put("func",   FUN);
    		keywords.put("if",     IF);
    		keywords.put("nil",    NIL);
    		keywords.put("or",     OR);
    		keywords.put("return", RETURN);
    		keywords.put("super",  SUPER);
    		keywords.put("this",   THIS);
    		keywords.put("true",   TRUE);
    		keywords.put("var",    VAR);
    		keywords.put("while",  WHILE);
			keywords.put("import", IMPORT);
			keywords.put("static", STATIC);
			keywords.put("throw",  THROW);
			keywords.put("try",    TRY);
			keywords.put("catch",  CATCH);
			keywords.put("finally",FINALLY);
  	}

  	Scanner(String source, Path currentSourceFile) {
    		this.source = source;
        this.currentSourceFile = currentSourceFile;
    }

	List<Token> scanTokens(){
		while(!isAtEnd()){
			start = current;
			scanToken();
		}

		tokens.add(new Token(EOF, "", null, line, currentSourceFile));
		return tokens;
	}

	private boolean isAtEnd(){
		return current >= source.length();
	}

	private void scanToken() {
    		char c = advance();
    		switch (c) {
      			case '(': addToken(LEFT_PAREN); break;
      			case ')': addToken(RIGHT_PAREN); break;
     	 		case '{': addToken(LEFT_BRACE); break;
     	 		case '}': addToken(RIGHT_BRACE); break;
				case '[': addToken(LEFT_BRACKET); break;
				case ']': addToken(RIGHT_BRACKET); break;
				case ',': addToken(COMMA); break;
     	 		case '.': addToken(DOT); break;
     	 		case '-': addToken(MINUS); break;
     			case '+': addToken(PLUS); break;
     			case ';': addToken(SEMICOLON); break;
				case ':': addToken(COLON); break;
     	 		case '*': addToken(STAR); break;
				case '%': addToken(PERCEN); break;
				case '\\': addToken(BACKSLASH); break;
			case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
      			case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
      			case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
      			case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
			case '/':
				  if(match('/')){ // For Comments
					  while(peek() != '\n' && !isAtEnd()) advance();
				  }
				  else
					  addToken(SLASH);
				  break;
			case ' ':
      			case '\r':
      			case '\t': // Ignore whitespace.
        			break;
      			case '\n':line++; break;
			case '"': string(); break;
			default:
				if(isDigit(c))
					number();
				else if(isAlpha(c))
					identifier();
				else
				  	Gem.error(line, "Unexpected character.", currentSourceFile);
				break;
    		}
  	}

	private char advance(){
		return source.charAt(current++);
	}
	private void addToken(TokenType type){
		addToken(type,null);
	}
	private void addToken(TokenType type, Object literal){
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line, currentSourceFile));
	}
	private boolean match(char expected){
		if(isAtEnd()) return false;
		if(source.charAt(current) != expected) return false;

		current++;
		return true;
	}
	private char peek(){
		if(isAtEnd()) return '\0';
		return source.charAt(current);
	}
	private void string(){
		while(peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') line++;
			advance();
		}

		if(isAtEnd()){
			Gem.error(line, "Unterminated String.", currentSourceFile);
			return;
		}

		advance();
		
		//Unescape escape sequences done here!
		String value = source.substring(start+1, current-1);

		addToken(STRING, unescape(value));
	}
	private static String unescape(String input) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '\\' && i + 1 < input.length()) {
				char next = input.charAt(i + 1);
				switch (next) {
					case 'n': result.append('\n'); i++; break;
					case 't': result.append('\t'); i++; break;
					case 'r': result.append('\r'); i++; break;
					case 'b': result.append('\b'); i++; break;
					case 'f': result.append('\f'); i++; break;
					case '\'': result.append('\''); i++; break;
					case '\"': result.append('\"'); i++; break;
					case '\\': result.append('\\'); i++; break;
					case 'u':
						// Handle Unicode escape
						if (i + 5 < input.length()) {
							String hex = input.substring(i + 2, i + 6);
							try {
								int codePoint = Integer.parseInt(hex, 16);
								result.append((char) codePoint);
								i += 5;
							} catch (NumberFormatException e) {
								result.append("\\u"); // Leave as is if invalid
								i++;
							}
						} else {
							result.append("\\u"); // Incomplete
							i++;
						}
						break;
					default:
						result.append('\\').append(next);
						i++;
						break;
				}
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	private boolean isDigit(char c){
		return c >= '0' && c <= '9';
	}
	private void number(){
		while(isDigit(peek())) advance();

		if(peek() == '.' && isDigit(peekNext())){
			advance();

			while(isDigit(peek())) advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	private char peekNext(){
		if(current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	private void identifier(){
		while(isAlphaNumeric(peek())) advance();
		
		String text = source.substring(start, current);

		if(text.substring(1).contains("#")){
			Gem.error(line, "'#' should appear only at the beginning of identifier.", currentSourceFile);
		}

		TokenType type = keywords.get(text);
		if(type == null) type = IDENTIFIER;



		addToken(type);
	}
	private boolean isAlpha(char c){
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '#';
	}
	private boolean isAlphaNumeric(char c){
		return isAlpha(c) || isDigit(c);
	}

}
