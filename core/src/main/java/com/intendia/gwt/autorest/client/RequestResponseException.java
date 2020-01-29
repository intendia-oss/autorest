package com.intendia.gwt.autorest.client;

/* @Experimental */
public class RequestResponseException extends RuntimeException {

    public RequestResponseException(String msg, Throwable cause) { super(msg, cause); }

    public static class ResponseFormatException extends RequestResponseException {

        public ResponseFormatException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class FailedStatusCodeException extends RequestResponseException {
        private final int status;
        private final String responseText;

        public FailedStatusCodeException(int status, String responseText, String msg) {
            super(msg, null);
            this.status = status;
            this.responseText = responseText;
        }

        public int getStatusCode() { return status; }

        public String getResponseText() { return responseText; }
    }
}
