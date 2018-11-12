package com.fit2cloud.codedeploy.client;

public class Fit2CloudException extends RuntimeException {
    private static final long serialVersionUID = -649559784594858788L;

    public Fit2CloudException() {
    }

    public Fit2CloudException(String message, Throwable cause) {
        super(message, cause);
    }

    public Fit2CloudException(String message) {
        super(message);
    }

    public Fit2CloudException(Throwable cause) {
        super(cause);
    }
}
