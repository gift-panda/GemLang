package com.interpreter.gem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

	public final Environment globals = new Environment();
	private Environment environment = globals;
	static public final Environment scopes = new Environment();
	private final Map<Expr, Integer> locals = new HashMap<>();
	public Path currentSourceFile = null;
	private final List<String> alreadyImported = new ArrayList<>();
	private String currentClass = "~";
	private static Stack<String> stackTrace = new Stack<>();

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

	public static GemClass stringClass;
	public static GemClass numberClass;
	public static GemClass booleanClass;
	public static GemClass listClass;
	public static GemClass errorClass;

	public static void setWrappers(GemClass stringCls, GemClass numberCls, GemClass booleanCls, GemClass listCls, GemClass errorCls) {
		stringClass = stringCls;
		numberClass = numberCls;
		booleanClass = booleanCls;
		listClass = listCls;
		errorClass = errorCls;
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


	boolean nearlyEqualRel(double a, double b, double relTol, double absTol) {
		return Math.abs(a - b) <= Math.max(relTol * Math.max(Math.abs(a), Math.abs(b)), absTol);
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

				checkNumberOperands(expr.operator, leftRaw, rightRaw);

				runtimeError(expr.operator, "Incompatible types for operator " + expr.operator.lexeme);
		}

		return null;
	}

	public static void runtimeError(Token token, String msg) {
		GemInstance errorInstance = new GemInstance(errorClass);
        StringBuilder msgBuilder = new StringBuilder(msg);
        for(int i = stackTrace.size() - 1; i >= 0; i--){
			msgBuilder.append("\n").append(stackTrace.get(i));
		}
        msg = msgBuilder.toString();
        errorInstance.set("msg", msg);
		throw new GemThrow(token, errorInstance);
	}

	private Object wrapNumber(double value) {
		return numberClass.call(this, List.of(value), true);
	}

	private Object wrapString(String value) {
		return stringClass.call(this, List.of(value, value.length()), true);
	}

	private Object wrapList(GemList list) {
		return listClass.call(this, List.of(list, list.size()), true);
	}

	private Object wrapBoolean(boolean value) { return booleanClass.call(this, List.of(value), true); }

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		Object value = expr.value;

		if (value instanceof String) {
			return stringClass.call(this, List.of(value, ((String) value).length()), true);
		} else if (value instanceof Double) {
			return numberClass.call(this, List.of(value), true);
		} else if (value instanceof Boolean) {
			return booleanClass.call(this, List.of(value), true);
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
		if(obj instanceof Integer){
			return wrapNumber(Double.valueOf((Integer) obj));
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
			case BANG: return wrap(!isTruthy(right));
			case MINUS:
				   checkNumberOperand(expr.operator, right);
				   return wrap(-(double)unwrap(right));
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
			return environment.get(name);
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
		if(a instanceof Double d1 && b instanceof Double d2){
			return nearlyEqualRel(d1, d2, 1e-9, 1e-12);
		}

		return a.equals(b);
	}

	private void checkNumberOperand(Token operator, Object operand) {
		operand = unwrap(operand);
		if (operand instanceof Double) return;
		runtimeError(operator, "Operand must be a number.");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) return;

		runtimeError(operator, "Operands must be numbers.");
	}
	void interpret(List<Stmt> statements){
		try{
			for(Stmt statement : statements){
				execute(statement);
			}
		}catch (GemThrow error) {
			System.err.println("[Line " + error.line + "] " + error.errorObject.klass.name() + ": " + error.msg);
			System.exit(0);
		}
	}

	private void execute(Stmt stmt){
		stmt.accept(this);
	}

	private static Object stringify(Object object){
		if(object == null) return "nil";

		if(object instanceof Double){
			String text = object.toString();
			if(text.endsWith(".0")){
				text = text.substring(0, text.length() - 2);
			}
			return Double.parseDouble(text);
		}

		return object;
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

		scopes.define(stmt.name.lexeme, currentClass);
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
		executeBlock(stmt.statements, new Environment(environment), currentClass);
		return null;
	}

	@Override
	public Void visitThrowStmt(Stmt.Throw stmt) {
		Object value = evaluate(stmt.value);
		if(value instanceof GemInstance instance){
			if(instance.isError()){
				StringBuilder str = new StringBuilder();
				for(int i = stackTrace.size() - 1; i >= 0; i--){
					str.append("\n").append(stackTrace.get(i));
				}
				instance.set("msg", unwrap(instance.get("msg")) + " (" + stmt.keyword.sourceFile.getFileName() + ":" + stmt.keyword.line + ")" + str.toString());
				throw new GemThrow(stmt.keyword, instance);
			}
		}
		runtimeError(stmt.keyword, "Only errors can be thrown.");
		return null;
	}

	@Override
	public Void visitTryStmt(Stmt.Try stmt) {
		Stack<String> backup = (Stack<String>) stackTrace.clone();
		try {
			execute(stmt.tryBlock);
		} catch (GemThrow error) {
			if (stmt.catchBlock instanceof Stmt.Block block) {
				stackTrace = backup;
				Environment catchEnv = new Environment(environment);
				catchEnv.define(stmt.errorVar.name.lexeme, error.errorObject);
				executeBlock(block.statements, catchEnv, currentClass);
			} else {
				throw error; // rethrow if no catch block
			}
		} finally {
			if(!(stmt.catchBlock instanceof Stmt.Block block)){
				stackTrace = backup;
			}
			if (stmt.finallyBlock != null) {
				execute(stmt.finallyBlock);
			}
		}
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

	public static Object unwrapAll(Object obj) {
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
			}catch(GemThrow error){
				callee = environment.get(new Token(TokenType.IDENTIFIER, varExpr.name.lexeme, null, varExpr.name.line, currentSourceFile));
			}

			callee = wrap(callee);

			if (!(callee instanceof GemCallable function)) {
				GemInstance errorInstance = new GemInstance(errorClass);
				errorInstance.set("msg", "Can only call functions and classes.");
				throw new GemThrow(varExpr.name, errorInstance);
			}

			if(callee.toString().equals("<native fn>")){
				System.err.println(currentSourceFile);
				for(int i = 0; i < arguments.size(); i++){
					arguments.set(i, unwrapAll(arguments.get(i)));
				}
			}


			stackTrace.add("at "+ varExpr.name.lexeme + " (" + varExpr.name.sourceFile.getFileName() + ":" + varExpr.name.line + ")");
			Object result = function.call(this, arguments);
			stackTrace.pop();

			if(callee instanceof GemClass && result instanceof String strResult){
				runtimeError(expr.paren, strResult);
			}

			return wrap(result);
		}

		Object callee = wrap(evaluate(expr.callee));

		if (!(callee instanceof GemCallable function)) {
			GemInstance errorInstance = new GemInstance(errorClass);
			errorInstance.set("msg", "Can only call functions and classes.");
			throw new GemThrow(expr.paren, errorInstance);

		}

		Object result = function.call(this, arguments);

		stackTrace.add("at " + expr.callee);
		return wrap(result);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if(object != null) {
			if (object instanceof GemInstance instance){
				Object value = instance.get(expr.name);
				if (value instanceof DeferredCallable function) {
					if(expr.name.lexeme.charAt(0) != '#'){
						return function;
					}
					if(expr.name.lexeme.charAt(0) == '#' && currentClass.equals(function.parent)) {
						return function;
					}
					runtimeError(expr.name, "Cannot access private method from current scope.");
				}
				else{
					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && currentClass.equals(scopes.get(expr.name))){
						return value;
					}
					runtimeError(expr.name, "Cannot access private field from current scope.");
				}
			}
			if(object instanceof GemClass clazz) {
				if(clazz.hasOverloadedStaticMethod(expr.name.lexeme)) {
					DeferredStaticCallable value = new DeferredStaticCallable(clazz, expr.name.lexeme, expr.name, clazz.name());
					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && currentClass.equals(value.parent)) {
						return value;
					}

					runtimeError(expr.name, "Cannot access private method from current scope.");
				}
				else{
					Object value = clazz.getStaticField(expr.name.lexeme);

					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && currentClass.equals(scopes.get(expr.name))){
						return value;
					}
					runtimeError(expr.name, "Cannot access private field from current scope.");
				}
			}
		}


		GemInstance errorInstance = new GemInstance(errorClass);
		errorInstance.set("msg", "Only instances have properties");
		throw new GemThrow(expr.name, errorInstance);
	}


	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);

		if(object instanceof GemClass clazz) {
			Object value = evaluate(expr.value);
			clazz.setStaticField(expr.name.lexeme, value);
			return value;
		}

		if(!(object instanceof GemInstance)){
			runtimeError(expr.name, "Cannot set property of non-instance '" + expr.name + "'.");
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
				runtimeError(expr.bracket, "Index " + start + " to " + end + " out of bounds for length " + (((String) obj).length() - 1) );
			}
			return wrapString(((String)obj).substring(start, end + 1));
		}

		if(indexStart instanceof Double && indexEnd instanceof Double)
			runtimeError(expr.bracket, "Only lists or strings can be indexed.");

		runtimeError(expr.bracket, "Index(s) must be a number.");
		return null;
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

		runtimeError(expr.bracket, "Only lists can be assigned by index.");
		return null;
	}


	void executeBlock(List<Stmt> statements, Environment environment, String clazz){
		Environment previous = this.environment;
		String prevClass = currentClass;
		try{
			this.currentClass = clazz;
			this.environment = environment;
			for(Stmt statement: statements) {
				execute(statement);
			}
		}
		finally{
			this.currentClass = prevClass;
			this.environment = previous;
		}
	}
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		// Mangle the name by arity
		String mangledName = mangleName(stmt.name.lexeme, stmt.params.size());
		GemFunction function = new GemFunction(stmt, environment, false, stmt.parent);
		environment.define(mangledName, function);

		String mangled = mangleName(stmt.name.lexeme, stmt.params.size());
		environment.define(mangled, function);

		if (!environment.exists(stmt.name.lexeme)) {
			environment.define(stmt.name.lexeme,
					new FunctionDispatcher(stmt.name.lexeme, environment, stmt.name));
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
			throw new RuntimeException("Invalid import: " + module);
		}
	}


	private String resolveImportPath(String moduleName, Path sourceFile) {
		Path baseDir;

		URL rootUrl = Interpreter.class.getClassLoader().getResource("com/interpreter/gem");

		if (rootUrl == null) {
			throw new RuntimeException("Could not locate current package.");
		}

		if (moduleName.startsWith("gem.")) {
			baseDir = Paths.get("internals");//Paths.get("/home/meow/com/interpreter/internals"); // define GEM_PATH somewhere
			moduleName = moduleName.replace("gem.", "");
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
				runtimeError(stmt.superclass.name,
						"Superclass must be a class.");
			}
		}

		environment.define(stmt.name.lexeme, null);

		if(stmt.superclass != null){
			environment = new Environment(environment);
			environment.define("super", superclass);
		}

		Map<String, GemFunction> methods = new HashMap<>();
		Map<String, GemFunction> staticMethods = new HashMap<>();
		Map<String, Object> staticFields = new HashMap<>();

		for (Stmt.Function method : stmt.methods) {
			String name = method.name.lexeme;
			int arity = method.params.size();
			boolean isInit = name.equals("init");

			String mangled = mangleName(name, arity);
			methods.put(mangled, new GemFunction(method, environment, isInit, method.parent));
		}

		for (Stmt.Function method : stmt.staticMethods) {
			String name = method.name.lexeme;
			int arity = method.params.size();
			if(name.equals("init")){runtimeError(method.name, "Class constructor cannot be static");}

			String mangled = mangleName(name, arity);
			staticMethods.put(mangled, new GemFunction(method, environment, false, method.parent));
		}

		for(Stmt.Var var: stmt.staticFields){
			String name = var.name.lexeme;
			Object value = null;
			if(var.initializer != null){
				value = evaluate(var.initializer);
			}
			staticFields.put(name, value);
		}

		GemClass klass = new GemClass(stmt.name.lexeme, (GemClass)superclass,methods, staticMethods, staticFields, currentSourceFile);

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