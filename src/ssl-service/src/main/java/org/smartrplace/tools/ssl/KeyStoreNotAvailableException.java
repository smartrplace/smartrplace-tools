package org.smartrplace.tools.ssl;

public class KeyStoreNotAvailableException extends Exception {

	private static final long serialVersionUID = 1L;

	public KeyStoreNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public KeyStoreNotAvailableException(String message) {
		super(message);
	}
	
}
