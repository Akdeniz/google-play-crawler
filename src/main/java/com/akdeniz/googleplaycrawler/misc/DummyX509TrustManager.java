package com.akdeniz.googleplaycrawler.misc;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Dummy trust manager that accepts all certificates.
 * 
 * @author akdeniz
 */
public class DummyX509TrustManager implements X509TrustManager {

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}

	@Override
	public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
			throws CertificateException {
	}

	@Override
	public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate, String paramString)
			throws CertificateException {
	}
};