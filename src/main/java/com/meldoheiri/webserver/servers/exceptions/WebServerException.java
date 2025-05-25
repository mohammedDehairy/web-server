package com.meldoheiri.webserver.servers.exceptions;

public class WebServerException extends Exception {
    public WebServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebServerException(String message) {
        super(message);
    }
}
