package com.interpreter.gem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GemClass implements GemCallable{
    final private String name;
    public final Map<String, GemFunction> methods;
    final GemClass superclass;
    public final Map<String, GemFunction> staticMethods;
    public final Map<String, Object> staticFields;
    public final Path currentSourceFile;

    GemClass(String name, GemClass superclass, Map<String, GemFunction> methods, Map<String, GemFunction> staticMethods, Map<String, Object> staticFields, Path currentFile) {
        this.name = name;
        this.methods = methods;
        this.superclass = superclass;
        this.staticMethods = staticMethods;
        this.staticFields = staticFields;
        this.currentSourceFile = currentFile;

        //System.out.println(name + " : " + methods.keySet());

        List<Object> keyset = new ArrayList<>(methods.keySet());
        Interpreter.scopes.put(name, keyset);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {

        if(this.name().charAt(0) == '#' && !interpreter.currentSourceFile.equals(currentSourceFile)){
            return "Can't access private class from current scope.";
        }

        GemInstance instance = new GemInstance(this);

        String mangled = Interpreter.mangleName("init", arguments.size());
        GemFunction initializer = findMethod(mangled);

        if (initializer != null){
            initializer.bind(instance).call(interpreter, arguments);
            return instance;
        }

        if(this.hasOverloadedMethod("init") || this.hasOverloadedMethod("#init")) {
            return "Can't find constructor for class with " + arguments.size() + " arguments.";
        }

        return instance;
    }

    public Object call(Interpreter interpreter, List<Object> arguments, boolean force) {
        GemInstance instance = new GemInstance(this);

        String mangled = Interpreter.mangleName("#init", arguments.size());
        GemFunction initializer = findMethod(mangled);

        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }


    public boolean hasOverloadedMethod(String baseName) {
        for (String methodName : methods.keySet()) {
            if (methodName.equals(baseName) || methodName.startsWith(baseName + "$")) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOverloadedStaticMethod(String baseName) {
        for (String methodName : staticMethods.keySet()) {
            if (methodName.equals(baseName) || methodName.startsWith(baseName + "$")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int arity() {
        GemFunction initializer = methods.get("init");
        if(initializer == null) {
            return 0;
        }
        return initializer.arity();
    }

    @Override
    public String name() {
        return name;
    }

    public GemFunction findMethod(String name) {
        if(methods.containsKey(name)) {
            return methods.get(name);
        }
        if(superclass != null) {
            return superclass.findMethod(name);
        }
        return null;
    }

    public GemFunction getStaticMethod(String name){
        if(staticMethods.containsKey(name)) {
            return staticMethods.get(name);
        }
        if(superclass != null) {
            return superclass.getStaticMethod(name);
        }
        return null;
    }

    public Object getStaticField(String name){
        if(staticFields.containsKey(name)) {
            return staticFields.get(name);
        }
        if(superclass != null) {
            return superclass.getStaticField(name);
        }
        return null;
    }

    public void setStaticField(String name, Object value){
        if(staticFields.containsKey(name)) {
            staticFields.put(name, value);
        }
        if(superclass != null) {
            superclass.setStaticField(name, value);
        }
    }

    public boolean isError(){
        if(this.name().equals("RuntimeError"))
            return true;
        if(superclass != null)
            return superclass.isError();
        return false;
    }
}
