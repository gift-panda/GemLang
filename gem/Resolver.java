package com.interpreter.gem;

import java.io.IOException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private ClassType currentClass = ClassType.NONE;
    private final List<String> internalImports = List.of("String", "Number", "Boolean");
    private final Path currentSourceFile;

    private enum ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        INITIALIZER,
        STATIC
    }

    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter, Path currentSourceFile) {
        this.interpreter = interpreter;
        this.currentSourceFile = currentSourceFile;
    }

    void resolve(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve(statement);
        }
    }

    public List<String> listFileNames(String dirPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(dirPath))) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        }
    }

    private List<String> internalImports() {
        try {
            List<String> internal = listFileNames("/home/meow/com/interpreter/internals");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Gem.error(name, "Name already declared in this scope in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitThrowStmt(Stmt.Throw stmt) {
        resolve(stmt.value);
        return null;
    }

    @Override
    public Void visitTryStmt(Stmt.Try stmt) {
        resolve(stmt.tryBlock);
        if(stmt.catchBlock != null) {
            beginScope();

            //declare(stmt.errorVar.name);
            //define(stmt.errorVar.name);
            resolve(stmt.catchBlock);

            endScope();
        }
        if(stmt.finallyBlock != null)
            resolve(stmt.finallyBlock);

        return null;
    }


    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;


        if(internalImports.contains(stmt.name.lexeme) && interpreter.globals.exists(stmt.name.lexeme)) {
            Gem.error(stmt.name, "Can't override an internal class.");
        }

        declare(stmt.name);
        define(stmt.name);

        if (stmt.superclass != null &&
                stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            Gem.error(stmt.superclass.name,
                    "A class can't inherit from itself.");
        }

        if(stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS;
            resolve(stmt.superclass);
            beginScope();
            scopes.peek().put("super", true);
        }

        beginScope();
        scopes.peek().put("this", true);

        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;

            if(method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }

            resolveFunction(method, declaration);
        }

        for (Stmt.Function method : stmt.staticMethods) {
            FunctionType declaration = FunctionType.STATIC;

            resolveFunction(method, declaration);
        }

        for(Stmt.Var var : stmt.staticFields){
            declare(var.name);
            if(var.initializer != null){
                resolve(var.initializer);
            }

            define(var.name);
        }

        endScope();

        if(stmt.superclass != null) {
            endScope();
        }

        currentClass = enclosingClass;
        return null;
    }

    @Override
    public Void visitSuperExpr(Expr.Super expr) {
        if (currentClass == ClassType.NONE) {
            Gem.error(expr.keyword,
                    "Can't use 'super' outside of a class.");
        } else if (currentClass != ClassType.SUBCLASS) {
            Gem.error(expr.keyword,
                    "Can't use 'super' in a class with no superclass.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }

        if(stmt.name.lexeme.charAt(0) == '#' && currentClass == ClassType.NONE) {
            //Gem.error(stmt.name, "Private variables can only exist within classes.");
        }

        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Gem.error(expr.name, "Can't read local variable in its own initializer.");
        }

        if(expr.name.lexeme.charAt(0) == '#' && currentClass == ClassType.NONE) {
            //Gem.error(expr.name, "Private variables can only exist within classes.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);

        if(expr.name.lexeme.charAt(0) == '#' && currentClass == ClassType.NONE) {
            //Gem.error(expr.name, "Private variables can only exist within classes.");
        }

        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);

        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Gem.error(stmt.keyword, "Can't return from top-level code.");
        }

        if(stmt.value != null && currentFunction == FunctionType.INITIALIZER) {
            Gem.error(stmt.keyword, "Can't return values from a constructor");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }

        return null;
    }

    @Override
    public Void visitImportStmt(Stmt.Import stmt) {
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);

        //if(expr.name.lexeme.charAt(0) == '#' && !(expr.object instanceof Expr.This)) {
          //  Gem.error(expr.name, "Cannot access private fields of this instance.");
        //}

        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if(currentClass == ClassType.NONE) {
            Gem.error(expr.keyword, "Can't use 'this' outside of a class.");
            return null;
        }

        if(currentFunction == FunctionType.STATIC){
            Gem.error(expr.keyword, "No instance of this in a static function.");
            return null;
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitGetIndexExpr(Expr.GetIndex expr) {
        resolve(expr.object);
        resolve(expr.indexStart);
        resolve(expr.indexEnd);
        return null;
    }

    @Override
    public Void visitSetIndexExpr(Expr.SetIndex expr) {
        resolve(expr.object);
        resolve(expr.index);
        resolve(expr.value);
        return null;
    }

    @Override
    public Void visitListLiteralExpr(Expr.ListLiteral expr) {
        for(Expr literal : expr.elements) {
            resolve(literal);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }
}
