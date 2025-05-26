package com.interpreter.gem;

import java.io.Serializable;
import java.util.List;

public interface GemCallable extends Serializable{
	Object call(Interpreter interpreter, List<Object> arguments);
	int arity();
	String name();
}
