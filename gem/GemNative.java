package com.interpreter.gem;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;

public class GemNative {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java GemNative <path/to/Class.class or directory>");
            return;
        }

        File input = new File(args[0]);
        if (!input.exists()) {
            System.err.println("File or directory does not exist.");
            return;
        }

        File rootDir = input.isDirectory() ? input : input.getParentFile();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{rootDir.toURI().toURL()});

        List<File> classFiles = new ArrayList<>();

        if (input.isDirectory()) {
            Files.walk(input.toPath())
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> classFiles.add(p.toFile()));
        } else if (input.getName().endsWith(".class")) {
            classFiles.add(input);
        }

        for (File classFile : classFiles) {
            makeNatives(classFile, rootDir, classLoader);
        }
    }

    private static void makeNatives(File classFile, File rootDir, ClassLoader classLoader) throws Exception {
        String fqcn = getFullyQualifiedClassNameFromFile(classFile, rootDir);

        if (!fqcn.startsWith("com.interpreter.GemNativeFunctions.")) return;

        String className = getClassNameFromFile(classFile) + ".class";



        Class<?> clazz = classLoader.loadClass(fqcn);



        if (!GemCallable.class.isAssignableFrom(clazz)) {
            throw new RuntimeException(fqcn + " does not implement GemCallable");
        }

        GemCallable instance = (GemCallable) clazz.getDeclaredConstructor().newInstance();

        File outDir = new File(Interpreter.sourcePath.toString(), "com/interpreter/natives");
        outDir.mkdirs();

        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(new File(outDir, className + ".nav")))) {
            out.writeObject(instance);
        }

        System.out.println("Successfully implemented native function: " + className);
    }

    private static String getClassNameFromFile(File classFile) {
        String name = classFile.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    private static String getFullyQualifiedClassNameFromFile(File classFile, File rootDir) {
        Path classPath = rootDir.toPath().relativize(classFile.toPath());
        String pathStr = classPath.toString().replace(File.separatorChar, '.');
        return pathStr.substring(0, pathStr.lastIndexOf('.')); // remove .class
    }
}
