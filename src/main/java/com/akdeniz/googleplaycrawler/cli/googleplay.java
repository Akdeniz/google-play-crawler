package com.akdeniz.googleplaycrawler.cli;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import com.akdeniz.googleplaycrawler.GooglePlayAPI;
import com.akdeniz.googleplaycrawler.GooglePlayAPI.RECOMMENDATION_TYPE;
import com.akdeniz.googleplaycrawler.GooglePlayAPI.REVIEW_SORT;
import com.akdeniz.googleplaycrawler.GooglePlayException;
import com.akdeniz.googleplaycrawler.GooglePlay.AppDetails;
import com.akdeniz.googleplaycrawler.GooglePlay.BrowseLink;
import com.akdeniz.googleplaycrawler.GooglePlay.BrowseResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.BulkDetailsEntry;
import com.akdeniz.googleplaycrawler.GooglePlay.BulkDetailsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.DetailsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.DocV2;
import com.akdeniz.googleplaycrawler.GooglePlay.GetReviewsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.ListResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.Offer;
import com.akdeniz.googleplaycrawler.GooglePlay.ReviewResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.SearchResponse;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.BindAccountResponse;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.LoginResponse;
import com.akdeniz.googleplaycrawler.gsf.packets.BindAccountRequestPacket;
import com.akdeniz.googleplaycrawler.gsf.packets.HeartBeatPacket;
import com.akdeniz.googleplaycrawler.gsf.packets.LoginRequestPacket;
import com.akdeniz.googleplaycrawler.gsf.MTalkConnector;
import com.akdeniz.googleplaycrawler.gsf.MessageFilter;
import com.akdeniz.googleplaycrawler.gsf.NotificationListener;
import com.akdeniz.googleplaycrawler.Utils;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.choice.CollectionArgumentChoice;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.FeatureControl;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

/**
 * 
 * @author akdeniz
 *
 */
public class googleplay {

    private static final String DELIMETER = ";";

    private ArgumentParser parser;
    private GooglePlayAPI service;
    private Namespace namespace;

    public static enum COMMAND {
	LIST, DOWNLOAD, CHECKIN, CATEGORIES, SEARCH, PERMISSIONS, REVIEWS, REGISTER, USEGCM, RECOMMENDATIONS
    }

    private static final String LIST_HEADER = new StringJoiner(DELIMETER).add("Title").add("Package").add("Creator")
	    .add("Price").add("Installation Size").add("Number Of Downloads").toString();
    private static final String CATEGORIES_HEADER = new StringJoiner(DELIMETER).add("ID").add("Name").toString();
    private static final String SUBCATEGORIES_HEADER = new StringJoiner(DELIMETER).add("ID").add("Title").toString();

    private static final int TIMEOUT = 10000;

    public googleplay() {
	parser = ArgumentParsers.newArgumentParser("googleplay").description("Play with Google Play API :)");

	/* =================Common Arguments============== */
	parser.addArgument("-f", "--conf")
		.nargs("?")
		.help("Configuration file to used for login! If any of androidid, email and password is supplied, it will be ignored!")
		.setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-i", "--androidid").nargs("?")
		.help("ANDROID-ID to be used! You can use \"Checkin\" mechanism, if you don't have one!")
		.setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-e", "--email").nargs("?").help("Email address to be used for login.")
		.setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-p", "--password").nargs("?").help("Password to be used for login.")
		.setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-t", "--securitytoken").nargs("?").help("Security token that was generated at checkin. It is only required for \"usegcm\" option")
	.setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-z", "--localization").nargs("?").help("Localization string that will customise fetched informations such as reviews, " +
			"descriptions,... Can be : en-EN, en-US, tr-TR, fr-FR ... (default : en-EN)").setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-a", "--host").nargs("?").help("Proxy host").setDefault(FeatureControl.SUPPRESS);
	parser.addArgument("-l", "--port").type(Integer.class).nargs("?").help("Proxy port")
		.setDefault(FeatureControl.SUPPRESS);

	Subparsers subparsers = parser.addSubparsers().description("Command to be executed.");

	/* =================Download Arguments============== */
	Subparser downloadParser = subparsers.addParser("download", true).description("download file(s)!")
		.setDefault("command", COMMAND.DOWNLOAD);
	downloadParser.addArgument("packagename").nargs("+").help("applications to download");

	/* =================Check-In Arguments============== */
	subparsers.addParser("checkin", true).description("checkin section!").setDefault("command", COMMAND.CHECKIN);

	/* =================List Arguments============== */
	Subparser listParser = subparsers.addParser("list", true)
		.description("Lists sub-categories and applications within them!").setDefault("command", COMMAND.LIST);
	listParser.addArgument("category").required(true).help("defines category");
	listParser.addArgument("-s", "--subcategory").required(false).help("defines sub-category");
	listParser.addArgument("-o", "--offset").type(Integer.class).required(false)
		.help("offset to define where list begins");
	listParser.addArgument("-n", "--number").type(Integer.class).required(false)
		.help("how many app will be listed");

	/* =================Categories Arguments============== */
	Subparser categoriesParser = subparsers.addParser("categories", true)
		.description("list categories for browse section").setDefault("command", COMMAND.CATEGORIES);

	/* =================Search Arguments============== */
	Subparser searchParser = subparsers.addParser("search", true).description("search for query!")
		.setDefault("command", COMMAND.SEARCH);
	searchParser.addArgument("query").help("query to be searched");
	searchParser.addArgument("-o", "--offset").type(Integer.class).required(false)
		.help("offset to define where list begins");
	searchParser.addArgument("-n", "--number").type(Integer.class).required(false)
		.help("how many app will be listed");

	/* =================Permissions Arguments============== */
	Subparser permissionsParser = subparsers.addParser("permissions", true)
		.description("list permissions of given application").setDefault("command", COMMAND.PERMISSIONS);
	permissionsParser.addArgument("package").nargs("+").help("applications whose permissions to be listed");

	/* =================Reviews Arguments============== */
	Subparser reviewsParser = subparsers.addParser("reviews", true)
		.description("lists reviews of given application").setDefault("command", COMMAND.REVIEWS);
	reviewsParser.addArgument("package").help("application whose reviews to be listed");
	reviewsParser.addArgument("-s", "--sort").choices(new ReviewSortChoice()).type(new ReviewSort())
		.required(false).help("sorting type").setDefault(REVIEW_SORT.HELPFUL);
	reviewsParser.addArgument("-o", "--offset").type(Integer.class).required(false)
		.help("offset to define where list begins");
	reviewsParser.addArgument("-n", "--number").type(Integer.class).required(false)
		.help("how many reviews will be listed");
	
	/* =================Recommendation Arguments============== */
	Subparser recommendationParser = subparsers.addParser("recommendations", true)
		.description("lists recommended apps of given application").setDefault("command", COMMAND.RECOMMENDATIONS);
	recommendationParser.addArgument("package").help("application whose recommendations to be listed");
	recommendationParser.addArgument("-t", "--type").choices(new ReleationChoice()).type(new RecommendationType())
		.required(false).help("releations type").setDefault(RECOMMENDATION_TYPE.ALSO_INSTALLED);
	recommendationParser.addArgument("-o", "--offset").type(Integer.class).required(false)
		.help("offset to define where list begins");
	recommendationParser.addArgument("-n", "--number").type(Integer.class).required(false)
		.help("how many recommendations will be listed");
	
	/* =================Register Arguments============== */
	subparsers.addParser("register", true).description("registers device so that can be seen from web!")
		.setDefault("command", COMMAND.REGISTER);
	
	/* =================UseGCM Arguments============== */
	subparsers.addParser("usegcm", true).description("listens GCM(GoogleCloudMessaging) for download notification and downloads them!")
		.setDefault("command", COMMAND.USEGCM);
    }

    public static void main(String[] args) throws Exception {

	new googleplay().operate(args);
    }

    public void operate(String[] argv) {
	try {
	    namespace = parser.parseArgs(argv);
	} catch (ArgumentParserException e) {
	    System.err.println(e.getMessage());
	    parser.printHelp();
	    System.exit(-1);
	}

	COMMAND command = (COMMAND) namespace.get("command");

	try {
	    switch (command) {
	    case CHECKIN:
		checkinCommand();
		break;
	    case DOWNLOAD:
		downloadCommand();
		break;
	    case LIST:
		listCommand();
		break;
	    case CATEGORIES:
		categoriesCommand();
		break;
	    case SEARCH:
		searchCommand();
		break;
	    case PERMISSIONS:
		permissionsCommand();
		break;
	    case REVIEWS:
		reviewsCommand();
		break;
	    case REGISTER:
		registerCommand();
		break;
	    case USEGCM:
		useGCMCommand();
		break;
	    case RECOMMENDATIONS:
	    recommendationsCommand();
		break;
	    }
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    System.exit(-1);
	}
    }
    
    private void useGCMCommand() throws Exception {
	String ac2dmAuth = loginAC2DM();
	
	MTalkConnector connector = new MTalkConnector(new NotificationListener(service));
	ConnectFuture connectFuture = connector.connect();
	connectFuture.await(TIMEOUT);
	if (!connectFuture.isConnected()) {
	    throw new IOException("Couldn't connect to GTALK server!");
	}

	final IoSession session = connectFuture.getSession();
	send(session, IoBuffer.wrap(new byte[] { 0x07 })); // connection sanity check
	System.out.println("Connected to server.");

	String deviceIDStr = String.valueOf(new BigInteger(service.getAndroidID(), 16).longValue());
	String securityTokenStr = String.valueOf(new BigInteger(service.getSecurityToken(), 16).longValue());
	
	LoginRequestPacket loginRequestPacket = new LoginRequestPacket(deviceIDStr, securityTokenStr, service.getAndroidID());
	
	LoginResponseFilter loginResponseFilter = new LoginResponseFilter(loginRequestPacket.getPacketID());
	connector.addFilter(loginResponseFilter);
	send(session, loginRequestPacket);
	LoginResponse loginResponse = loginResponseFilter.nextMessage(TIMEOUT);
	connector.removeFilter(loginResponseFilter);
	if(loginResponse==null){
	    throw new IllegalStateException("Login response could not be received!");
	} else if(loginResponse.hasError()){
	    throw new IllegalStateException(loginResponse.getError().getExtension(0).getMessage());
	}
	System.out.println("Autheticated.");

	BindAccountRequestPacket bindAccountRequestPacket = new BindAccountRequestPacket(service.getEmail(), ac2dmAuth);
	
	BindAccountResponseFilter barf = new BindAccountResponseFilter(bindAccountRequestPacket.getPacketID());
	connector.addFilter(barf);
	send(session, bindAccountRequestPacket);
	BindAccountResponse bindAccountResponse = barf.nextMessage(TIMEOUT);
	connector.removeFilter(barf);
	
	/*if(bindAccountResponse==null){
	    throw new IllegalStateException("Account bind response could not be received!");
	} else if(bindAccountResponse.hasError()){
	    throw new IllegalStateException(bindAccountResponse.getError().getExtension(0).getMessage());
	}*/

	System.out.println("Listening for notifications from server..");
	
	// send heart beat packets to keep connection up.
	while (true) {
	    send(session, new HeartBeatPacket());
	    Thread.sleep(30000);
	}
    }
    
    private static void send(IoSession session, Object object) throws InterruptedException, IOException {
	WriteFuture writeFuture = session.write(object);
	writeFuture.await(TIMEOUT);
	if (!writeFuture.isWritten()) {
	    Throwable exception = writeFuture.getException();
	    if(exception!=null){
		throw new IOException("Error occured while writing!", exception);
	    }
	    throw new IOException("Error occured while writing!");
	}
    }
    
    private void recommendationsCommand() throws Exception {
	login();

	String packageName = namespace.getString("package");
	RECOMMENDATION_TYPE type = (RECOMMENDATION_TYPE) namespace.get("type");
	Integer offset = namespace.getInt("offset");
	Integer number = namespace.getInt("number");

	ListResponse recommendations = service.recommendations(packageName, type, offset, number);
	
	if (recommendations.getDoc(0).getChildCount() == 0) {
	    System.out.println("No recommendation found!");
	} else {
		for (DocV2 child : recommendations.getDoc(0).getChildList()) {
			System.out.println(child.getDetails().getAppDetails().getPackageName());
		}
	}
    }

    private void reviewsCommand() throws Exception {
	login();

	String packageName = namespace.getString("package");
	REVIEW_SORT sort = (REVIEW_SORT) namespace.get("sort");
	Integer offset = namespace.getInt("offset");
	Integer number = namespace.getInt("number");

	ReviewResponse reviews = service.reviews(packageName, sort, offset, number);
	GetReviewsResponse response = reviews.getGetResponse();
	if (response.getReviewCount() == 0) {
	    System.out.println("No review found!");
	}
	System.out.println(response);
    }
    
    private void registerCommand() throws Exception {
	login();
	service.uploadDeviceConfig();
	System.out.println("A device is registered to your account! You can see it at \"https://play.google.com/store/account\" after a few downloads!");
    }

    private void permissionsCommand() throws Exception {
	login();

	List<String> packages = namespace.getList("package");
	BulkDetailsResponse bulkDetails = service.bulkDetails(packages);

	for (BulkDetailsEntry bulkDetailsEntry : bulkDetails.getEntryList()) {
	    DocV2 doc = bulkDetailsEntry.getDoc();
	    AppDetails appDetails = doc.getDetails().getAppDetails();
	    System.out.println(doc.getDocid());
	    for (String permission : appDetails.getPermissionList()) {
		System.out.println("\t" + permission);
	    }
	}

    }

    private void searchCommand() throws Exception {
	login();

	String query = namespace.getString("query");
	Integer offset = namespace.getInt("offset");
	Integer number = namespace.getInt("number");

	SearchResponse searchResponse = service.search(query, offset, number);
	System.out.println(LIST_HEADER);
	for (DocV2 child : searchResponse.getDoc(0).getChildList()) {
	    AppDetails appDetails = child.getDetails().getAppDetails();
	    String formatted = new StringJoiner(DELIMETER).add(child.getTitle()).add(appDetails.getPackageName())
		    .add(child.getCreator()).add(child.getOffer(0).getFormattedAmount())
		    .add(String.valueOf(appDetails.getInstallationSize())).add(appDetails.getNumDownloads()).toString();
	    System.out.println(formatted);

	}
    }

    private void categoriesCommand() throws Exception {
	login();
	BrowseResponse browseResponse = service.browse();
	System.out.println(CATEGORIES_HEADER);
	for (BrowseLink browseLink : browseResponse.getCategoryList()) {
	    String[] splitedStrs = browseLink.getDataUrl().split("&cat=");
	    System.out.println(new StringJoiner(DELIMETER).add(splitedStrs[splitedStrs.length - 1])
		    .add(browseLink.getName()).toString());
	}
    }

    private void checkinCommand() throws Exception {
	checkin();

	System.out.println("Your account succesfully checkined!");
	System.out.println("AndroidID : " + service.getAndroidID());
	System.out.println("SecurityToken : " + service.getSecurityToken());
    }

    private void login() throws Exception {
	String androidid = namespace.getString("androidid");
	String email = namespace.getString("email");
	String password = namespace.getString("password");
	String localization = namespace.getString("localization");

	if (androidid != null && email != null && password != null) {
	    createLoginableService(androidid, email, password, localization);
	    service.login();
	    return;
	}

	if (namespace.getAttrs().containsKey("conf")) {
	    Properties properties = new Properties();
	    properties.load(new FileInputStream(namespace.getString("conf")));

	    androidid = properties.getProperty("androidid");
	    email = properties.getProperty("email");
	    password = properties.getProperty("password");
	    localization = properties.getProperty("localization");

	    if (androidid != null && email != null && password != null) {
		createLoginableService(androidid, email, password, localization);
		service.login();
		return;
	    }
	}

	throw new GooglePlayException("Lack of information for login!");
    }
    
    private String loginAC2DM() throws Exception {
	String androidid = namespace.getString("androidid");
	String email = namespace.getString("email");
	String password = namespace.getString("password");
	String securityToken = namespace.getString("securitytoken");
	String localization = namespace.getString("localization");

	if (androidid != null && email != null && password != null && securityToken!=null) {
	    createLoginableService(androidid, email, password, localization);
	    service.login();
	    service.setSecurityToken(securityToken);
	    return service.loginAC2DM();
	}

	if (namespace.getAttrs().containsKey("conf")) {
	    Properties properties = new Properties();
	    properties.load(new FileInputStream(namespace.getString("conf")));

	    androidid = properties.getProperty("androidid");
	    email = properties.getProperty("email");
	    password = properties.getProperty("password");
	    securityToken = properties.getProperty("securitytoken");
	    localization = properties.getProperty("localization");

	    if (androidid != null && email != null && password != null && securityToken!=null) {
		createLoginableService(androidid, email, password, localization);
		service.login();
		service.setSecurityToken(securityToken);
		return service.loginAC2DM();
	    }
	}

	throw new GooglePlayException("Lack of information for login!");
    }

    private void createLoginableService(String androidid, String email, String password, String localization) throws Exception {
	service = new GooglePlayAPI(email, password, androidid);
	service.setLocalization(localization);
	HttpClient proxiedHttpClient = getProxiedHttpClient();
	if (proxiedHttpClient != null) {
	    service.setClient(proxiedHttpClient);
	}
    }

    private void createCheckinableService(String email, String password, String localization) throws Exception {
	service = new GooglePlayAPI(email, password);
	service.setLocalization(localization);
	HttpClient proxiedHttpClient = getProxiedHttpClient();
	if (proxiedHttpClient != null) {
	    service.setClient(proxiedHttpClient);
	}
    }

    private void listCommand() throws Exception {
	login();

	String category = namespace.getString("category");
	String subcategory = namespace.getString("subcategory");
	Integer offset = namespace.getInt("offset");
	Integer number = namespace.getInt("number");

	ListResponse listResponse = service.list(category, subcategory, offset, number);
	if (subcategory == null) {
	    System.out.println(SUBCATEGORIES_HEADER);
	    for (DocV2 child : listResponse.getDocList()) {
		String formatted = new StringJoiner(DELIMETER).add(child.getDocid()).add(child.getTitle()).toString();
		System.out.println(formatted);
	    }
	} else {
	    System.out.println(LIST_HEADER);
	    for (DocV2 child : listResponse.getDoc(0).getChildList()) {
		AppDetails appDetails = child.getDetails().getAppDetails();
		String formatted = new StringJoiner(DELIMETER).add(child.getTitle()).add(appDetails.getPackageName())
			.add(child.getCreator()).add(child.getOffer(0).getFormattedAmount())
			.add(String.valueOf(appDetails.getInstallationSize())).add(appDetails.getNumDownloads())
			.toString();
		System.out.println(formatted);

	    }
	}
    }

    private void downloadCommand() throws Exception {
	login();
	List<String> packageNames = namespace.getList("packagename");
	for (String packageName : packageNames) {
	    download(packageName);
	}
    }

    private void checkin() throws Exception {
	String email = namespace.getString("email");
	String password = namespace.getString("password");
	String localization = namespace.getString("localization");

	if (email != null && password != null) {
	    createCheckinableService(email, password, localization);
	    service.checkin();
	    return;
	}

	if (namespace.getAttrs().containsKey("conf")) {
	    Properties properties = new Properties();
	    properties.load(new FileInputStream(namespace.getString("conf")));

	    email = properties.getProperty("email");
	    password = properties.getProperty("password");
	    localization = properties.getProperty("localization");

	    if (email != null && password != null) {
		createCheckinableService(email, password, localization);
		service.checkin();
		return;
	    }
	}

	throw new GooglePlayException("Lack of information for login!");
    }

    private HttpClient getProxiedHttpClient() throws Exception {
	String host = namespace.getString("host");
	Integer port = namespace.getInt("port");

	if (host != null && port != null) {
	    return getProxiedHttpClient(host, port);
	}

	if (namespace.getAttrs().containsKey("conf")) {
	    Properties properties = new Properties();
	    properties.load(new FileInputStream(namespace.getString("conf")));

	    host = properties.getProperty("host");
	    String portString = properties.getProperty("port");

	    if (host != null && portString != null) {
		port = Integer.valueOf(portString);
		return getProxiedHttpClient(host, port);
	    }
	}

	return null;
    }

    private static HttpClient getProxiedHttpClient(String host, Integer port) throws Exception {
	HttpClient client = new DefaultHttpClient(GooglePlayAPI.getConnectionManager());
	client.getConnectionManager().getSchemeRegistry().register(Utils.getMockedScheme());
	HttpHost proxy = new HttpHost(host, port);
	client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	return client;
    }

    private void download(String packageName) throws IOException {
	DetailsResponse details = service.details(packageName);
	AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
	Offer offer = details.getDocV2().getOffer(0);

	int versionCode = appDetails.getVersionCode();
	long installationSize = appDetails.getInstallationSize();
	int offerType = offer.getOfferType();
	boolean checkoutRequired = offer.getCheckoutFlowRequired();

	System.out.println("Downloading..." + appDetails.getPackageName() + " : " + installationSize + " bytes");
	InputStream downloadStream;

	// paid application...ignore
	if (checkoutRequired) {
	    System.out.println("Checkout required! Assuming you have already purchased it, use the delivery flow for " + appDetails.getPackageName());
		downloadStream = service.delivery(appDetails.getPackageName(), versionCode, offerType);
	} else {
	    downloadStream = service.download(appDetails.getPackageName(), versionCode, offerType);
	}

	FileOutputStream outputStream = new FileOutputStream(appDetails.getPackageName() + ".apk");

	byte buffer[] = new byte[1024];
	for (int k = 0; (k = downloadStream.read(buffer)) != -1;) {
	    outputStream.write(buffer, 0, k);
	}
	downloadStream.close();
	outputStream.close();
	System.out.println("Downloaded! " + appDetails.getPackageName() + ".apk");
    }

}

class ReviewSort implements ArgumentType<Object> {

    @Override
    public Object convert(ArgumentParser parser, Argument arg, String value) throws ArgumentParserException {
	try {
	    return REVIEW_SORT.valueOf(value);
	} catch (IllegalArgumentException ex) {
	    return value;
	}
    }
}

class ReviewSortChoice extends CollectionArgumentChoice<REVIEW_SORT> {

    public ReviewSortChoice() {
	super(REVIEW_SORT.NEWEST, REVIEW_SORT.HIGHRATING, REVIEW_SORT.HELPFUL);
    }

    @Override
    public boolean contains(Object val) {
	try {
	    return super.contains(val);
	} catch (IllegalArgumentException ex) {
	    return false;
	}
    }
}

class RecommendationType implements ArgumentType<Object> {

    @Override
    public Object convert(ArgumentParser parser, Argument arg, String value) throws ArgumentParserException {
	try {
	    return RECOMMENDATION_TYPE.valueOf(value);
	} catch (IllegalArgumentException ex) {
	    return value;
	}
    }
}

class ReleationChoice extends CollectionArgumentChoice<RECOMMENDATION_TYPE> {

    public ReleationChoice() {
	super(RECOMMENDATION_TYPE.ALSO_VIEWED, RECOMMENDATION_TYPE.ALSO_INSTALLED);
    }

    @Override
    public boolean contains(Object val) {
	try {
	    return super.contains(val);
	} catch (IllegalArgumentException ex) {
	    return false;
	}
    }
}

class StringJoiner {
    private String delimeter;
    List<String> elements = new ArrayList<String>();

    public StringJoiner(String delimeter) {
	this.delimeter = delimeter;
    }

    public StringJoiner add(String elem) {
	elements.add(elem);
	return this;
    }

    @Override
    public String toString() {
	if (elements.isEmpty())
	    return "";
	Iterator<String> iter = elements.iterator();
	StringBuilder builder = new StringBuilder(iter.next());
	while (iter.hasNext()) {
	    builder.append(delimeter).append(iter.next());
	}
	return builder.toString();
    }
}

class LoginResponseFilter extends MessageFilter<LoginResponse>{
    private String id;

    public LoginResponseFilter(String id) {
	super(LoginResponse.class);
	this.id = id;
    }
    
    @Override
    protected boolean accept(LoginResponse message) {
	return id.equals(message.getPacketid());
    }
}

class BindAccountResponseFilter extends MessageFilter<BindAccountResponse>{
    private String id;

    public BindAccountResponseFilter(String id) {
	super(BindAccountResponse.class);
	this.id = id;
    }
    
    @Override
    protected boolean accept(BindAccountResponse message) {
	return id.equals(message.getPacketid());
    }
}
