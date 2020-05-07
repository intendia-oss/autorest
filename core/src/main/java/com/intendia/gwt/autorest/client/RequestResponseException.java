package com.intendia.gwt.autorest.client;

/* @Experimental */
public class RequestResponseException extends RuntimeException {
	private static final long serialVersionUID = -5757679302341993741L;

	public RequestResponseException(String msg, Throwable cause) { super(msg, cause); }

    public static class ResponseFormatException extends RequestResponseException {
		private static final long serialVersionUID = -5240053826881194798L;

		public ResponseFormatException(String msg, Throwable cause) { super(msg, cause); }
    }

    public static class FailedStatusCodeException extends RequestResponseException {
		private static final long serialVersionUID = 6940506548373183794L;
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
