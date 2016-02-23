package com.appdynamics.extensions.f5.models;

/**
 * @author Satish Muddam
 */
public class StatEntry {

    public enum Type {
        NUMERIC, STRING;
    }

    private String name;
    private String value;
    private Type type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
