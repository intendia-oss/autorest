package com.intendia.gwt.autorest.client;

/* @Experimental */
public class RequestResponseException extends RuntimeException {

    public RequestResponseException(String msg, Throwable cause) { super(msg, cause); }

    public static class ResponseFormatException extends RequestResponseException {

        public ResponseFormatException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class FailedStatusCodeException extends RequestResponseException {
        private final int status;

        public FailedStatusCodeException(int status, String msg) {
            super(msg, null);
            this.status = status;
        }

        public int getStatusCode() { return status; }
    }
}
