package com.akdeniz.googleplaycrawler;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
 * XXX : DO NOT call checkin, login and download consecutively. To allow server to catch up, sleep for a while before download! (5 sec will do!)
 * Also it is recommended to call checkin once and use generated android-id for further operations.
 * 
 * @author akdeniz
 * 
 */
public class GooglePlayAPI {

	private static final String FDFE_URL = "https://android.clients.google.com/fdfe/";
	private static String SERVICE = "androidmarket";
	private static String URL_LOGIN = "https://android.clients.google.com/auth";
	private static String ACCOUNT_TYPE_HOSTED_OR_GOOGLE = "HOSTED_OR_GOOGLE";

	private String authSubToken;
	private String androidId;
	private String email;
	private String password;

	public GooglePlayAPI(String email, String password, String androidId) {
		this(email, password);
		this.setAndroidId(androidId);
	}

	public GooglePlayAPI(String email, String password) {
		this.email = email;
		this.password = password;
	}

	/**
	 * Performs authentication on "ac2dm" service and match up android id, security token and email by
	 * checking them in.
	 * 
	 */
	public AndroidCheckinResponse checkin() throws Exception {
		
		// this first checkin is for generating android-id
		AndroidCheckinResponse checkinResponse = postCheckin(Utils.generateAndroidCheckinRequest().toByteArray());
		this.setAndroidId(BigInteger.valueOf(checkinResponse.getAndroidId()).toString(16));
		String securityToken = (BigInteger.valueOf(checkinResponse.getSecurityToken()).toString(16));

		HttpEntity c2dmResponseEntity = executePost(URL_LOGIN, new String[][] { { "Email", this.email },
																				{ "Passwd", this.password },
																				{ "service", "ac2dm" },
																				{ "accountType",
																					ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
																				{ "has_permission", "1" },
																				{ "source", "android" },
																				{ "app", "com.google.android.gsf" },
																				{ "device_country", "us" },
																				{ "device_country", "us" },
																				{ "lang", "en" },
																				{ "sdk_version", "16" }, }, null);

		Map<String, String> c2dmAuth = Utils.parseResponse(new String(Utils.readAll(c2dmResponseEntity.getContent())));

		GoogleServicesFramework.AndroidCheckinRequest.Builder checkInbuilder = GoogleServicesFramework.AndroidCheckinRequest
				.newBuilder(Utils.generateAndroidCheckinRequest());

		AndroidCheckinRequest build = checkInbuilder.setId(new BigInteger(this.getAndroidId(), 16).longValue())
				.setSecurityToken(new BigInteger(securityToken, 16).longValue())
				.addAccountCookie("[" + email + "]").addAccountCookie(c2dmAuth.get("Auth")).build();
		// this is the second checkin to match credentials with android-id
		return postCheckin(build.toByteArray());
	}

	/**
	 * Authenticate with given email and password.
	 * 
	 * If android id is not supplied while initializing, this method will try to
	 * acquire one.
	 */
	public void login() throws Exception {

		HttpEntity responseEntity = executePost(URL_LOGIN, new String[][] { { "Email", this.email },
																			{ "Passwd", this.password },
																			{ "service", SERVICE },
																			{ "accountType", ACCOUNT_TYPE_HOSTED_OR_GOOGLE },
																			{ "has_permission", "1" },
																			{ "source", "android" },
																			{ "androidId", this.getAndroidId() },
																			{ "app", "com.android.vending" },
																			{ "device_country", "en" },
																			{ "lang", "en" },
																			{ "sdk_version", "16" }, }, null);

		Map<String, String> response = Utils.parseResponse(new String(Utils.readAll(responseEntity.getContent())));
		if (response.containsKey("Auth")) {
			setAuthSubToken(response.get("Auth"));
		} else {
			throw new GooglePlayException("Authentication failed!");
		}

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

		/*try {
			client.getConnectionManager().getSchemeRegistry().register(Utils.getMockedScheme());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// proxy TODO : disable!
		HttpHost proxy = new HttpHost("localhost", 8888);
		client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);*/

		HttpResponse response = client.execute(request);

		if (response.getStatusLine().getStatusCode() != 200) {
			throw new GooglePlayException(new String(Utils.readAll(response.getEntity().getContent())));
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

	private AndroidCheckinResponse postCheckin(byte[] request) throws IOException {

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
								{ "Authorization", "GoogleLogin auth=" + getAuthSubToken() },
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
