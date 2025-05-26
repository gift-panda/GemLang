package com.interpreter.gem;

import java.util.List;

abstract class Stmt {
	interface Visitor<R> {
		R visitBlockStmt(Block stmt);
		R visitThrowStmt(Throw stmt);
		R visitTryStmt(Try stmt);
		R visitClassStmt(Class stmt);
		R visitExpressionStmt(Expression stmt);
		R visitIfStmt(If stmt);
		R visitWhileStmt(While stmt);
		R visitVarStmt(Var stmt);
		R visitReturnStmt(Return stmt);
		R visitImportStmt(Import stmt);
		R visitFunctionStmt(Function stmt);
	}
  static class Block extends Stmt {
    Block(List<Stmt> statements) {
      this.statements = statements;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitBlockStmt(this);
	}

    final List<Stmt> statements;
  }
  static class Throw extends Stmt {
    Throw(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitThrowStmt(this);
	}

    final Token keyword;
    final Expr value;
  }
  static class Try extends Stmt {
    Try(Stmt tryBlock, Token catchToken, Expr.Variable errorVar, Stmt catchBlock, Stmt finallyBlock) {
      this.tryBlock = tryBlock;
      this.catchToken = catchToken;
      this.errorVar = errorVar;
      this.catchBlock = catchBlock;
      this.finallyBlock = finallyBlock;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitTryStmt(this);
	}

    final Stmt tryBlock;
    final Token catchToken;
    final Expr.Variable errorVar;
    final Stmt catchBlock;
    final Stmt finallyBlock;
  }
  static class Class extends Stmt {
    Class(Token name, Expr.Variable superclass, List<Stmt.Function> methods, List<Stmt.Function> staticMethods, List<Stmt.Var> staticFields) {
      this.name = name;
      this.superclass = superclass;
      this.methods = methods;
      this.staticMethods = staticMethods;
      this.staticFields = staticFields;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitClassStmt(this);
	}

    final Token name;
    final Expr.Variable superclass;
    final List<Stmt.Function> methods;
    final List<Stmt.Function> staticMethods;
    final List<Stmt.Var> staticFields;
  }
  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitExpressionStmt(this);
	}

    final Expr expression;
  }
  static class If extends Stmt {
    If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
      this.condition = condition;
      this.thenBranch = thenBranch;
      this.elseBranch = elseBranch;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitIfStmt(this);
	}

    final Expr condition;
    final Stmt thenBranch;
    final Stmt elseBranch;
  }
  static class While extends Stmt {
    While(Expr condition, Stmt body) {
      this.condition = condition;
      this.body = body;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitWhileStmt(this);
	}

    final Expr condition;
    final Stmt body;
  }
  static class Var extends Stmt {
    Var(Token name, Expr initializer) {
      this.name = name;
      this.initializer = initializer;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitVarStmt(this);
	}

    final Token name;
    final Expr initializer;
  }
  static class Return extends Stmt {
    Return(Token keyword, Expr value) {
      this.keyword = keyword;
      this.value = value;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitReturnStmt(this);
	}

    final Token keyword;
    final Expr value;
  }
  static class Import extends Stmt {
    Import(String moduleName, Token keyword) {
      this.moduleName = moduleName;
      this.keyword = keyword;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitImportStmt(this);
	}

    final String moduleName;
    final Token keyword;
  }
  static class Function extends Stmt {
    Function(Token name, List<Token> params, List<Stmt> body, String parent) {
      this.name = name;
      this.params = params;
      this.body = body;
      this.parent = parent;
    }

	@Override
	<R> R accept(Visitor<R> visitor){
		return visitor.visitFunctionStmt(this);
	}

    final Token name;
    final List<Token> params;
    final List<Stmt> body;
    final String parent;
  }

	abstract <R> R accept(Visitor<R> visitor);
}
