package org.smartrplace.tools.ssl.impl;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.LoggerFactory;
import org.smartrplace.tools.ssl.KeyStoreNotAvailableException;
import org.smartrplace.tools.ssl.SslConstants;
import org.smartrplace.tools.ssl.SslService;


@Component(
		service = SslService.class,
		configurationPid=SslConstants.SSL_SERVICE_PID,
		configurationPolicy=ConfigurationPolicy.OPTIONAL
)
@Designate(ocd=SslConfig.class)
public class SslServiceImpl implements SslService {

	private volatile SslConfig config;
	private volatile KeyStore keyStore;
	private volatile KeyStore trustStore;
	
	@Activate
	protected void activate(SslConfig config) {
		this.config = config;
		try {
			this.keyStore = loadKeystore(config.keystorePath(), config.keystoreType(), config.provider());
		} catch (KeyStoreException | NoSuchProviderException e) {
			LoggerFactory.getLogger(SslService.class).error("Failed to load keystore at {}", config.keystorePath(), e);
		}
		try {
			this.trustStore = loadKeystore(config.truststorePath(), config.keystoreType(), config.provider());
		} catch (KeyStoreException | NoSuchProviderException e) {
			LoggerFactory.getLogger(SslService.class).error("Failed to load truststore at {}", config.truststorePath(), e);
		}
	}

	@Override
	public SSLContext getContext(boolean initKeystore, boolean initTruststore) throws KeyStoreNotAvailableException  {
		final SslConfig config = this.config;
		try {
			final SSLContext context;
				context = config.provider().isEmpty() ? SSLContext.getInstance("TLS") : SSLContext.getInstance("TLS", config.provider());
			final KeyManager[] keyManagers;
			final TrustManager[] trustManagers;
			if (initKeystore) { 
				final String algo = !config.algorithm().isEmpty() ? config.algorithm() : KeyManagerFactory.getDefaultAlgorithm();
				final KeyManagerFactory kmFactory = config.provider().isEmpty() ? KeyManagerFactory.getInstance(algo) :
					KeyManagerFactory.getInstance(algo, config.provider());
				kmFactory.init(keyStore, config.keystorePassword().toCharArray());
				keyManagers = kmFactory.getKeyManagers();
			} else {
				keyManagers = null;
			}
			if (initTruststore) {
				final String algo = !config.algorithm().isEmpty() ? config.algorithm() : TrustManagerFactory.getDefaultAlgorithm();
				final TrustManagerFactory tmFactory = config.provider().isEmpty() ? TrustManagerFactory.getInstance(algo) :
					TrustManagerFactory.getInstance(algo, config.provider());
				tmFactory.init(trustStore);
				trustManagers = tmFactory.getTrustManagers();
			} else {
				trustManagers = null;
			}
			context.init(keyManagers, trustManagers, null);
			return context;
		} catch (NoSuchAlgorithmException | NoSuchProviderException | UnrecoverableKeyException |
				KeyStoreException | KeyManagementException e) {
			throw new KeyStoreNotAvailableException("Failed to load keystore",e);
		} catch (NullPointerException e) {
			throw new IllegalStateException("Service is inactive",e);
		}
	}
	
	private static KeyStore loadKeystore(final String path, String keystoreType, final String provider) throws KeyStoreException, NoSuchProviderException {
		final Path pth ;
		try {
			pth = Paths.get(path);
		} catch (InvalidPathException e) {
			LoggerFactory.getLogger(SslService.class).error("Invalid keystore path {}",path);
			return null;
		}
		if (!Files.isRegularFile(pth))
			return null;
		if (keystoreType == null || keystoreType.isEmpty())
			keystoreType = KeyStore.getDefaultType();
		if (provider == null || provider.isEmpty())
			return KeyStore.getInstance(keystoreType);
		return KeyStore.getInstance(path, provider);
	}
	
}
