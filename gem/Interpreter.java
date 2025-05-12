package com.interpreter.gem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

	public final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();
	public Path currentSourceFile = null;
	private final List<String> alreadyImported = new ArrayList<>();

	Interpreter(){
		try {
			List<GemCallable> natives = loadAllNatives("natives");

			for(GemCallable nativeFunction : natives){
				environment.define(mangleName(nativeFunction.name(), nativeFunction.arity()), nativeFunction);
			}

		}catch (IOException | ClassNotFoundException e){
			throw new RuntimeException(e);
		}

	}

	private GemClass stringClass;
	private GemClass numberClass;
	private GemClass booleanClass;
	private GemClass listClass;

	public void setWrappers(GemClass stringCls, GemClass numberCls, GemClass booleanCls, GemClass listCls) {
		this.stringClass = stringCls;
		this.numberClass = numberCls;
		this.booleanClass = booleanCls;
		this.listClass = listCls;
	}


	public static List<GemCallable> loadAllNatives(String directoryPath) throws IOException, ClassNotFoundException {
		File dir = new File(directoryPath);
		if (!dir.exists() || !dir.isDirectory()) {
			return new ArrayList<>();
		}

		List<GemCallable> natives = new ArrayList<>();
		for (File file : Objects.requireNonNull(dir.listFiles((d, name) -> name.endsWith(".nav")))) {
			try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
				Object obj = in.readObject();
				if (obj instanceof GemCallable nativeFunc) {
					natives.add(nativeFunc);
				}
			}
		}
		return natives;
	}


	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		Object leftRaw = unwrap(left);
		Object rightRaw = unwrap(right);

		switch (expr.operator.type) {
			case BANG_EQUAL:
				return wrapBoolean(!isEqual(leftRaw, rightRaw));
			case EQUAL_EQUAL:
				return wrapBoolean(isEqual(leftRaw, rightRaw));
		}

		switch (expr.operator.type) {
			case GREATER:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapBoolean((double) leftRaw > (double) rightRaw);
			case GREATER_EQUAL:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapBoolean((double) leftRaw >= (double) rightRaw);
			case LESS:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapBoolean((double) leftRaw < (double) rightRaw);
			case LESS_EQUAL:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapBoolean((double) leftRaw <= (double) rightRaw);
			case MINUS:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapNumber((double) leftRaw - (double) rightRaw);
			case PERCEN:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapNumber((double) leftRaw % (double) rightRaw);
			case BACKSLASH:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapNumber((double) ((int) (double) leftRaw / (int) (double) rightRaw));
			case SLASH:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapNumber((double) leftRaw / (double) rightRaw);
			case STAR:
				checkNumberOperands(expr.operator, leftRaw, rightRaw);
				return wrapNumber((double) leftRaw * (double) rightRaw);
			case PLUS:
				if (leftRaw instanceof Double && rightRaw instanceof Double) {
					return wrapNumber((double) leftRaw + (double) rightRaw);
				}

				if (leftRaw instanceof String || rightRaw instanceof String) {
					String leftText = leftRaw != null ? leftRaw.toString() : "nil";
					String rightText = rightRaw != null ? rightRaw.toString() : "nil";

					// Trim .0 from double-as-string if desired
					if (leftText.endsWith(".0")) {
						leftText = leftText.substring(0, leftText.length() - 2);
					}
					if (rightText.endsWith(".0")) {
						rightText = rightText.substring(0, rightText.length() - 2);
					}

					return wrapString(leftText + rightText);
				}

				throw new RuntimeError(expr.operator, "Operands must be of same type.");
		}

		return null;
	}

	private Object wrapNumber(double value) {
		return numberClass.call(this, List.of(value));
	}

	private Object wrapString(String value) {
		return stringClass.call(this, List.of(value, value.length()));
	}

	private Object wrapList(GemList list) {
		return listClass.call(this, List.of(list, list.size()));
	}

	private Object wrapBoolean(boolean value) {
		return booleanClass.call(this, List.of(value));
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		Object value = expr.value;

		if (value instanceof String) {
			return stringClass.call(this, List.of(value, ((String) value).length()));
		} else if (value instanceof Double) {
			return numberClass.call(this, List.of(value));
		} else if (value instanceof Boolean) {
			return booleanClass.call(this, List.of(value));
		}
		return value;
	}

	public static Object unwrap(Object obj) {
		if (obj instanceof GemInstance inst && (inst.klass.name().equals("String") || inst.klass.name().equals("Number") || inst.klass.name().equals("Boolean") || inst.klass.name().equals("List"))) {
            return inst.get("value"); // assumes wrapper stores it in `.value`
		}

		return obj;
	}

	public Object wrap(Object obj) {
		if(obj instanceof Double){
			return wrapNumber((Double) obj);
		}
		if(obj instanceof String){
			return wrapString((String) obj);
		}
		if(obj instanceof Boolean){
			return wrapBoolean((Boolean) obj);
		}
		if(obj instanceof List){
			return wrapList((GemList) obj);
		}

		return obj;
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
		return lookUpVariable(expr.name, expr);
	}

	private Object lookUpVariable(Token name, Expr expr){
		Integer distance = locals.get(expr);
		if(distance != null){
			return environment.getAt(distance, name.lexeme);
		}
		else{
			return globals.get(name);
		}
	}

	private Object evaluate(Expr expr){
	    	return (expr.accept(this));
	}

	private boolean isTruthy(Object object){
        return switch (object) {
			case null -> false;
            case GemInstance instance when instance.klass.name().equals("Boolean") -> (Boolean) instance.get("value");
            case GemInstance instance when instance.klass.name().equals("Number") ->
                    (Double) instance.get("value") != 0;
            default -> true;
        };

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
			Gem.runtimeError(error, error.token);
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

		Integer distance = locals.get(expr);
		if(distance != null){
			environment.assignAt(distance, expr.name, value);
		}
		else{
			globals.assign(expr.name, value);
		}

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

	public Object unwrapAll(Object obj) {
		obj = unwrap(obj); // Unwrap once

		if (obj instanceof GemList) {
			List<Object> result = new ArrayList<>();
			for (Object item : ((GemList) obj).getItems()) {
				Object unwrapped = unwrapAll(item);
				result.add(unwrapped);
			}
			return result;
		}

		return stringify(obj);
	}




	@Override
	public Object visitCallExpr(Expr.Call expr) {
		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}

		if (expr.callee instanceof Expr.Variable varExpr) {
			String mangled = mangleName(varExpr.name.lexeme, arguments.size());
			Object callee;
			try {
				callee = environment.get(new Token(TokenType.IDENTIFIER, mangled, null, varExpr.name.line, currentSourceFile));
			}catch(RuntimeError error){
				callee = environment.get(new Token(TokenType.IDENTIFIER, varExpr.name.lexeme, null, varExpr.name.line, currentSourceFile));			}

			if (!(callee instanceof GemCallable function)) {
				throw new RuntimeError(varExpr.name, "Can only call functions and classes.");
			}

			if(callee.toString().equals("<native fn>")){
				for(int i = 0; i < arguments.size(); i++){
					arguments.set(i, unwrapAll(arguments.get(i)));
				}
			}

			Object result = function.call(this, arguments);


			if(result instanceof Boolean)
				return wrapBoolean((Boolean) result);
			else if(result instanceof Double)
				return wrapNumber((Double) result);
			else if(result instanceof String)
				return wrapString((String) result);

			return result;
		}

		if (expr.callee instanceof Expr.Get getExpr) {
			Object object = evaluate(getExpr.object);
			if (object instanceof GemInstance instance) {
				String mangled = mangleName(getExpr.name.lexeme, arguments.size());
				GemFunction method = instance.klass.findMethod(mangled);

				if (method == null) {
					throw new RuntimeError(getExpr.name, "Undefined method '" +
							getExpr.name.lexeme + "' with " + arguments.size() + " arguments.");
				}

				Object result =  method.bind(instance).call(this, arguments);

				if(result instanceof Boolean)
					return wrapBoolean((Boolean) result);
				else if(result instanceof Double)
					return wrapNumber((Double) result);
				else if(result instanceof String)
					return wrapString((String) result);

				return result;
			}
		}


		Object callee = evaluate(expr.callee);

		if (!(callee instanceof GemCallable function)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");
		}

		Object result = function.call(this, arguments);

		if(result instanceof Boolean)
			return wrapBoolean((Boolean) result);
		else if(result instanceof Double)
			return wrapNumber((Double) result);
		else if(result instanceof String)
			return wrapString((String) result);

		return result;
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);if(object != null)
		if (object instanceof GemInstance instance) {
			Object value = instance.get(expr.name);

			if (value instanceof GemFunction function) {
				return function.bind(instance);
			}
			return value;
		}

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}


	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if(!(object instanceof GemInstance)){
			throw new RuntimeError(expr.name, "Cannot set property of non-instance '" + expr.name + "'.");
		}

		Object value = evaluate(expr.value);
		((GemInstance)object).set(expr.name, value);
		return value;
	}

	@Override
	public Object visitListLiteralExpr(Expr.ListLiteral expr) {
		GemList list = new GemList();
		for (Expr elementExpr : expr.elements) {
			list.add(evaluate(elementExpr));
		}
		return wrapList(list);
	}

	@Override
	public Object visitGetIndexExpr(Expr.GetIndex expr) {
		Object obj = unwrap(unwrap(evaluate(expr.object)));

		Object indexStart = unwrap(evaluate(expr.indexStart));
		Object indexEnd = unwrap(evaluate(expr.indexEnd));
		if (obj instanceof GemList && indexStart instanceof Double && indexEnd instanceof Double) {
			Object returnVal =  ((GemList) obj).get(expr.bracket, ((Double) indexStart).intValue(), ((Double) indexEnd).intValue());
			return wrap(returnVal);
		}

		if(obj instanceof String && indexStart instanceof Double && indexEnd instanceof Double){
			int start = ((Double)indexStart).intValue();
			int end = ((Double)indexEnd).intValue();
			if(start < 0 || start > ((String) obj).length() - 1 || start > end || end > ((String) obj).length() - 1){
				throw new RuntimeError(expr.bracket, "Index " + start + " to " + end + " out of bounds for length " + (((String) obj).length() - 1) );
			}
			return wrapString(((String)obj).substring(start, end + 1));
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
			return wrap(value);
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
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// Mangle the name by arity
		String mangledName = mangleName(stmt.name.lexeme, stmt.params.size());
		GemFunction function = new GemFunction(stmt, environment, false);
		environment.define(mangledName, function);

		String mangled = mangleName(stmt.name.lexeme, stmt.params.size());
		environment.define(mangled, function);

// If base name not defined, define dispatcher
		if (!environment.exists(stmt.name.lexeme)) {
			environment.define(stmt.name.lexeme,
					new FunctionDispatcher(stmt.name.lexeme, environment));
		}


		return null;
	}

	public static String mangleName(String name, int arity) {
		return name + "$" + arity;
	}



	@Override
	public Void visitReturnStmt(Stmt.Return stmt){
		Object value = null;
		if(stmt.value != null){
			value = evaluate(stmt.value);
		}

		throw new Return(value);
	}

	@Override
	public Void visitImportStmt(Stmt.Import stmt) {
		String module = stmt.moduleName;
		String resolvedPath = resolveImportPath(module, currentSourceFile);

		Path currFile = currentSourceFile;
		this.currentSourceFile = Paths.get(resolvedPath);

		if (alreadyImported.contains(resolvedPath)) return null;
		alreadyImported.add(resolvedPath);

		String source = readFile(resolvedPath, stmt.keyword, module);
		List<Stmt> statements = new Parser(new com.interpreter.gem.Scanner(source, currentSourceFile).scanTokens(), currentSourceFile).parse();

		Resolver resolver = new Resolver(this, currentSourceFile);
		resolver.resolve(statements);

		interpret(statements);

		this.currentSourceFile = currFile;
		return null;
	}

	private String readFile(String path, Token keyword, String module) {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(path));
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeError(keyword, "Invalid import: " + module);
		}

	}


	private String resolveImportPath(String moduleName, Path sourceFile) {
		Path baseDir;

		if (moduleName.startsWith("Gem.")) {
			baseDir = Paths.get("/home/meow/com/interpreter/internals"); // define GEM_PATH somewhere
			moduleName = moduleName.replace("Gem.", "");
		} else if (sourceFile != null) {
			baseDir = sourceFile.toAbsolutePath().getParent();
		} else {
			baseDir = Paths.get("").toAbsolutePath(); // PWD (REPL)
		}

		String relativePath = moduleName.replace('.', File.separatorChar) + ".gem";
		return baseDir.resolve(relativePath).toString();
	}


	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof GemClass)) {
				throw new RuntimeError(stmt.superclass.name,
						"Superclass must be a class.");
			}
		}

		environment.define(stmt.name.lexeme, null);

		if(stmt.superclass != null){
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		Map<String, GemFunction> methods = new HashMap<>();

		for (Stmt.Function method : stmt.methods) {
			String name = method.name.lexeme;
			int arity = method.params.size();
			boolean isInit = name.equals("init");

			String mangled = mangleName(name, arity);
			methods.put(mangled, new GemFunction(method, environment, isInit));
		}


		GemClass klass = new GemClass(stmt.name.lexeme, (GemClass)superclass,methods);

		if(superclass != null){
			environment = environment.enclosing;
		}
		environment.assign(stmt.name, klass);
		return null;
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		return environment.get(expr.keyword);
	}

	void resolve(Expr expr, int depth){
		locals.put(expr, depth);
	}

	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr);
		GemClass superclass = (GemClass) environment.getAt(distance, "super");

		GemInstance object = (GemInstance) environment.getAt(distance - 1, "this");
		return new DeferredSuperCallable(superclass, object, expr.method.lexeme, expr.keyword);
	}
}







