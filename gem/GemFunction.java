package com.interpreter.gem;

import java.util.List;

public class GemFunction implements GemCallable{
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	public final String parent;

	GemFunction(Stmt.Function declaration, Environment closure, boolean isInitializer, String currentClass){
		this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.parent = currentClass;
    }

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments){
		Environment environment = new Environment(closure);
		for(int i = 0; i < declaration.params.size(); i++){
			environment.define(declaration.params.get(i).lexeme, arguments.get(i));
		}
		try {
			interpreter.executeBlock(declaration.body, environment, parent);
		}catch(Return returnValue){
			if(isInitializer){
				return closure.getAt(0, "this");
			}
			return returnValue.value;
		}
		if (isInitializer) return closure.getAt(0, "this");
		return null;
	}

	@Override
	public int arity(){
		return declaration.params.size();
	}

	@Override
	public String name() {
		return null;
	}

	@Override
	public String toString(){
		return "<fn " + declaration.name.lexeme + ">";
	}

	public GemFunction bind(GemInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new GemFunction(declaration, environment, isInitializer, parent);
	}

	public GemFunction staticBind() {
		Environment environment = new Environment(closure);
		environment.define("hum" , "hi");
		return new GemFunction(declaration, environment, isInitializer, parent);
	}
}
