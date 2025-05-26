package com.interpreter.gem;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GemNative{
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: java GemNative <path/to/File.java>");
            return;
        }
        System.out.println("GemNative " + args[0]);
        File file = new File(args[0]);
        ArrayList<File> filesList = new ArrayList<>();

        if(file.exists()){
            if (file.isDirectory()) {
                for (File source : Objects.requireNonNull(file.listFiles())) {
                    if (source.isFile()) {
                        filesList.add(source);
                    }
                }
            }
            else {
                filesList.add(file);
            }
        }

        for(File f : filesList){
            makeNatives(f);
        }

        deleteDirectoryRecursively(new File("compiled_classes"));
    }

    private static void makeNatives(File javaFile) throws Exception{
        String className = getClassName(javaFile);  // infer from file name
        File tempOutDir = new File("compiled_classes");
        tempOutDir.mkdir();

        // 1. Compile the Java file

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(tempOutDir));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(javaFile);
        boolean success = compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();

        if (!success) {
            throw new RuntimeException("Compilation failed.");
        }

        // 2. Load compiled class
        URLClassLoader classLoader = new URLClassLoader(new URL[]{tempOutDir.toURI().toURL()});
        String fqcn = getFullyQualifiedClassName(javaFile);
        Class<?> clazz = classLoader.loadClass(fqcn);

        // 3. Check interface and instantiate
        if (!GemCallable.class.isAssignableFrom(clazz)) {
            throw new RuntimeException("Class does not implement GemFunction");
        }

        GemCallable instance = (GemCallable) clazz.getDeclaredConstructor().newInstance();

        // 4. Serialize the object to natives directory
        File outDir = new File("natives");
        System.out.println(outDir.getAbsolutePath());
        outDir.mkdir();
        try (ObjectOutputStream out = new ObjectOutputStream(
                new FileOutputStream(new File(outDir, className + ".nav")))) {
            out.writeObject(instance);
            System.out.println("Successfully implemented native function: " + className);
        }

        //System.out.println("Successfully implemented native function: " + className);
    }

    private static String getClassName(File javaFile) {
        String name = javaFile.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }

    private static String getFullyQualifiedClassName(File javaFile) throws IOException {
        String className = javaFile.getName().replace(".java", "");
        String packageName = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(javaFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("package ")) {
                    packageName = line.substring(8, line.indexOf(';')).trim();
                    break;
                }
            }
        }
        return packageName.isEmpty() ? className : packageName + "." + className;
    }

    public static void deleteDirectoryRecursively(File dir) {
        if (dir.isDirectory()) {
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                deleteDirectoryRecursively(file);
            }
        }
        dir.delete();
    }
}