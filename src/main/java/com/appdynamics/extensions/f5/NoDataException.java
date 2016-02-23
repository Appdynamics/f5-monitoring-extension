package com.appdynamics.extensions.f5;

/**
 * @author Satish Muddam
 */
public class NoDataException extends RuntimeException {

    public NoDataException(String message) {
        super(message);
    }

    public NoDataException(String message, Throwable e) {
        super(message, e);
    }

}
