package com.interpreter.gem;

import java.util.ArrayList;
import java.util.List;

public class GemList {
    private final List<Object> items = new ArrayList<>();

    public Object get(int index) {
        return items.get(index);
    }

    public void set(int index, Object value) {
        // Grow the list if index is out of bounds
        while (items.size() <= index) {
            items.add(null);
        }
        items.set(index, value);
    }


    public void add(Object value) {
        items.add(value);
    }

    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        return items.toString().replace("null", "nil").replace(".0", "");
    }
}

