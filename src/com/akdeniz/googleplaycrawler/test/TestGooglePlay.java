package com.akdeniz.googleplaycrawler.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.akdeniz.googleplaycrawler.GooglePlayAPI;
import com.akdeniz.googleplaycrawler.Googleplay.DetailsResponse;
import com.akdeniz.googleplaycrawler.Googleplay.DocV2;
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
		
		// search
		SearchResponse search = service.search("merlin", 0, 100);

		System.out.println("#Found : " + search.getDoc(0).getChildCount());

		for (DocV2 doc : search.getDocList()) {
			for (DocV2 child : doc.getChildList()) {
				download(child.getBackendDocid());
			}
		}

	}
	
	public static void download(String packageName) throws IOException{
		
		File apkFile = new File(packageName + ".apk");
		if (apkFile.exists()) {
			return;
		}

		DetailsResponse details = service.details(packageName);
		int versionCode = details.getDocV2().getDetails().getAppDetails()
				.getVersionCode();
		int offerType = details.getDocV2().getOffer(0).getOfferType();

		long installationSize = details.getDocV2().getDetails()
				.getAppDetails().getInstallationSize();

		System.out.println("Downloading..." + apkFile.getName() + " : "
				+ installationSize + " bytes");

		InputStream downloadStream = service.download(packageName,
				versionCode, offerType);
		FileOutputStream outputStream = new FileOutputStream(apkFile);

		byte buffer[] = new byte[1024];
		int k = 0;
		for (long l = 0; (k = downloadStream.read(buffer)) != -1; l += k) {
			outputStream.write(buffer, 0, k);
		}
		downloadStream.close();
		outputStream.close();
		System.out.println("Downloaded! " + apkFile.getName());
	}

}



