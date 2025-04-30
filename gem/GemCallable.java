package com.interpreter.gem;

import java.util.List;

interface GemCallable{
	Object call(Interpreter interpreter, List<Object> arguments);
	int arity();
}
