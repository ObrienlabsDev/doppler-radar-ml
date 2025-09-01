package dev.obrienlabs.weather.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;

/**
 * https://github.com/ObrienlabsDev/doppler-radar-ml/issues/7
 */
public class EccCapture {
	
	public static final String BASE_URL = "https://dd.weather.gc.ca/";
	public static final String TARGET_DIR = "/Users/michaelobrien/_download/cappi/";
	private static final String USER_AGENT = "michael-at-obrienlabs-dev/0.9 (+java.net.http)";
	//https://console.cloud.google.com/storage/overview;tab=overview?hl=en&project=doppler-radar-old
	public static final String CLOUD_STORAGE_URL = "";
	public static final String GCS_BUCKET_NAME = "doppler1_old";
	
	// https://dd.weather.gc.ca/radar/CAPPI/GIF/CASFT/202508311230_CASFT_CAPPI_1.5_RAIN.gif
	// https://dd.weather.gc.ca/radar/DPQPE/GIF/CASFT/20250831T1230Z_MSC_Radar-DPQPE_CASFT_Rain.gif
	public static final String[] CAPPI_DPQPE_L3_ID = { "CAPPI", "DPQPE" };
	// 30
	public static final String[] SITE_L2_ID = { "FT","AG","BI","BV","CL","CM","CV","DR","ET","FM","GO","HP","HR","KR","LA","MA","MB","MM","MR","PG","RA","RF","SF","SM","SN","SR","SS","SU","VD","WL" };
	public static final DateTimeFormatter DATE_TIME_FORMATTER_CAPPI = DateTimeFormatter.ofPattern("yyyyMMddHH");
	public static final DateTimeFormatter DATE_TIME_FORMATTER_DPQPE = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final Random RANDOM = new Random();
	private static final long MIN_RANDOM = 5230L;
	private static final long MAX_RANDOM = 5200L;
	private static final int RADAR_MIN_RESOLUTION = 6;
	private static final int RADAR_MIN_POST_UPLOAD_TIME_MIN = 1; // the time between current and last image upload
		
    private static final Logger logger = Logger.getLogger(EccCapture.class.getName());
    private final Storage storage;
    
    // add map to track current interval of 30 images
	
    public EccCapture() {
    	// authentication will be handled by ENV variables starting with GOOGLE_APPLICATION_CREDENTIALS
    	// restart eclipse after running
    	// gcloud auth application-default login
    	// gcloud config set project doppler-radar-old 
    	// gcloud auth application-default set-quota-project doppler-radar-old
    	// gcloud services enable cloudbilling.googleapis.com
    	// PROJ=$(gcloud config list --format 'value(core.project)') 
    	// export ORGANIZATION_ID=$(gcloud projects get-ancestors $PROJECT_ID --format='get(id)' | tail -1)
    	// export PROJECT_ID=$(gcloud config list --format 'value(core.project)') 
    	// export ORGANIZATION_ID=$(gcloud projects get-ancestors $PROJECT_ID --format='get(id)' | tail -1)
    	// export BILLING_ID=$(gcloud alpha billing projects describe $PROJECT_ID '--format=value(billingAccountName)' | sed 's/.*\///')
    	// USER_EMAIL=`gcloud config list account --format "value(core.account)"`
    	this.storage = StorageOptions.getDefaultInstance().getService();
    }
    
	// poc - pull from today directory once
	public void capture() {// throws IOException, InterruptedException {
		//createGCSBucket(GCS_BUCKET_NAME);
		for(;;) {
			// add wait until 1 min after - NEED TO COMPLETE IN 4 min after possible 2 min late start
			waitForSixMinuteTrailingOffsetInterval();
			for(int site=0; site<30; site++) {
				int cappiDpqpeFlag = 0;
				try {
					captureImage(SITE_L2_ID[site].toLowerCase(), BASE_URL + computePostfixUrl(site, cappiDpqpeFlag));
				} catch (Exception e) {
					// particular radar image n/a - skip
					System.out.println(e);
					System.out.println("Skipping: " + SITE_L2_ID[site]);
				}
				//try { // check 5 min + 1 min wait - crosses 6 - image not ready, wait 6, skipped image
				//	Thread.sleep(60000 * 5); // waiting 6 min may miss every 6th image
				//} catch (Exception e) {
				//}
			}
		}
	}
	
	private String computePostfixUrl(int siteID, int cappiID) {
		StringBuffer buffer = new StringBuffer();
		// GMT-4 check DST - align to 00+6min intervals
		LocalDateTime offsetTime = LocalDateTime.now().minusMinutes(0).plusHours(4);
		String formattedDateTime = offsetTime.format(DATE_TIME_FORMATTER_CAPPI);
		String urlPostfix = buffer.append("today/radar/")
				.append(CAPPI_DPQPE_L3_ID[cappiID])
				.append("/GIF/")
				.append("CAS")
				.append(SITE_L2_ID[siteID])
				.append("/")
				.append(formattedDateTime)
				.append(getSixMinuteTrailingOffsetMinute(offsetTime.getMinute()))
				.append("_")
				.append("CAS")
				.append(SITE_L2_ID[siteID])
				.append("_")
				.append(CAPPI_DPQPE_L3_ID[cappiID])
				.append("_")
				.append("1.5_RAIN.gif")
				.toString();
		return urlPostfix;
	}
	
	/**
	 * Compute the 6 min trailing offset (0,6,12,18,24,30,36,42,48,54) from the current minute.
	 * Return as padded 06 for minute 9 for example
	 * @param minute
	 * @return
	 */
	private String getSixMinuteTrailingOffsetMinute(int minute) {
		int _trailingMinute = (minute / RADAR_MIN_RESOLUTION) * RADAR_MIN_RESOLUTION;
		if(minute - _trailingMinute < RADAR_MIN_POST_UPLOAD_TIME_MIN) {
	    	try {
	    		System.out.print(" sleep 60s ");
	    		Thread.sleep(60000);
	    	} catch (Exception e) {
	    	}
		}
		// convert to String with prefix 0
		String postfix = Integer.toString(_trailingMinute);
		return postfix.length() > 1 ? postfix : "0" + postfix;
	}
	
	private void waitForSixMinuteTrailingOffsetInterval() {
		LocalDateTime offsetTime;
		for (;;) {
			offsetTime = LocalDateTime.now().minusMinutes(0).plusHours(4);
			int _trailingMinute = (offsetTime.getMinute() / RADAR_MIN_RESOLUTION) * RADAR_MIN_RESOLUTION;
		
			if(offsetTime.getMinute() - _trailingMinute > RADAR_MIN_POST_UPLOAD_TIME_MIN) {
				try {
					System.out.println(" wait 30s for 6 min interval start ");
					Thread.sleep(30000);
				} catch (Exception e) {
				}
			} else {
				return;
			}
		}
		
	}
    
    public void uploadImage() {
    	
    }
    
    private long random10secDelay() {
    	long sec = MIN_RANDOM + RANDOM.nextLong(MAX_RANDOM);
    	System.out.print(String.format("wait %d ms - ", sec));
    	try {
    		Thread.sleep(sec);
    	} catch (Exception e) {
    	}
        return sec;
    }
    
    public void captureImage(String site, String fullUrl) throws IOException, InterruptedException {
    	Path target = Path.of(TARGET_DIR + site, Path.of(URI.create(fullUrl).getPath()).getFileName().toString());
    	// check target already exists - exit if
    	random10secDelay();
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(fullUrl))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " while downloading " + fullUrl);
        }

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
		System.out.println(" Captured: " + fullUrl);
    }

    
    public void downloadAllImagesFromFolder(String folderName) {
    	
    }
	
	// setup HttpClient
	
	// compute url (don't parse the directory)
	
	// schedule for every 6 min with offset 2 min from 00
	
	// iterate across all radar sites with 5-10 sec random delay - don't overload and don't look like a robot
	
	// save images in mirror subfolders
	

	/**
	 * Create bucket if not already existing
	 * @param bucketName
	 * @return
	 */
    public Bucket createGCSBucket(String bucketName) {
        try {
        	// check project id first
            Bucket existingBucket = storage.get(bucketName);
            if (existingBucket != null) {
                logger.log(Level.INFO, "Bucket {0} already exists", bucketName);
                return existingBucket;
            }

            BucketInfo bucketInfo = BucketInfo.newBuilder(bucketName)
                    .setLocation("northamerica-northeast1")
                    .setStorageClass(StorageClass.STANDARD)
                    .build();

            Bucket newBucket = storage.create(bucketInfo);
            logger.log(Level.INFO, "Bucket {0} created successfully", bucketName);
            return newBucket;

        } catch (com.google.cloud.storage.StorageException e) {
            logger.log(Level.SEVERE, "Error creating bucket " + bucketName + ": " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error bucket " + bucketName + ": " + e.getMessage(), e);
            return null;
        }
    }
	
/**
 *   As of 20250821 the ECC site has added an easier way to capture live radar data.  Instead of parsing the consumer site we can go directly to the ecc server and it's secondary. 
  These exist on the following sites - see https://github.com/ObrienlabsDev/doppler-radar-ml/issues/7
  
  Analyse
  
https://eccc-msc.github.io/open-data/readme_en/

https://eccc-msc.github.io/open-data/msc-datamart/readme_en/

primary

https://dd.weather.gc.ca/today/

10x speed (auth)

https://hpfx.collab.science.gc.ca/

  We will create a scheduled service to compute the date for the today folder, for example

```  
https://dd.weather.gc.ca/today/radar/CAPPI/GIF/CASFT/202508211800_CASFT_CAPPI_1.5_RAIN.gif 
  

computed as BASE_URL https://dd.weather.gc.ca/today/radar

DBQPE OR CAPPI
20250829T0000Z_MSC_Radar-DPQPE_CASFT_Rain.gif

GIF

site = CASFT (franktown - the older xft)

date = 20250821

time = 1800 (intervals are 6 min from 00)

site = as above = CASFT

type = 1.5

precif = RAIN

.gif

```
 * @param argv
 */
	public static void main(String[] argv) {
	
		EccCapture eccCapture = new EccCapture();
		//try {
			eccCapture.capture();
		//} catch (Exception e) {
		//	System.out.println(e);
		//}
	}
}
