package edu.rpi.legup.app;

/**
 * {@code InvalidConfigException} is a custom exception class for handling invalid configuration
 * errors
 */
public class InvalidConfigException extends Exception {
    public InvalidConfigException(String message) {
        super(message);
    }
}
