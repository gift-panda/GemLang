package com.interpreter.gem;

import java.util.ArrayList;
import java.util.List;

public class GemList{
    private final ArrayList<Object> items = new ArrayList<>();

    public Object get(Token token, int indexStart, int indexEnd) {
        if(indexStart < 0 || indexStart > items.size() - 1 || indexStart > indexEnd || indexEnd > items.size() - 1) {
            Interpreter.runtimeError(token, "Index " + indexStart + " to " + indexEnd + " out of bounds for length " + items.size(), "IndexOutOfBoundsError");
        }

        GemList result = new GemList();

        if(indexStart == indexEnd) {
            return items.get(indexStart);
        }

        for(int i = indexStart; i <= indexEnd; i++){
            result.add(items.get(i));
        }

        return result;
    }

    public void set(int index, Object value) {
        if(this == value){
            value = this.items.clone();
        }

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

    public List<Object> getItems() {
        return items;
    }

    @Override
    public String toString() {
        return items.toString().replace("null", "nil").replace(".0", "");
    }
}

