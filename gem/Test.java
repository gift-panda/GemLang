package com.interpreter.gem;

public class Test {
    public static void main(String[] args) {
        Class<?> clazz = Test.class;

        Package pkg = clazz.getPackage();
        if (pkg != null) {
            System.out.println("Package Name: " + pkg.getName());
        } else {
            System.out.println("This class is in the default (unnamed) package.");
        }

        // Optionally get the physical location
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        System.out.println("Class File Location: " + path);
    }
}
