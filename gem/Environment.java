package com.interpreter.gem;

import java.util.HashMap;
import java.util.Map;

public class Environment{
	public final Map<String, Object> values = new HashMap<>();
	final Environment enclosing;

	Environment(){
		enclosing = null;
	}

	Environment(Environment enclosing){
		this.enclosing = enclosing;
	}

	void define(String name, Object value){
		values.put(name, value);
	}

	public Object get(Token name){
		if(values.containsKey(name.lexeme)){
			return values.get(name.lexeme);
		}

		if(enclosing != null) return enclosing.get(name);
		Interpreter.runtimeError(name, "Undefined variable '" + name.lexeme + "'.", "NameError");

		return null;
	}

	public Object get(String name){
		if(values.containsKey(name)){
			return values.get(name);
		}

		if(enclosing != null) return enclosing.get(name);

		return null;
	}

	void assign(Token name, Object value){
		if(values.containsKey(name.lexeme)){
			values.put(name.lexeme, value);
			return;
		}

		if(enclosing != null){
			enclosing.assign(name, value);
			return;
		}

		Interpreter.runtimeError(name, "Undefined '" + name.lexeme + "'.", "NameError");
	}

	public boolean exists(String name) {
		return values.containsKey(name);
	}


	Object getOrNull(Token name) {
		if (values.containsKey(name.lexeme)) return values.get(name.lexeme);
		if (enclosing != null) return enclosing.getOrNull(name);
		return null;
	}

	Object getAt(int distance, String name){
		return ancestor(distance).values.get(name);
	}

	Environment ancestor(int distance){
		Environment environment = this;
		for(int i = 0; i < distance; i++){
			environment = environment.enclosing;
		}
		return environment;
	}

	void assignAt(int distance,Token name, Object value){
		ancestor(distance).values.put(name.lexeme, value);
	}

}
