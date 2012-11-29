package com.akdeniz.googleplaycrawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPublicKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.crypto.Cipher;
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
	
	private static final String GOOGLE_PUBLIC_KEY = "AAAAgMom/1a/v0lblO2Ubrt60J2gcuXSljGFQXgcyZWveWLEwo6prwgi3"
			+ "iJIZdodyhKZQrNWp5nKJ3srRXcUW+F1BD3baEVGcmEgqaLZUNBjm057pKRI16kB0YppeGx5qIQ5QjKzsR8ETQbKLNWgRY0Q"
			+ "RNVz34kMJR3P/LgHax/6rmf5AAAAAwEAAQ==";

	/**
	 * Parses key-value response into map.
	 */
	public static Map<String, String> parseResponse(String response) {

		Map<String, String> keyValueMap = new HashMap<String, String>();
		StringTokenizer st = new StringTokenizer(response, "\n\r");

		while (st.hasMoreTokens()) {
			String[] keyValue = st.nextToken().split("=");
			keyValueMap.put(keyValue[0], keyValue[1]);
		}

		return keyValueMap;
	}

	private static PublicKey createKey(byte[] keyByteArray) throws Exception {

		int modulusLength = readInt(keyByteArray, 0);
		byte[] modulusByteArray = new byte[modulusLength];
		System.arraycopy(keyByteArray, 4, modulusByteArray, 0, modulusLength);
		BigInteger modulus = new BigInteger(1, modulusByteArray);

		int exponentLength = readInt(keyByteArray, modulusLength + 4);
		byte[] exponentByteArray = new byte[exponentLength];
		System.arraycopy(keyByteArray, modulusLength + 8, exponentByteArray, 0, exponentLength);
		BigInteger publicExponent = new BigInteger(1, exponentByteArray);

		return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
	}

	/**
	 * Encrypts given string with Google Public Key.
	 *
	 */
	public static String encryptString(String str2Encrypt) throws Exception {

		byte[] keyByteArray = Base64.decode(GOOGLE_PUBLIC_KEY, Base64.DEFAULT);

		byte[] header = new byte[5];
		byte[] digest = MessageDigest.getInstance("SHA-1").digest(keyByteArray);
		header[0] = 0;
		System.arraycopy(digest, 0, header, 1, 4);

		PublicKey publicKey = createKey(keyByteArray);

		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWITHSHA1ANDMGF1PADDING");
		byte[] bytes2Encrypt = str2Encrypt.getBytes("UTF-8");
		int len = ((bytes2Encrypt.length - 1) / 86) + 1;
		byte[] cryptedBytes = new byte[len * 133];

		for (int j = 0; j < len; j++) {
			cipher.init(1, publicKey);
			byte[] arrayOfByte4 = cipher.doFinal(bytes2Encrypt, j * 86, (bytes2Encrypt.length - j * 86));
			System.arraycopy(header, 0, cryptedBytes, j * 133, header.length);
			System.arraycopy(arrayOfByte4, 0, cryptedBytes, j * 133 + header.length, arrayOfByte4.length);
		}
		return Base64.encodeToString(cryptedBytes, 10);
	}

	private static int readInt(byte[] data, int offset) {
		return (0xFF & data[offset]) << 24 | (0xFF & data[(offset + 1)]) << 16 | (0xFF & data[(offset + 2)]) << 8
				| (0xFF & data[(offset + 3)]);
	}
	
	/**
	 * Reads all contents of the input stream.
	 *
	 */
	public static byte[] readAll(InputStream inputStream) throws IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		int k = 0;
		for (; (k = inputStream.read(buffer)) != -1;) {
			outputStream.write(buffer, 0, k);
		}

		return outputStream.toByteArray();
	}

	public static Scheme getMockedScheme() throws NoSuchAlgorithmException, KeyManagementException {
		SSLContext sslcontext = SSLContext.getInstance("TLS");

		sslcontext.init(null, new TrustManager[] { new DummyX509TrustManager() }, null);
		SSLSocketFactory sf = new SSLSocketFactory(sslcontext);
		Scheme https = new Scheme("https", 443, sf);

		return https;
	}

	/**
	 * Dummy trust manager that accepts all certificates.
	 */
	static class DummyX509TrustManager implements X509TrustManager {

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
}
