package com.akdeniz.googleplaycrawler.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.akdeniz.googleplaycrawler.GooglePlayAPI;
import com.akdeniz.googleplaycrawler.Googleplay.AppDetails;
import com.akdeniz.googleplaycrawler.Googleplay.BrowseLink;
import com.akdeniz.googleplaycrawler.Googleplay.BrowseResponse;
import com.akdeniz.googleplaycrawler.Googleplay.BulkDetailsEntry;
import com.akdeniz.googleplaycrawler.Googleplay.BulkDetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.DetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.DocV2;
import com.akdeniz.googleplaycrawler.Googleplay.ListResponse;
import com.akdeniz.googleplaycrawler.Googleplay.Offer;
import com.akdeniz.googleplaycrawler.Googleplay.SearchResponse;

/**
 * 
 * @author akdeniz
 * 
 */
public class TestGooglePlay {

	// your device id, which can be optained from Gtalk Service Monitor(aid key)
	// by "*#*#8255#*#*" combination
	public static String ANDROID_ID = "1234567890123456";
	public static String GOOGLE_LOGIN = "";
	public static String GOOGLE_PASSWORD = "";

	private static GooglePlayAPI service = new GooglePlayAPI(ANDROID_ID);

	public static void main(String[] args) throws IOException, Exception {

		// one can set authSubToken not to call login at evert turn
		// service.setAuthSubToken("here-comes-auth-string");
		service.login(GOOGLE_LOGIN, GOOGLE_PASSWORD);

		testBrowse();
		testBrowseSubCategories();
		testList();
		testListSubCategories();
		testSearch();
		testDetails();
		testPermissions();
		testBulkDetails();
		testDownload();
		
		System.out.println("Tests passed succesfully!");

	}

	static void testBrowse() throws IOException {
		BrowseResponse browseResponse = service.browse();
		for (BrowseLink browseLink : browseResponse.getCategoryList()) {
			String[] splitedStrs = browseLink.getDataUrl().split("&cat=");
			System.out.println(splitedStrs[splitedStrs.length - 1]);
		}
	}

	static void testBrowseSubCategories() throws IOException {
		BrowseResponse browseResponse = service.browse("GAME", "SPORTS_GAMES");
		for (BrowseLink browseLink : browseResponse.getCategoryList()) {
			String[] splitedStrs = browseLink.getDataUrl().split("&cat=");
			System.out.println(splitedStrs[splitedStrs.length - 1]);
		}
	}

	static void testList() throws IOException {
		ListResponse listResponse = service.list("GAME", null, null, null);
		for (DocV2 child : listResponse.getDocList()) {
			System.out.println(child.getDocid());
		}
	}

	static void testListSubCategories() throws IOException {
		ListResponse listResponse = service.list("GAME", "apps_topselling_free", 6, 36);
		for (DocV2 child : listResponse.getDoc(0).getChildList()) {
			System.out.println(child.getBackendDocid());
		}
	}

	static void testSearch() throws IOException {
		SearchResponse searchResponse = service.search("criticker");
		for (DocV2 child : searchResponse.getDoc(0).getChildList()) {
			System.out.println(child.getBackendDocid());
		}
	}

	static void testDetails() throws IOException {
		DetailsResponse detailsResponse = service.details("com.mobulasoft.criticker");
		System.out.println(detailsResponse);
	}

	private static void testPermissions() throws IOException {
		DetailsResponse details = service.details("com.mobulasoft.criticker");
		AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
 
		for (String permission : appDetails.getPermissionList()) {
			System.out.println(permission);
		}
	}

	private static void testBulkDetails() throws IOException {
		BulkDetailsResponse bulkDetails = service.bulkDetails(new String[] { "com.mobulasoft.criticker",
																			"com.cricbuzz.android",
																			"com.sticksports.stickcricket",
																			"com.indiagames.cricketfever", });

		for (BulkDetailsEntry bulkDetailsEntry : bulkDetails.getEntryList()) {
			DocV2 doc = bulkDetailsEntry.getDoc();
			AppDetails appDetails = doc.getDetails().getAppDetails();
			System.out.println(doc.getDocid());
			for (String permission : appDetails.getPermissionList()) {
				System.out.println("\t"+ permission);
			}
		}

	}

	static void testDownload() throws IOException {
		DetailsResponse details = service.details("com.mobulasoft.criticker");
		AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
		Offer offer = details.getDocV2().getOffer(0);

		int versionCode = appDetails.getVersionCode();
		long installationSize = appDetails.getInstallationSize();
		int offerType = offer.getOfferType();
		boolean checkoutRequired = offer.getCheckoutFlowRequired();

		// paid application...ignore
		if (checkoutRequired) {
			System.out.println("Checkout required! Ignoring.." + appDetails.getPackageName());
			return;
		}

		System.out.println("Downloading..." + appDetails.getPackageName() + " : " + installationSize + " bytes");
		InputStream downloadStream = service.download(appDetails.getPackageName(), versionCode, offerType);

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
