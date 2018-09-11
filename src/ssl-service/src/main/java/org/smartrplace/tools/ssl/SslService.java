package org.smartrplace.tools.ssl;

import javax.net.ssl.SSLContext;

public interface SslService {

	SSLContext getContext(boolean initKeystore, boolean initTruststore) throws KeyStoreNotAvailableException;

	/**
	 * For testing we can disable the server certificate verification. Do not use in production.
	 * @param initKeystore
	 * @return
	 * @throws KeyStoreNotAvailableException
	 */
	SSLContext getUnsafeTrustAllContext(boolean initKeystore) throws KeyStoreNotAvailableException;
	
}
