package com.digisoft.mss.model;

/**
 * Generic error container for all wrong cases.
 */
public class Error {
    private int code;
    private String message;

    public Error(int code, String message) {

        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public Error setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Error setMessage(String message) {
        this.message = message;
        return this;
    }
}
