package com.interpreter.gem;

public class GemNumber {
    private Double value;
    private boolean isInteger = false;

    public GemNumber(Double value){
        this.value = value;
    }

    public void makeInteger(){
        isInteger = true;
    }

    public Double getValue(){
        return value;
    }

    public static GemNumber parseDouble(String input){
        double value = Double.parseDouble(input);
        return new GemNumber(value);
    }

    public String toString(){
        if(isInteger){
            return value.toString().replace(".0", "");
        }
        return value.toString();
    }
}
