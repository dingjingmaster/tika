package com.github.dingjingmaster.tika.main.FileOperation;


/**
 * Simple wrapper exception to be thrown for consistent handling
 * of exceptions that can happen during a parse.
 */
public class TikaServerParseException extends Exception {

    public TikaServerParseException(String msg) {
        super(msg);
    }

    public TikaServerParseException(Exception e) {
        super(e);
    }
}
