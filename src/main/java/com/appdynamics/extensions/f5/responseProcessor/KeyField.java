package com.appdynamics.extensions.f5.responseProcessor;

/**
 * @author Satish Muddam
 */
public class KeyField {

    private Field[] fieldNames;
    private String fieldSeparator;

    public Field[] getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(Field... fieldNames) {
        this.fieldNames = fieldNames;
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public void setFieldSeparator(String fieldSeperator) {
        this.fieldSeparator = fieldSeperator;
    }
}
