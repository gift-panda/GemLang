package com.interpreter.gem;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void>{

	public final Environment globals = new Environment();
	private Environment environment = globals;
	static public final Map<String, List<Object>> scopes = new HashMap<>();
	private final Map<Expr, Integer> locals = new HashMap<>();
	public Path currentSourceFile = null;
	private final List<String> alreadyImported = new ArrayList<>();
	private String currentClass = "~";
	public static Stack<String> stackTrace = new Stack<>();

	public static Path sourcePath;

	public class Break extends RuntimeException {}
	public class Continue extends RuntimeException {}

    static {
        try {
            sourcePath = Paths.get(Interpreter.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    Interpreter(){
		try {
			List<GemCallable> natives = loadAllNatives("com/interpreter/natives");

			for(GemCallable nativeFunction : natives){
				environment.define(mangleName(nativeFunction.name(), nativeFunction.arity()), nativeFunction);
			}

		}catch (Exception e){
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

	public static List<GemCallable> loadAllNatives(String directoryPath) throws Exception {

		List<GemCallable> natives = new ArrayList<>();
		try (JarFile jar = new JarFile(String.valueOf(sourcePath))) {
			Enumeration<JarEntry> entries = jar.entries();

			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				String name = entry.getName();

				if (name.startsWith(directoryPath) && name.endsWith(".nav")) {
					try (InputStream in = jar.getInputStream(entry);
						 ObjectInputStream ois = new ObjectInputStream(in)) {

						Object obj = ois.readObject();
						if (obj instanceof GemCallable nativeFunc) {
							natives.add(nativeFunc);
						}
					}
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
				try{
					return overload(leftRaw, rightRaw, "!=", expr.operator);
				}catch (Exception e) {
					return wrapBoolean(!isEqual(leftRaw, rightRaw));
				}
			case EQUAL_EQUAL:
				try{
					return overload(leftRaw, rightRaw, "==", expr.operator);
				}catch (Exception e) {
					return wrapBoolean(
							isEqual(leftRaw, rightRaw));
				}
		}

		switch (expr.operator.type) {
			case GREATER:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapBoolean((double) leftRaw > (double) rightRaw);
				return overload(leftRaw, rightRaw, ">", expr.operator);
			case GREATER_EQUAL:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapBoolean((double) leftRaw >= (double) rightRaw);
				return overload(leftRaw, rightRaw, ">=", expr.operator);
			case LESS:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapBoolean((double) leftRaw < (double) rightRaw);
				return overload(leftRaw, rightRaw, "<", expr.operator);
			case LESS_EQUAL:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapBoolean((double) leftRaw <= (double) rightRaw);
				return overload(leftRaw, rightRaw, "<=", expr.operator);
			case MINUS:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapNumber((double) leftRaw - (double) rightRaw);
				return overload(leftRaw, rightRaw, "-", expr.operator);
			case PERCEN:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapNumber((double) leftRaw % (double) rightRaw);
				return overload(leftRaw, rightRaw, "%", expr.operator);
			case BACKSLASH:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapNumber((double) ((int) (double) leftRaw / (int) (double) rightRaw));
				return overload(leftRaw, rightRaw, "\\", expr.operator);
			case SLASH:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapNumber((double) leftRaw / (double) rightRaw);
				return overload(leftRaw, rightRaw, "/", expr.operator);
			case STAR:
				if(checkNumberOperands(expr.operator, leftRaw, rightRaw))
					return wrapNumber((double) leftRaw * (double) rightRaw);
				return overload(leftRaw, rightRaw, "*", expr.operator);
			case PLUS:
				if (leftRaw instanceof Double && rightRaw instanceof Double) {
					return wrapNumber((double) leftRaw + (double) rightRaw);
				}
				if (leftRaw instanceof String || rightRaw instanceof String) {
					String leftText = stringify(leftRaw).toString();
					String rightText = stringify(rightRaw).toString();

					if (leftText.endsWith(".0")) {
						leftText = leftText.substring(0, leftText.length() - 2);
					}
					if (rightText.endsWith(".0")) {
						rightText = rightText.substring(0, rightText.length() - 2);
					}
					return wrapString(leftText + rightText);
				}
				return overload(leftRaw, rightRaw, "+", expr.operator);
		}
		return null;
	}

	public Object overload(Object left, Object right, String operator, Token token) {
		if(left instanceof GemInstance leftInst && right instanceof GemInstance rightInst) {
			GemFunction function = leftInst.klass.findMethod(mangleName(operator, 1));
			if (function != null) {
				return stringify(function.bind(leftInst).call(this, List.of(rightInst)));
			}
		}

		runtimeError(token, "Incompatible operands for operation " + operator + ", received " + typeOf(left) + " and " +  typeOf(right), "TypeError");
		throw new RuntimeException();
	}

	public static void runtimeError(Token token, String msg, String type){
		GemInstance errorInstance = new GemInstance(errorClass);
        StringBuilder msgBuilder = new StringBuilder();
		msgBuilder.append("\t").append("at (").append(token.sourceFile.getFileName()).append(":").append(token.line).append(")");
        for(int i = stackTrace.size() - 1; i >= 0; i--){
			msgBuilder.append("\n\t").append(stackTrace.get(i));
		}
        String stack = msgBuilder.toString();
        errorInstance.set("message", msg);
		errorInstance.set("stackTrace", stack);

		throw new GemThrow(token, errorInstance, type);
	}

	private Object wrapNumber(double value) {
		return numberClass.call(this, List.of(value), true);
	}

	private Object wrapString(String value) {
		return stringClass.call(this, List.of(value, (double)value.length()), true);
	}

	private Object wrapList(GemList list) {
		return listClass.call(this, List.of(list, (double)list.size()), true);
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
            return inst.get("#value"); // assumes wrapper stores it in `.value`
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
			return environment.get(name.lexeme);
		}
	}

	private Object evaluate(Expr expr){
		return (expr.accept(this));
	}

	private boolean isTruthy(Object object){
        return switch (object) {
			case null -> false;
            case GemInstance instance when instance.klass.name().equals("Boolean") -> (Boolean) instance.get("#value");
            case GemInstance instance when instance.klass.name().equals("Number") ->
                    (Double) instance.get("#value") != 0;
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
		runtimeError(operator, "Operand expected to be a number, received " + typeOf(operand), "TypeError");
	}

	private boolean checkNumberOperands(Token operator, Object left, Object right) {
		left = unwrap(left);
		right = unwrap(right);
		if (left instanceof Double && right instanceof Double) return true;

		return false;

		//runtimeError(operator, "Operands expected to be numbers, received " + typeOf(left) + " and " +  typeOf(right), "TypeError");
	}
	void interpret(List<Stmt> statements){
		try{
			for(Stmt statement : statements){
				execute(statement);
			}
		}catch (GemThrow error) {
			System.err.println("[Line " + error.line + "] " + error.name + ": " + error.msg);
			System.exit(70);
		}
	}

	private void execute(Stmt stmt) {
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

		if(object instanceof GemInstance instance){
			GemFunction function = instance.klass.findMethod(Interpreter.mangleName("toString",0));
			if(function != null) {
				function = function.bind(instance);
				Object result = Interpreter.unwrapAll(function.call(new Interpreter(), new ArrayList<>()));
				return stringify(unwrap(result));
			}
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

		//scopes.define(stmt.name.lexeme, currentClass);
		if(scopes.containsKey(currentClass))
			scopes.get(currentClass).add(stmt.name.lexeme);
		else
			scopes.put(currentClass, new ArrayList<>());
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
					str.append("\n\t").append(stackTrace.get(i));
				}
				instance.set("stackTrace", "\tat (" + stmt.keyword.sourceFile.getFileName() + ":" + stmt.keyword.line + ")" + str.toString());
				throw new GemThrow(stmt.keyword, instance, instance.klass.name());
			}
		}
		runtimeError(stmt.keyword, "Expected an error to throw, received " + typeOf(unwrap(value)), "TypeError");
		return null;
	}

	@Override
	public Void visitTryStmt(Stmt.Try stmt) {
		Stack<String> backup = (Stack<String>)stackTrace.clone();
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
	public Void visitWhileStmt(Stmt.While stmt) {
		try {
			while (isTruthy(evaluate(stmt.condition))) {
				try {
					execute(stmt.body);
				} catch (Continue e) {
					if(stmt.increment != null){
						//System.out.println("dis is not null");
						Environment prev = environment;
						environment = new Environment(environment);
						execute(stmt.increment);
						environment = prev;
						//System.out.println("done increment");
						//environment.get("i");
					}
				}
			}
		} catch (Break e) {}
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
				Token token = varExpr.name;
				String msg = "Expected functions or classes to call, received " + typeOf(unwrap(callee)) + ".";
				runtimeError(token, msg, "TypeError");
				throw new RuntimeException();
			}

			if(callee.toString().contains("native")){
				for(int i = 0; i < arguments.size(); i++){
					arguments.set(i, unwrapAll(arguments.get(i)));
				}
			}
			stackTrace.add("at "+ varExpr.name.lexeme + " (" + varExpr.name.sourceFile.getFileName() + ":" + varExpr.name.line + ")");

			Object result;
			result = function.call(this, arguments);

			stackTrace.pop();

			if(callee instanceof GemClass && result instanceof String strResult){
				if(strResult.endsWith("arguments."))
					runtimeError(expr.paren, strResult, "InstantiationError");
				else if(strResult.endsWith("scope."))
					runtimeError(expr.paren, strResult, "IllegalAccessError");
			}

			return wrap(result);
		}

		Object callee = wrap(evaluate(expr.callee));

		if (!(callee instanceof GemCallable function)) {
			runtimeError(expr.paren, "Expected functions or classes to call, received " + typeOf(unwrap(callee)) + ".", "TypeError");
			throw new RuntimeException();

		}

		Object result = function.call(this, arguments);

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
					if(expr.name.lexeme.charAt(0) == '#' && scopes.get(currentClass).contains(function.parent)) {
						return function;
					}
					runtimeError(expr.name, "Cannot access private method from current scope.", "IllegalAccessError");
				}
				else{


					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && scopes.get(currentClass).contains(expr.name.lexeme)){
						return value;
					}
					runtimeError(expr.name, "Cannot access private field from current scope.", "IllegalAccessError");
				}
			}
			if(object instanceof GemClass clazz) {
				if(clazz.hasOverloadedStaticMethod(expr.name.lexeme)) {
					DeferredStaticCallable value = new DeferredStaticCallable(clazz, expr.name.lexeme, expr.name, clazz.name());
					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && clazz.getStaticMethod(expr.name.lexeme) != null) {
						return value;
					}

					runtimeError(expr.name, "Cannot access private method from current scope.", "IllegalAccessError");
				}
				else{
					Object value = clazz.getStaticField(expr.name.lexeme);

					if(expr.name.lexeme.charAt(0) != '#'){
						return value;
					}
					if(expr.name.lexeme.charAt(0) == '#' && clazz.getStaticField(expr.name.lexeme) != null){
						return value;
					}
					runtimeError(expr.name, "Cannot access private field from current scope.", "IllegalAccessError");
				}
			}
		}


		runtimeError(expr.name, "Only instances have properties.", "TypeError");
		throw new RuntimeException();
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
			runtimeError(expr.name, "Expected instance, received " + object + ", '" + expr.name + "'.", "TypeError");
		}

		Object value = evaluate(expr.value);
		((GemInstance)object).set(expr.name, value);
		return value;
	}

	@Override
	public Object visitListLiteralExpr(Expr.ListLiteral expr) {
		GemList list = new GemList();
		for (Expr elementExpr : expr.elements) {
			list.add(unwrap(evaluate(elementExpr)));
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
				runtimeError(expr.bracket, "Index " + start + " to " + end + " out of bounds for length " + (((String) obj).length() - 1), "IndexOutOfBoundsError");
			}
			return wrapString(((String)obj).substring(start, end + 1));
		}

		if(indexStart instanceof Double && indexEnd instanceof Double)
			runtimeError(expr.bracket, "Expected a list or string, received + " + typeOf(obj), "TypeError");

		runtimeError(expr.bracket, "Index(s) must be a number.", "TypeError");
		return null;
	}

	@Override
	public Object visitSetIndexExpr(Expr.SetIndex expr) {
		Object obj = unwrap(evaluate(expr.object));
		Object index = unwrap(evaluate(expr.index));
		Object value = unwrap(evaluate(expr.value));
		if (obj instanceof GemList && index instanceof Double) {
			((GemList) obj).set(((Double) index).intValue(), value);
			return wrap(value);
		}

		runtimeError(expr.bracket, "Expected a list, received " + typeOf(obj) + " instead.", "TypeError");
		return null;
	}

	public String typeOf(Object object) {
		List<Object> temp = new ArrayList<>();
		temp.add(unwrap(object));
		return (String) ((GemCallable)environment.get("type$1")).call(this, temp);
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

	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		throw new Break();
	}

	@Override
	public Void visitContinueStmt(Stmt.Continue stmt) {
		throw new Continue();
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
		//System.out.println("Returning `" + unwrap(value) + "`");

		throw new Return(value);
	}

	@Override
	public Void visitImportStmt(Stmt.Import stmt) {
		String module = stmt.moduleName;
		String resolvedPath = null;
		try {
			resolvedPath = resolveImportPath(module, currentSourceFile);
		}
		catch(Exception e){
			runtimeError(stmt.keyword, "Cannot resolve import module: " + module, "ImportError");
		}

		Path currFile = currentSourceFile;
		this.currentSourceFile = Paths.get(resolvedPath);

		if (alreadyImported.contains(resolvedPath)) {
			this.currentSourceFile = currFile;
			return null;
		}

		alreadyImported.add(resolvedPath);

		String source = readFile(resolvedPath, stmt.keyword, module);
		List<Stmt> statements = new Parser(new Scanner(source, currentSourceFile).scanTokens(), currentSourceFile).parse();

		Resolver resolver = new Resolver(this, currentSourceFile);
		resolver.resolve(statements);

		interpret(statements);

		this.currentSourceFile = currFile;
		return null;
	}

	private String readFile(String pathOrResource, Token keyword, String module) {
		try {
			if (pathOrResource.startsWith("@internal/")) {
				String resourcePath = pathOrResource.substring("@internal/".length());

				try (InputStream in = Interpreter.class.getClassLoader().getResourceAsStream(resourcePath)) {
					if (in == null) {
						runtimeError(keyword, "Could not resolve import " + module, "ImportError");
					}
                    assert in != null;
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
				}
			} else {
				// External file path
				byte[] bytes = Files.readAllBytes(Paths.get(pathOrResource));
				return new String(bytes, StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			runtimeError(keyword, "Could not read file " + pathOrResource, "ImportError");
		}
		return null;
	}

	private String resolveImportPath(String moduleName, Path sourceFile) {
		if (moduleName.startsWith("gem.")) {
			String internalPath = "com/interpreter/internals/" +
					moduleName.substring("gem.".length()).replace('.', '/') + ".gem";

			return "@internal/" + internalPath;
		} else {
			Path baseDir;
			baseDir = Gem.currentSourceFile.getParent();
			String relativePath = moduleName.replace('.', File.separatorChar) + ".gem";
			return baseDir.resolve(relativePath).toString();
		}
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof GemClass)) {
				runtimeError(stmt.superclass.name,
						"Superclass must be a class.", "TypeError");
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

		// Inject instance getClass method
		Stmt.Function getClassMethod = makeGetClassMethod(stmt.name);
		String instanceMangled = mangleName("getClass", 0);
		methods.put(instanceMangled, new GemFunction(getClassMethod, environment, false, stmt.name.lexeme));

		for (Stmt.Function method : stmt.staticMethods) {
			String name = method.name.lexeme;
			int arity = method.params.size();
			if(name.equals("init")){runtimeError(method.name, "Class constructor cannot be static", "ModifierConflictError");}

			String mangled = mangleName(name, arity);
			staticMethods.put(mangled, new GemFunction(method, environment, false, method.parent));
		}

		// Inject static getSuper method
		Stmt.Function getSuperMethod = makeGetSuperMethod(stmt.name);
		String mangled = mangleName("getSuper", 0);
		staticMethods.put(mangled, new GemFunction(getSuperMethod, environment, false, stmt.name.lexeme));


		for(Stmt.Var var: stmt.staticFields){
			String name = var.name.lexeme;
			Object value = null;
			if(var.initializer != null){
				value = evaluate(var.initializer);
			}
			staticFields.put(name, value);
		}

        assert superclass instanceof GemClass;
		GemClass klass = new GemClass(stmt.name.lexeme, (GemClass) superclass ,methods, staticMethods, staticFields, currentSourceFile);

		if(superclass != null){
			environment = environment.enclosing;
		}
		environment.assign(stmt.name, klass);
		return null;
	}

	private Stmt.Function makeGetSuperMethod(Token className) {
		List<Token> params = Collections.emptyList();

		// Create `return super;` expression
		Expr returnExpr = new Expr.Variable(new Token(TokenType.IDENTIFIER, "super", null, className.line, currentSourceFile), "");
		Stmt returnStmt = new Stmt.Return(className, returnExpr);

		// Wrap into a block body
		List<Stmt> body = Collections.singletonList(returnStmt);

		// Construct function token
		Token nameToken = new Token(TokenType.IDENTIFIER, "getSuper", null, className.line, currentSourceFile);

		// Return synthetic Stmt.Function
		return new Stmt.Function(nameToken, params, body, className.lexeme);
	}

	private Stmt.Function makeGetClassMethod(Token className) {
		List<Token> params = Collections.emptyList();

		// Create `return this;` expression
		Expr returnExpr = new Expr.This(className);
		Stmt returnStmt = new Stmt.Return(className, returnExpr);

		// Wrap into a block body
		List<Stmt> body = Collections.singletonList(returnStmt);

		// Construct function token
		Token nameToken = new Token(TokenType.IDENTIFIER, "getClass", null, className.line, currentSourceFile);

		// Return synthetic Stmt.Function
		return new Stmt.Function(nameToken, params, body, className.lexeme);
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