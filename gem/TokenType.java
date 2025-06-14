package com.interpreter.gem;

enum TokenType {
  // Single-character tokens.
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE, BACKSLASH,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR, PERCEN,
  LEFT_BRACKET, RIGHT_BRACKET, COLON,

  // One or two character tokens.
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  // Literals.
  IDENTIFIER, STRING, NUMBER,

  // Keywords.
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  RETURN, SUPER, THIS, TRUE, VAR, WHILE,
  IMPORT, STATIC, THROW, TRY, CATCH, FINALLY,
  BREAK, CONTINUE, OPERATOR,

  EOF
}
