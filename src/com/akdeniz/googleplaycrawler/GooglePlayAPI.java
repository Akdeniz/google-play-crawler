package com.akdeniz.googleplaycrawler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.akdeniz.googleplaycrawler.GoogleServicesFramework.AndroidCheckinRequest;
import com.akdeniz.googleplaycrawler.GoogleServicesFramework.AndroidCheckinResponse;
import com.akdeniz.googleplaycrawler.Googleplay.AndroidAppDeliveryData;
import com.akdeniz.googleplaycrawler.Googleplay.BrowseResponse;
import com.akdeniz.googleplaycrawler.Googleplay.BulkDetailsRequest;
import com.akdeniz.googleplaycrawler.Googleplay.BulkDetailsRequest.Builder;
import com.akdeniz.googleplaycrawler.Googleplay.BulkDetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.BuyResponse;
import com.akdeniz.googleplaycrawler.Googleplay.DetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.HttpCookie;
import com.akdeniz.googleplaycrawler.Googleplay.ListResponse;
import com.akdeniz.googleplaycrawler.Googleplay.ResponseWrapper;
import com.akdeniz.googleplaycrawler.Googleplay.SearchResponse;

/**
 * 
 * @author akdeniz
 * 
 */
public class GooglePlayAPI {

	private static final String FDFE_URL = "https://android.clients.google.com/fdfe/";
	private static String SERVICE = "androidmarket";
	private static String URL_LOGIN = "https://android.clients.google.com/auth";
	private static String ACCOUNT_TYPE_HOSTED_OR_GOOGLE = "HOSTED_OR_GOOGLE";
	private static String CHECKIN_REQUEST_BASE64 = "EAAitQEKiwEKNWdlbmVyaWMvc2RrL2dlbmVyaWM6NC4"
			+ "xLjEvSlJPMDNFLzQwMzA1OTplbmcvdGVzdC1rZXlzEghnb2xkZmlzaBoHZ2VuZXJpYyoHdW5"
			+ "rbm93bjIOYW5kcm9pZC1nb29nbGU4kJOCgAVAEEoHZ2VuZXJpY1AQWgNzZGtiB3Vua25vd25"
			+ "qA3Nka3AAEAAyBjMxMDI2MDoGMzEwMjYwQhFtb2JpbGUtbm90cm9hbWluZ0gAMgVlbl9VU1I"
			+ "PMDAwMDAwMDAwMDAwMDAwYg9FdXJvcGUvSXN0YW5idWxwA5IBjQkIAxACGAMgAigBMAE48AF"
			+ "AAEoTYW5kcm9pZC50ZXN0LnJ1bm5lckodY29tLmFuZHJvaWQubG9jYXRpb24ucHJvdmlkZXJ"
			+ "KCmphdmF4Lm9iZXhSGmFuZHJvaWQuaGFyZHdhcmUuYmx1ZXRvb3RoUhdhbmRyb2lkLmhhcmR"
			+ "3YXJlLmNhbWVyYVIhYW5kcm9pZC5oYXJkd2FyZS5jYW1lcmEuYXV0b2ZvY3VzUhphbmRyb2l"
			+ "kLmhhcmR3YXJlLmZha2V0b3VjaFIZYW5kcm9pZC5oYXJkd2FyZS5sb2NhdGlvblIhYW5kcm9"
			+ "pZC5oYXJkd2FyZS5sb2NhdGlvbi5uZXR3b3JrUhthbmRyb2lkLmhhcmR3YXJlLm1pY3JvcGh"
			+ "vbmVSIWFuZHJvaWQuaGFyZHdhcmUuc2NyZWVuLmxhbmRzY2FwZVIgYW5kcm9pZC5oYXJkd2F"
			+ "yZS5zY3JlZW4ucG9ydHJhaXRSJWFuZHJvaWQuaGFyZHdhcmUuc2Vuc29yLmFjY2VsZXJvbWV"
			+ "0ZXJSH2FuZHJvaWQuaGFyZHdhcmUuc2Vuc29yLmNvbXBhc3NSHGFuZHJvaWQuaGFyZHdhcmU"
			+ "udG91Y2hzY3JlZW5SJ2FuZHJvaWQuaGFyZHdhcmUudG91Y2hzY3JlZW4ubXVsdGl0b3VjaFI"
			+ "wYW5kcm9pZC5oYXJkd2FyZS50b3VjaHNjcmVlbi5tdWx0aXRvdWNoLmRpc3RpbmN0UjBhbmR"
			+ "yb2lkLmhhcmR3YXJlLnRvdWNoc2NyZWVuLm11bHRpdG91Y2guamF6emhhbmRaC2FybWVhYmk"
			+ "tdjdhWgdhcm1lYWJpYOADaKAGcgJhcnIFYXJfRUdyBWFyX0lMcgJiZ3IFYmdfQkdyAmNhcgV"
			+ "jYV9FU3ICY3NyBWNzX0NacgJkYXIFZGFfREtyAmRlcgVkZV9BVHIFZGVfQ0hyBWRlX0RFcgV"
			+ "kZV9MSXICZWxyBWVsX0dScgJlbnIFZW5fQVVyBWVuX0NBcgVlbl9HQnIFZW5fSUVyBWVuX0l"
			+ "OcgVlbl9OWnIFZW5fU0dyBWVuX1VTcgVlbl9aQXICZXNyBWVzX0VTcgVlc19VU3ICZmFyAmZ"
			+ "pcgVmaV9GSXICZnJyBWZyX0JFcgVmcl9DQXIFZnJfQ0hyBWZyX0ZScgJoaXIFaGlfSU5yAmh"
			+ "ycgVocl9IUnICaHVyBWh1X0hVcgJpZHICaW5yAml0cgVpdF9DSHIFaXRfSVRyAml3cgJqYXI"
			+ "FamFfSlByAmtvcgVrb19LUnICbHRyBWx0X0xUcgJsdnIFbHZfTFZyAm5icgVuYl9OT3ICbmx"
			+ "yBW5sX0JFcgVubF9OTHICcGxyBXBsX1BMcgJwdHIFcHRfQlJyBXB0X1BUcgJyb3IFcm9fUk9"
			+ "yAnJ1cgVydV9SVXICc2tyBXNrX1NLcgJzbHIFc2xfU0lyAnNycgVzcl9SU3ICc3ZyBXN2X1N"
			+ "FcgJ0aHIFdGhfVEhyAnRscgV0bF9QSHICdHJyBXRyX1RScgJ1a3IFdWtfVUFyAnZpcgV2aV9" + "WTnIFemhfQ05yBXpoX1RXoAEA";

	private String authSubToken;
	private String androidId;
	private String securityToken;

	public GooglePlayAPI(String androidId) {
		this.setAndroidId(androidId);
	}
	
	public GooglePlayAPI() {
	}

	/**
	 * Authenticate with given email and password.
	 * 
	 * If android id is not supplied while initializing, this method will try to acquire one.
	 */
	public void login(String email, String password) throws Exception {

		if (this.getAndroidId() == null) {
			AndroidCheckinResponse checkinResponse = checkin(Base64.decode(CHECKIN_REQUEST_BASE64, Base64.NO_WRAP));

			this.setAndroidId(BigInteger.valueOf(checkinResponse.getAndroidId()).toString(16));
			this.setSecurityToken(BigInteger.valueOf(checkinResponse.getSecurityToken()).toString(16));

			authAndCheckin(email, password);

		} else {
			HttpEntity responseEntity = executePost(URL_LOGIN, new String[][] { { "Email", email },
																				{ "Passwd", password },
																				{ "service", SERVICE },
																				{	"accountType",
																					ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
																				{ "has_permission", "1" },
																				{ "source", "android" },
																				{ "androidId", this.getAndroidId() },
																				{ "app", "com.android.vending" },
																				{ "device_country", "en" },
																				{ "lang", "en" },
																				{ "sdk_version", "16" }, }, null);

			Map<String, String> response = Utils.parseResponse(new String(Utils.readAll(responseEntity.getContent())));
			if(response.containsKey("Auth")){
				setAuthSubToken(response.get("Auth"));
			} else {
				throw new GooglePlayException("Authentication failed!");
			}
		}
	}

	/**
	 * Performs authentication on three different service (ac2dm, sierra, androidmarket) and match up
	 * android id, security token and email by checking them in.
	 *
	 */
	private void authAndCheckin(String email, String password) throws Exception {

		StringBuilder builder = new StringBuilder();
		builder.append(email);
		builder.append("\u0000");
		builder.append(password);

		String encryptedPassword = Utils.encryptString(builder.toString());

		HttpEntity c2dmResponseEntity = executePost(URL_LOGIN,
				new String[][] { { "Email", email },
								{ "EncryptedPasswd", encryptedPassword },
								{ "service", "ac2dm" },
								{ "accountType", ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
								{ "has_permission", "1" },
								{ "add_account", "1" },
								{ "source", "android" },
								{ "androidId", this.getAndroidId() },
								{ "device_country", "us" },
								{ "device_country", "us" },
								{ "lang", "en" },
								{ "sdk_version", "16" }, }, null);

		Map<String, String> c2dmAuth = Utils.parseResponse(new String(Utils.readAll(c2dmResponseEntity.getContent())));

		HttpEntity sierraResponseEntity = executePost(URL_LOGIN,
				new String[][] { { "Email", email },
								{ "Token", c2dmAuth.get("Token") },
								{ "service", "sierra" },
								{ "accountType", ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
								{ "has_permission", "1" },
								{ "source", "android" },
								{ "androidId", this.getAndroidId() },
								{ "app", "com.android.vending" },
								{ "client_sig", "38918a453d07199354f8b19af05ec6562ced5788" },
								{ "device_country", "us" },
								{ "device_country", "us" },
								{ "lang", "en" },
								{ "sdk_version", "16" }, }, null);

		//Map<String, String> sierraAuth = Utils.parseResponse(new String(readAll(sierraResponseEntity.getContent())));

		HttpEntity marketResponseEntity = executePost(URL_LOGIN,
				new String[][] { { "Email", email },
								{ "Token", c2dmAuth.get("Token") },
								{ "service", "androidmarket" },
								{ "accountType", ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
								{ "has_permission", "1" },
								{ "source", "android" },
								{ "androidId", this.getAndroidId() },
								{ "app", "com.android.vending" },
								{ "client_sig", "38918a453d07199354f8b19af05ec6562ced5788" },
								{ "device_country", "us" },
								{ "device_country", "us" },
								{ "lang", "en" },
								{ "sdk_version", "16" }, }, null);

		Map<String, String> marketAuth = Utils.parseResponse(new String(Utils.readAll(marketResponseEntity.getContent())));

		if (marketAuth.containsKey("Auth")) {
			setAuthSubToken(marketAuth.get("Auth"));
		} else {
			throw new GooglePlayException("Authentication failed!");
		}

		AndroidCheckinRequest checkinRequest = GoogleServicesFramework.AndroidCheckinRequest.parseFrom(Base64.decode(
				CHECKIN_REQUEST_BASE64, Base64.NO_WRAP));
		GoogleServicesFramework.AndroidCheckinRequest.Builder checkInbuilder = GoogleServicesFramework.AndroidCheckinRequest
				.newBuilder(checkinRequest);

		checkInbuilder.setId(new BigInteger(this.getAndroidId(), 16).longValue());
		checkInbuilder.setSecurityToken(new BigInteger(this.getSecurityToken(), 16).longValue());
		checkInbuilder.addAccountCookie("[" + email + "]");
		checkInbuilder.addAccountCookie(marketAuth.get("SID"));

		AndroidCheckinRequest build = checkInbuilder.build();
		AndroidCheckinResponse checkinResponse = checkin(build.toByteArray());

	}

	private HttpEntity executePost(String url, String[][] postParams, String[][] headerParams) throws IOException {

		List<NameValuePair> formparams = new ArrayList<NameValuePair>();

		for (String[] param : postParams) {
			if (param[0] != null && param[1] != null) {
				formparams.add(new BasicNameValuePair(param[0], param[1]));
			}
		}

		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");

		return executePost(url, entity, headerParams);
	}

	private HttpEntity executePost(String url, HttpEntity postData, String[][] headerParams) throws IOException {
		HttpPost httppost = new HttpPost(url);

		if (headerParams != null) {
			for (String[] param : headerParams) {
				if (param[0] != null && param[1] != null) {
					httppost.setHeader(param[0], param[1]);
				}
			}
		}

		httppost.setEntity(postData);

		return executeHttpRequest(httppost);
	}

	private HttpEntity executeGet(String url, String[][] postParams, String[][] headerParams) throws IOException {

		if (postParams != null) {
			List<NameValuePair> formparams = new ArrayList<NameValuePair>();

			for (String[] param : postParams) {
				if (param[0] != null && param[1] != null) {
					formparams.add(new BasicNameValuePair(param[0], param[1]));
				}
			}

			url = url + "?" + URLEncodedUtils.format(formparams, "UTF-8");
		}

		HttpGet httpget = new HttpGet(url);

		if (headerParams != null) {
			for (String[] param : headerParams) {
				if (param[0] != null && param[1] != null) {
					httpget.setHeader(param[0], param[1]);
				}
			}
		}

		return executeHttpRequest(httpget);
	}

	private HttpEntity executeHttpRequest(HttpUriRequest request) throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();

		/*
		 * try {
		 * client.getConnectionManager().getSchemeRegistry().register(Utils
		 * .getMockedScheme()); } catch (Exception e) { throw new
		 * RuntimeException(e); }
		 * 
		 * // proxy TODO : disable! 
		 * HttpHost proxy = new HttpHost("localhost",
		 * 8888); client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
		 * proxy);
		 */

		HttpResponse response = client.execute(request);
		
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new GooglePlayException(new String(Utils.readAll(response
					.getEntity().getContent())));
		}

		return response.getEntity();
	}

	public String getAuthSubToken() {
		return authSubToken;
	}

	public void setAuthSubToken(String authSubToken) {
		this.authSubToken = authSubToken;
	}

	public String getAndroidId() {
		return androidId;
	}

	public void setAndroidId(String androidId) {
		this.androidId = androidId;
	}

	public String getSecurityToken() {
		return securityToken;
	}

	public void setSecurityToken(String securityToken) {
		this.securityToken = securityToken;
	}

	public SearchResponse search(String query) throws IOException {
		return search(query, null, null);
	}

	public SearchResponse search(String query, Integer offset, Integer numberOfResult) throws IOException {

		ResponseWrapper responseWrapper = executeGETRequest("search",
				new String[][] { { "c", "3" },
								{ "q", query },
								{ "o", (offset == null) ? null : String.valueOf(offset) },
								{ "n", (numberOfResult == null) ? null : String.valueOf(numberOfResult) }, });

		return responseWrapper.getPayload().getSearchResponse();
	}

	public DetailsResponse details(String packageName) throws IOException {
		ResponseWrapper responseWrapper = executeGETRequest("details", new String[][] { { "doc", packageName }, });

		return responseWrapper.getPayload().getDetailsResponse();
	}

	public BulkDetailsResponse bulkDetails(String[] packageNames) throws IOException {

		Builder bulkDetailsRequestBuilder = BulkDetailsRequest.newBuilder();
		bulkDetailsRequestBuilder.addAllDocid(Arrays.asList(packageNames));

		ResponseWrapper responseWrapper = executePOSTRequest("bulkDetails", bulkDetailsRequestBuilder.build()
				.toByteArray(), "application/x-protobuf");

		return responseWrapper.getPayload().getBulkDetailsResponse();
	}

	public BrowseResponse browse() throws IOException {

		return browse(null, null);
	}

	public BrowseResponse browse(String categoryId, String subCategoryId) throws IOException {

		ResponseWrapper responseWrapper = executeGETRequest("browse", new String[][] {	{ "c", "3" },
																						{ "cat", categoryId },
																						{ "ctr", subCategoryId } });

		return responseWrapper.getPayload().getBrowseResponse();
	}

	public ListResponse list(String categoryId) throws IOException {
		return list(categoryId, null, null, null);
	}

	public ListResponse list(String categoryId, String subCategoryId, Integer offset, Integer numberOfResult)
			throws IOException {
		ResponseWrapper responseWrapper = executeGETRequest("list",
				new String[][] { { "c", "3" },
								{ "cat", categoryId },
								{ "ctr", subCategoryId },
								{ "o", (offset == null) ? null : String.valueOf(offset) },
								{ "n", (numberOfResult == null) ? null : String.valueOf(numberOfResult) }, });

		return responseWrapper.getPayload().getListResponse();
	}

	private AndroidCheckinResponse checkin(byte[] request) throws IOException {

		HttpEntity httpEntity = executePost("https://android.clients.google.com/checkin", new ByteArrayEntity(request),
				new String[][] { { "User-Agent", "Android-Checkin/2.0 (generic JRO03E); gzip" },
								{ "Host", "android.clients.google.com" },
								{ "Content-Type", "application/x-protobuffer" } });
		return AndroidCheckinResponse.parseFrom(httpEntity.getContent());
	}

	private BuyResponse purchase(String packageName, int versionCode, int offerType) throws IOException {

		ResponseWrapper responseWrapper = executePOSTRequest(
				"purchase",
				new String[][] { { "ot", String.valueOf(offerType) },
								{ "doc", packageName },
								{ "vc", String.valueOf(versionCode) }, });

		return responseWrapper.getPayload().getBuyResponse();
	}

	public InputStream download(String packageName, int versionCode, int offerType) throws IOException {

		BuyResponse buyResponse = purchase(packageName, versionCode, offerType);

		AndroidAppDeliveryData appDeliveryData = buyResponse.getPurchaseStatusResponse().getAppDeliveryData();

		String downloadUrl = appDeliveryData.getDownloadUrl();
		HttpCookie downloadAuthCookie = appDeliveryData.getDownloadAuthCookie(0);

		return executeDownload(downloadUrl, downloadAuthCookie.getName() + "=" + downloadAuthCookie.getValue());

	}

	private ResponseWrapper executeGETRequest(String path, String[][] datapost) throws IOException {

		HttpEntity httpEntity = executeGet(FDFE_URL + path, datapost, getHeaderParameters(null));
		return Googleplay.ResponseWrapper.parseFrom(httpEntity.getContent());

	}

	private ResponseWrapper executePOSTRequest(String path, String[][] datapost) throws IOException {

		HttpEntity httpEntity = executePost(FDFE_URL + path, datapost, getHeaderParameters(null));
		return Googleplay.ResponseWrapper.parseFrom(httpEntity.getContent());

	}

	private ResponseWrapper executePOSTRequest(String path, byte[] datapost, String contentType) throws IOException {

		HttpEntity httpEntity = executePost(FDFE_URL + path, new ByteArrayEntity(datapost),
				getHeaderParameters(contentType));
		return Googleplay.ResponseWrapper.parseFrom(httpEntity.getContent());

	}

	private String[][] getHeaderParameters(String contentType) {

		return new String[][] { { "Accept-Language", "en-EN" },
								{ "Authorization", "GoogleLogin auth=" + this.authSubToken },
								{ "X-DFE-Enabled-Experiments", "cl:billing.select_add_instrument_by_default" },
								{	"X-DFE-Unsupported-Experiments",
									"nocache:billing.use_charging_poller,market_emails,buyer_currency,prod_baseline,checkin.set_asset_paid_app_field,shekel_test,content_ratings,buyer_currency_in_app,nocache:encrypted_apk,recent_changes" },
								{ "X-DFE-Device-Id", this.getAndroidId() },
								{ "X-DFE-Client-Id", "am-android-google" },
								{	"User-Agent",
									"Android-Finsky/3.7.13 (api=3,versionCode=8013013,sdk=16,device=crespo,hardware=herring,product=soju)" },
								{ "X-DFE-SmallestScreenWidthDp", "320" },
								{ "X-DFE-Filter-Level", "3" },
								{ "Host", "android.clients.google.com" },
								{	"Content-Type",
									(contentType != null) ? contentType
											: "application/x-www-form-urlencoded; charset=UTF-8" } };
	}

	private InputStream executeDownload(String url, String cookie) throws IOException {

		String[][] headerParams = new String[][] {	{ "Cookie", cookie },
													{	"User-Agent",
														"AndroidDownloadManager/4.1.1 (Linux; U; Android 4.1.1; Nexus S Build/JRO03E)" }, };

		HttpEntity httpEntity = executeGet(url, null, headerParams);
		return httpEntity.getContent();

	}

}
