package com.interpreter.gem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

	final Environment globals = new Environment();
	private Environment environment = globals;

	Interpreter(){
		/*globals.define("clock", new GemNative.Native(){
			@Override
			public int arity(){return 0;}

			@Override
			public String name() {
				return "clock";
			}

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments){
				return (double)System.currentTimeMillis()/1000.0;
			}

			@Override
			public String toString(){return "<native fn>";}
		});

		globals.define("asc", new GemNative.Native(){

			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				if(arguments.getFirst() instanceof String){
					return (double)(((String)arguments.getFirst()).charAt(0));
				}

				return arguments.getFirst();
			}

			@Override
			public int arity() {
				return 1;
			}

			@Override
			public String name() {
				return "asc";
			}

			@Override
			public String toString(){return "<native fn>";}
		});*/

		try {
			List<GemNative.Native> natives = loadAllNatives("natives");

			for(GemNative.Native n : natives){
				globals.define(n.name(), n);
			}

		}catch (IOException | ClassNotFoundException e){
			throw new RuntimeException(e);
		}

	}

	public static List<GemNative.Native> loadAllNatives(String directoryPath) throws IOException, ClassNotFoundException {
		File dir = new File(directoryPath);
		if (!dir.exists() || !dir.isDirectory()) {
			throw new IllegalArgumentException("Invalid natives directory: " + directoryPath);
		}

		List<GemNative.Native> natives = new ArrayList<>();
		for (File file : Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".nav")))) {
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
				Object obj = in.readObject();
				if (obj instanceof GemNative.Native nativeFunc) {
					natives.add(nativeFunc);
					//System.out.println("Loaded native: " + nativeFunc.name());
				}
			}
		}
		return natives;
	}


	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch(expr.operator.type) {
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
		}


		switch(expr.operator.type){
			case GREATER:
				checkNumberOperands(expr.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left >= (double)right;
			case LESS:
				checkNumberOperands(expr.operator, left , right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, left, right);
				return (double)left <= (double)right;
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left - (double)right;
			case PERCEN:
				checkNumberOperands(expr.operator, left, right);
				return (double)left % (double)right;
			case BACKSLASH:
				checkNumberOperands(expr.operator, left, right);
				return (double)((int)(double)left / (int)(double)right);
			case SLASH:
				checkNumberOperands(expr.operator, left, right);
				return (double)left / (double)right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double)left * (double)right;
			case PLUS:
				if(left instanceof Double && right instanceof Double)
					return (double)left + (double)right;

				//This could use a || instead of a && later?
				if(left instanceof String || right instanceof String) {
					String leftText = left.toString(), rightText = right.toString();
					if(left.toString().endsWith(".0"))
						leftText = leftText.substring(0, leftText.length() - 2);

					if(right.toString().endsWith(".0"))
						rightText = rightText.substring(0, rightText.length() - 2);
					return leftText + rightText;
				}

				throw new RuntimeError(expr.operator, "Operands must be of same type.");
			
		}
		return null;
	}

    	@Override
    	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
    	}
        @Override
	public Object visitLiteralExpr(Expr.Literal expr){
		return expr.value;
	}
	
	@Override
    	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch(expr.operator.type){
			case BANG: return !isTruthy(right);
			case MINUS:
				   checkNumberOperand(expr.operator, right);
				   return -(double)right;
		}

		return null;
    	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	private Object evaluate(Expr expr){
	    	return expr.accept(this);
	}

	private boolean isTruthy(Object object){
		if(object == null) return false;
		if(object instanceof Boolean) return (boolean)object;

		//Might need to be changed
		if(object instanceof Double && (double)object == 0) return false;
		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false;

		return a.equals(b);
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) return;
    
		throw new RuntimeError(operator, "Operands must be numbers.");
	}
	void interpret(List<Stmt> statements){
		try{
			for(Stmt statement : statements){
				execute(statement);
			}
		}catch(RuntimeError error) {
			Gem.runtimeError(error);
		}
	}

	private void execute(Stmt stmt){
		stmt.accept(this);
	}

	private String stringify(Object object){
		if(object == null) return "nil";

		if(object instanceof Double){
			String text = object.toString();
			if(text.endsWith(".0")){
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}

		return object.toString();
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt){
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if(stmt.initializer != null){
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);
		return null;

	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr){
		Object value = evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt){
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt){
		if(isTruthy(evaluate(stmt.condition))){
			execute(stmt.thenBranch);
		}
		else if(stmt.elseBranch != null){
			execute(stmt.elseBranch);
		}
		return null;
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr){
		Object left = evaluate(expr.left);

		if(expr.operator.type == TokenType.OR){
			if(isTruthy(left)) return left;
		}
		else{
			if(!isTruthy(left)) return left;
		}

		return evaluate(expr.right);
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt){
		while(isTruthy(evaluate(stmt.condition)))
			execute(stmt.body);
		return null;
	}

	@Override
	public Object visitCallExpr(Expr.Call expr){
		Object callee = evaluate(expr.callee);

		List<Object> arguments = new ArrayList<>();
		for(Expr argument : expr.arguments){
			arguments.add(evaluate(argument));
		}

		if(!(callee instanceof GemCallable || callee instanceof GemNative.Native)){
			throw new RuntimeError(expr.paren, "Can not be called.");
		}

		if(callee instanceof GemNative.Native function){
			Object result = null;
			try {
				result = function.call(Interpreter.this, arguments);
			}
			catch(Exception error) {
				if (arguments.size() != function.arity()) {
					throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, but received " + arguments.size() + ".");
				}
			}

			return result;

		}
		GemCallable function = (GemCallable)callee;

		if(arguments.size() != function.arity()){
			throw new RuntimeError(expr.paren, "Expected "+function.arity()+" arguments, but received "+arguments.size()+".");
		}

		return function.call(this, arguments);
	}

	@Override
	public Object visitListLiteralExpr(Expr.ListLiteral expr) {
		GemList list = new GemList();
		for (Expr elementExpr : expr.elements) {
			list.add(evaluate(elementExpr));
		}
		return list;
	}

	@Override
	public Object visitGetIndexExpr(Expr.GetIndex expr) {
		Object obj = evaluate(expr.object);
		Object indexStart = evaluate(expr.indexStart);
		Object indexEnd = evaluate(expr.indexEnd);
		if (obj instanceof GemList && indexStart instanceof Double && indexEnd instanceof Double) {
			return ((GemList) obj).get(expr.bracket, ((Double) indexStart).intValue(), ((Double) indexEnd).intValue());
		}

		if(obj instanceof String && indexStart instanceof Double && indexEnd instanceof Double){
			int start = ((Double)indexStart).intValue();
			int end = ((Double)indexEnd).intValue();
			if(start < 0 || start > ((String) obj).length() - 1 || start > end || end > ((String) obj).length() - 1){
				throw new RuntimeError(expr.bracket, "Index " + start + " to " + end + " out of bounds for length " + (((String) obj).length() - 1) );
			}
			return ((String)obj).substring(start, end + 1);
		}

		if(indexStart instanceof Double && indexEnd instanceof Double)
			throw new RuntimeError(expr.bracket, "Only lists or strings can be indexed.");

		throw new RuntimeError(expr.bracket, "Index(s) must be a number.");
	}

	@Override
	public Object visitSetIndexExpr(Expr.SetIndex expr) {
		Object obj = evaluate(expr.object);
		Object index = evaluate(expr.index);
		Object value = evaluate(expr.value);
		if (obj instanceof GemList && index instanceof Double) {
			((GemList) obj).set(((Double) index).intValue(), value);
			return value;
		}
		throw new RuntimeError(expr.bracket, "Only lists can be assigned by index.");
	}


	void executeBlock(List<Stmt> statements, Environment environment){
		Environment previous = this.environment;
		try{
			this.environment = environment;

			for(Stmt statement: statements) {
				execute(statement);
			}
		}
		finally{
			this.environment = previous;
		}
	}
	public Void visitFunctionStmt(Stmt.Function stmt){
		GemFunction function = new GemFunction(stmt, environment);
		environment.define(stmt.name.lexeme, function);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt){
		Object value = null;
		if(stmt.value != null){
			value = evaluate(stmt.value);
		}

		throw new Return(value);
	}

}







