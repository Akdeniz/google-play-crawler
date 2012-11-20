package com.akdeniz.googleplaycrawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.akdeniz.googleplaycrawler.Googleplay.AndroidAppDeliveryData;
import com.akdeniz.googleplaycrawler.Googleplay.BuyResponse;
import com.akdeniz.googleplaycrawler.Googleplay.DetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.HttpCookie;
import com.akdeniz.googleplaycrawler.Googleplay.ResponseWrapper;
import com.akdeniz.googleplaycrawler.Googleplay.SearchResponse;
/**
 * 
 * @author akdeniz
 *
 */
public class GooglePlayAPI {

	private static String SERVICE = "androidmarket";
	private static String URL_LOGIN = "https://android.clients.google.com/auth";
	private static String ACCOUNT_TYPE_HOSTED_OR_GOOGLE = "HOSTED_OR_GOOGLE";

	private String authSubToken;
	private String androidId;

	public GooglePlayAPI(String androidId) {
		this.androidId = androidId;
	}

	public void login(String email, String password) throws IOException {
		HttpEntity responseEntity = executePost(URL_LOGIN, new String[][] {
				{ "Email", email }, { "Passwd", password },
				{ "service", SERVICE },
				{ "accountType", ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
				{ "has_permission", "1" }, { "source", "android" },
				{ "androidId", this.androidId },
				{ "app", "com.android.vending" }, { "device_country", "tr" },
				{ "lang", "tr" }, { "sdk_version", "16" }, }, null);

		StringTokenizer st = new StringTokenizer(new String(
				readAll(responseEntity.getContent())), "\n\r=");
		while (st.hasMoreTokens()) {
			if (st.nextToken().equalsIgnoreCase("Auth")) {
				String token = st.nextToken();
				System.out.println("authSubToken found : "+ token);
				setAuthSubToken(token);
				break;
			}
		}
		if (getAuthSubToken() == null)
			throw new IOException("authSubToken not found in ");
	}

	private static byte[] readAll(InputStream inputStream) throws IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		int k = 0;
		for (; (k = inputStream.read(buffer)) != -1;) {
			outputStream.write(buffer, 0, k);
		}

		return outputStream.toByteArray();
	}

	private HttpEntity executePost(String url, String[][] postParams,
			String[][] headerParams) throws IOException {

		HttpPost httppost = new HttpPost(url);

		if (headerParams != null) {
			for (String[] param : headerParams) {
				httppost.setHeader(param[0], param[1]);
			}
		}

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();

		for (String[] param : postParams) {
			formparams.add(new BasicNameValuePair(param[0], param[1]));
		}

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams,
				"UTF-8");
		httppost.setEntity(entity);

		return executeHttpRequest(httppost);
	}

	private HttpEntity executeGet(String url, String[][] postParams,
			String[][] headerParams) throws IOException {

		if (postParams != null) {
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();

			for (String[] param : postParams) {
				formparams.add(new BasicNameValuePair(param[0], param[1]));
			}

			url = url + "?" + URLEncodedUtils.format(formparams, "UTF-8");
		}

		HttpGet httpget = new HttpGet(url);

		if (headerParams != null) {
			for (String[] param : headerParams) {
				httpget.setHeader(param[0], param[1]);
			}
		}

		return executeHttpRequest(httpget);
	}

	private HttpEntity executeHttpRequest(HttpUriRequest request)
			throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();

		/*try {
			client.getConnectionManager().getSchemeRegistry()
					.register(Utils.getMockedScheme());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// proxy TODO : disable!
		HttpHost proxy = new HttpHost("localhost", 8888);
		client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);*/

		HttpResponse response = client.execute(request);

		return response.getEntity();
	}

	public String getAuthSubToken() {
		return authSubToken;
	}

	public void setAuthSubToken(String authSubToken) {
		this.authSubToken = authSubToken;
	}

	public SearchResponse search(String query) throws IOException {
		return search(query, 0, 10);
	}

	public SearchResponse search(String query, int offset, int numberOfResult)
			throws IOException {

		ResponseWrapper responseWrapper = executeRequestAPI2(
				"search",
				new String[][] { { "c", "3" }, { "q", query },
						{ "o", String.valueOf(offset) },
						{ "n", String.valueOf(numberOfResult) }, });

		return responseWrapper.getPayload().getSearchResponse();
	}

	public DetailsResponse details(String packageName) throws IOException {
		ResponseWrapper responseWrapper = executeRequestAPI2("details",
				new String[][] { { "doc", packageName }, });

		return responseWrapper.getPayload().getDetailsResponse();
	}

	private BuyResponse purchase(String packageName, int versionCode,
			int offerType) throws IOException {

		ResponseWrapper responseWrapper = executeRequestAPI1(
				"purchase",
				new String[][] { { "ot", String.valueOf(offerType) },
						{ "doc", packageName },
						{ "vc", String.valueOf(versionCode) }, });

		return responseWrapper.getPayload().getBuyResponse();
	}

	public InputStream download(String packageName, int versionCode,
			int offerType) throws IOException {

		BuyResponse buyResponse = purchase(packageName, versionCode, offerType);

		AndroidAppDeliveryData appDeliveryData = buyResponse
				.getPurchaseStatusResponse().getAppDeliveryData();

		String downloadUrl = appDeliveryData.getDownloadUrl();
		HttpCookie downloadAuthCookie = appDeliveryData
				.getDownloadAuthCookie(0);

		return executeDownload(downloadUrl, downloadAuthCookie.getName() + "="
				+ downloadAuthCookie.getValue());

	}

	public ResponseWrapper executeRequestAPI2(String path, String[][] datapost)
			throws IOException {

		String[][] headerParams = new String[][] {
				{ "Accept-Language", "tr-TR" },
				{ "Authorization", "GoogleLogin auth=" + this.authSubToken },
				{ "X-DFE-Enabled-Experiments",
						"cl:billing.select_add_instrument_by_default" },
				{
						"X-DFE-Unsupported-Experiments",
						"nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes" },
				{ "X-DFE-Device-Id", this.androidId },
				{ "X-DFE-Client-Id", "am-android-google" },
				{
						"User-Agent",
						"Android-Finsky/3.7.13 (api=3,versionCode=8013013,sdk=16,device=crespo,hardware=herring,product=soju)" },
				{ "X-DFE-SmallestScreenWidthDp", "320" },
				{ "X-DFE-Filter-Level", "3" },
				{ "Host", "android.clients.google.com" },
				{ "Content-Type",
						"application/x-www-form-urlencoded; charset=UTF-8" } };

		HttpEntity httpEntity = executeGet(
				"https://android.clients.google.com/fdfe/" + path, datapost,
				headerParams);
		return Googleplay.ResponseWrapper.parseFrom(httpEntity.getContent());

	}

	public ResponseWrapper executeRequestAPI1(String path, String[][] datapost)
			throws IOException {

		String[][] headerParams = new String[][] {
				{ "Accept-Language", "tr-TR" },
				{ "Authorization", "GoogleLogin auth=" + this.authSubToken },
				{ "X-DFE-Enabled-Experiments",
						"cl:billing.select_add_instrument_by_default" },
				{
						"X-DFE-Unsupported-Experiments",
						"nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes" },
				{ "X-DFE-Device-Id", this.androidId },
				{ "X-DFE-Client-Id", "am-android-google" },
				{
						"User-Agent",
						"Android-Finsky/3.7.13 (api=3,versionCode=8013013,sdk=16,device=crespo,hardware=herring,product=soju)" },
				{ "X-DFE-SmallestScreenWidthDp", "320" },
				{ "X-DFE-Filter-Level", "3" },
				{ "Host", "android.clients.google.com" },
				{ "Content-Type",
						"application/x-www-form-urlencoded; charset=UTF-8" } };

		HttpEntity httpEntity = executePost(
				"https://android.clients.google.com/fdfe/" + path, datapost,
				headerParams);
		return Googleplay.ResponseWrapper.parseFrom(httpEntity.getContent());

	}

	public InputStream executeDownload(String url, String cookie)
			throws IOException {

		String[][] headerParams = new String[][] {
				{ "Cookie", cookie },
				{ "User-Agent",
						"AndroidDownloadManager/4.1.1 (Linux; U; Android 4.1.1; Nexus S Build/JRO03E)" }, };

		HttpEntity httpEntity = executeGet(url, null, headerParams);
		return httpEntity.getContent();

	}

}
