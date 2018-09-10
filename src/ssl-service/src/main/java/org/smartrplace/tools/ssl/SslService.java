package org.smartrplace.tools.ssl;

import javax.net.ssl.SSLContext;

public interface SslService {

	SSLContext getContext(boolean initKeystore, boolean initTruststore) throws KeyStoreNotAvailableException;
	
}
