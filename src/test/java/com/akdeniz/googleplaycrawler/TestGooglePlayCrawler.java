package com.akdeniz.googleplaycrawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.akdeniz.googleplaycrawler.GooglePlay.AppDetails;
import com.akdeniz.googleplaycrawler.GooglePlay.BrowseResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.BulkDetailsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.DetailsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.ListResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.Offer;
import com.akdeniz.googleplaycrawler.GooglePlay.SearchResponse;

public class TestGooglePlayCrawler {

	private static GooglePlayAPI service;

	@BeforeClass
	public static void setup() throws IOException {

		Properties properties = new Properties();
		properties.load(new FileInputStream("./src/test/resources/login.conf"));

		String email = properties.getProperty("email");
		String password = properties.getProperty("password");

		service = new GooglePlayAPI(email, password);
	}

	@Test
	public void shouldCheckin() throws Exception {
		service.checkin();
	}

	@Test(dependsOnMethods = { "shouldCheckin" })
	public void shouldLogin() throws Exception {
		// allow server to catch up after checkin...
		Thread.sleep(5000);
		service.login();
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldUploadDeviceConfiguration() throws Exception {
		service.uploadDeviceConfig();
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldBrowse() throws Exception {
		BrowseResponse browseResponse = service.browse();
		Assert.assertFalse(browseResponse.getCategoryList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldBrowseSubCategories() throws Exception {
		BrowseResponse browseResponse = service.browse("GAME", null);
		Assert.assertFalse(browseResponse.getCategoryList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldList() throws Exception {
		ListResponse listResponse = service.list("GAME", null, null, null);
		Assert.assertFalse(listResponse.getDocList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldListSubCategories() throws Exception {
		ListResponse listResponse = service.list("GAME", "apps_topselling_free", 6, 36);
		Assert.assertFalse(listResponse.getDocList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldSearch() throws Exception {
		SearchResponse searchResponse = service.search("criticker");
		Assert.assertFalse(searchResponse.getDoc(0).getChildList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldFetchDetails() throws Exception {
		service.details("com.mobulasoft.criticker");
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldFetchPermissions() throws Exception {
		DetailsResponse details = service.details("com.mobulasoft.criticker");
		AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
		Assert.assertFalse(appDetails.getPermissionList().isEmpty());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldFetchBulkDetails() throws Exception {
		List<String> packageNames = new ArrayList<String>();
		packageNames.add("com.mobulasoft.criticker");
		packageNames.add("com.cricbuzz.android");
		packageNames.add("com.sticksports.stickcricket");
		packageNames.add("com.indiagames.cricketfever");

		BulkDetailsResponse bulkDetails = service.bulkDetails(packageNames);
		Assert.assertEquals(bulkDetails.getEntryList().size(), packageNames.size());
	}

	@Test(dependsOnMethods = { "shouldLogin" })
	public void shouldDownload() throws Exception {
		final String packageName = "com.mobulasoft.criticker";
		DetailsResponse details = service.details(packageName);
		AppDetails appDetails = details.getDocV2().getDetails().getAppDetails();
		Offer offer = details.getDocV2().getOffer(0);

		int versionCode = appDetails.getVersionCode();
		long installationSize = appDetails.getInstallationSize();
		int offerType = offer.getOfferType();
		boolean checkoutRequired = offer.getCheckoutFlowRequired();

		// paid application...ignore
		if (checkoutRequired) {
			return;
		}

		InputStream downloadStream = service.download(appDetails.getPackageName(), versionCode, offerType);
		FileOutputStream outputStream = new FileOutputStream(File.createTempFile(packageName, ".apk"));

		byte buffer[] = new byte[1024];
		for (int k = 0; (k = downloadStream.read(buffer)) != -1;) {
			outputStream.write(buffer, 0, k);
		}
		downloadStream.close();
		outputStream.close();
	}
}
