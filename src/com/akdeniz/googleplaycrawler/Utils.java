package com.akdeniz.googleplaycrawler;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;

/**
 * 
 * @author akdeniz
 *
 */
public class Utils {

	public static Scheme getMockedScheme() throws NoSuchAlgorithmException,
			KeyManagementException {
		SSLContext sslcontext = SSLContext.getInstance("TLS");

		sslcontext.init(null,
				new TrustManager[] { new DummyX509TrustManager() }, null);
		SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
		Scheme https = new Scheme("https", 443, sf);

		return https;
	}
	
	static class DummyX509TrustManager implements X509TrustManager {

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }

        @Override
        public void checkServerTrusted(
                X509Certificate[] paramArrayOfX509Certificate,
                String paramString) throws CertificateException
        {
        }

        @Override
        public void checkClientTrusted(
                X509Certificate[] paramArrayOfX509Certificate,
                String paramString) throws CertificateException
        {
        }
    };
}
