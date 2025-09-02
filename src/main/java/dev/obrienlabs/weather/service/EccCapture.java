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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;

/**
 * <llm>none</llm>
 * https://github.com/ObrienlabsDev/doppler-radar-ml/issues/7
 */
public class EccCapture {
	
	public static final int RADAR_SITES_COUNT = 30;
	public static final String BASE_URL = "https://dd.weather.gc.ca/";
	public static final String TARGET_DIR = "/Users/michaelobrien/_download/";
	private static final String USER_AGENT = "github-obriensystems/0.9 (+java.net.http)";
	//https://console.cloud.google.com/storage/overview;tab=overview?hl=en&project=doppler-radar-old
	public static final String CLOUD_STORAGE_URL = "";
	public static final String GCS_BUCKET_NAME = "doppler1_old";
	
	// https://dd.weather.gc.ca/radar/CAPPI/GIF/CASFT/202508311230_CASFT_CAPPI_1.5_RAIN.gif
	// https://dd.weather.gc.ca/radar/DPQPE/GIF/CASFT/20250831T1230Z_MSC_Radar-DPQPE_CASFT_Rain.gif
	public static final String[] CAPPI_DPQPE_L2_ID = { "CAPPI", "DPQPE" };
	public static final String[] CAPPI_DPQPE_L3_PRE_ID = { "", "DPQPE" };
	public static final String[] CAPPI_DPQPE_L3_POST_ID = { "CAPPI", "" };
	private static final String[] CAPPI_DPQPE_TIME_T_ID = { "", "T" };
	private static final String[] CAPPI_DPQPE_TIME_ZULU_ID = { "", "Z" };
	private static final String[] CAPPI_DPQPE_POST_TIME_CHARS = { "", "_MSC_Radar-" };
	private static final String[] CAPPI_DPQPE_END = { "_1.5_RAIN.gif", "Rain.gif" };
	// 30
	public static final String[] SITE_L2_ID = { "FT","AG","BI","BV","CL","CM","CV","DR","ET","FM","GO","HP","HR","KR","LA","MA","MB","MM","MR","PG","RA","RF","SF","SM","SN","SR","SS","SU","VD","WL" };
	public static final List<DateTimeFormatter> dateFormatter = new ArrayList<>();
	public static final List<DateTimeFormatter> hourFormatter = new ArrayList<>();
	static {
		dateFormatter.add(DateTimeFormatter.ofPattern("yyyyMMdd")); //HH
		dateFormatter.add(DateTimeFormatter.ofPattern("yyyyMMdd"));
		hourFormatter.add(DateTimeFormatter.ofPattern("HH"));
		hourFormatter.add(DateTimeFormatter.ofPattern("HH"));
	}
	private static final Random RANDOM = new Random();
	private static final long MIN_RANDOM = 3850L;//5230L;
	private static final long MAX_RANDOM = 3000L;//5200L;
	private static final int RADAR_MIN_RESOLUTION = 6;
	private static final int RADAR_MIN_POST_UPLOAD_TIME_MIN = 0;//1; // the time between current and last image upload
	private static final int RADAR_2ND_LAST_INTERVAL_OFFSET_MIN = 6; // get the 2nd last set of 6 min radar images
	private static final long RADAR_TRAILING_OFFSET_CHECK_GRANULARITY_SEC = 10L;
	private static int DST_TO_UTC_INTERVAL_SUBTRACTION_HOUR = 4;  // pending TimeZone usage
		
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
			for(int site=0; site<RADAR_SITES_COUNT; site++) {
				try {
					captureImage(SITE_L2_ID[site].toLowerCase(), BASE_URL + computePostfixUrl(site,0), 0);
				} catch (Exception e) {
					// particular radar image n/a - skip
					System.out.println(e);
					System.out.println("Skipping: " + SITE_L2_ID[site]);
				}
		    	//random10secDelay(MIN_RANDOM);
		    	try {
					captureImage(SITE_L2_ID[site].toLowerCase(), BASE_URL + computePostfixUrl(site, 1), 1);
				} catch (Exception e) {
					// particular radar image n/a - skip
					System.out.println(e);
					System.out.println("Skipping: " + site + ": " + SITE_L2_ID[site]);
				}
			}
		}
	}
	
	// https://dd.weather.gc.ca/radar/CAPPI/GIF/CASFT/202508311230_CASFT_CAPPI_1.5_RAIN.gif
	// https://dd.weather.gc.ca/radar/DPQPE/GIF/CASFT/20250831T1230Z_MSC_Radar-DPQPE_CASFT_Rain.gif
	// last 2354 interval of today - insert 20250831/WXO-DD above /radar
	// https://dd.weather.gc.ca/20250831/WXO-DD/radar/CAPPI/GIF/CASFT/202508312354_CASFT_CAPPI_1.5_RAIN.gif
	// https://dd.weather.gc.ca/20250831/WXO-DD/radar/DPQPE/GIF/CASFT/20250831T2354Z_MSC_Radar-DPQPE_CASFT_Rain.gif
	private String computePostfixUrl(int siteID, int cappiID) {
		StringBuffer buffer = new StringBuffer();
		// GMT-4 check DST - align to 00+6min intervals for last radar upload, however get 6 min ago (2nd last upload)
		LocalDateTime offsetTime = LocalDateTime.now()
				.minusMinutes(RADAR_2ND_LAST_INTERVAL_OFFSET_MIN)
				.plusHours(DST_TO_UTC_INTERVAL_SUBTRACTION_HOUR);
		String formattedDate = offsetTime.format(dateFormatter.get(cappiID));
		String formattedHour = offsetTime.format(hourFormatter.get(cappiID));
		String urlPostfix = buffer.append("today/radar/")
				.append(CAPPI_DPQPE_L2_ID[cappiID])
				.append("/GIF/")
				.append("CAS")
				.append(SITE_L2_ID[siteID])
				.append("/")
				.append(formattedDate)
				.append(CAPPI_DPQPE_TIME_T_ID[cappiID])
				.append(formattedHour)
				.append(getSixMinuteTrailingOffsetMinute(offsetTime.getMinute()))
				.append(CAPPI_DPQPE_TIME_ZULU_ID[cappiID])
				.append(CAPPI_DPQPE_POST_TIME_CHARS[cappiID])
				.append(CAPPI_DPQPE_L3_PRE_ID[cappiID])
				.append("_")
				.append("CAS")
				.append(SITE_L2_ID[siteID])
				.append("_")
				.append(CAPPI_DPQPE_L3_POST_ID[cappiID])
				.append(CAPPI_DPQPE_END[cappiID])
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
	    		System.out.print(" sleep 5s ");//60s ");
	    		Thread.sleep(5000);//60000);
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
					System.out.println(" wait " + RADAR_TRAILING_OFFSET_CHECK_GRANULARITY_SEC
							+ "s for 6 min interval start - wait for 6: " + (offsetTime.getMinute() - _trailingMinute));
					Thread.sleep(RADAR_TRAILING_OFFSET_CHECK_GRANULARITY_SEC * 1000);
				} catch (Exception e) {
				}
			} else {
				return;
			}
		}
		
	}
    
    public void captureImage(String site, String fullUrl, int cappiID) throws IOException, InterruptedException {
    	Path target = Path.of(TARGET_DIR + CAPPI_DPQPE_L2_ID[cappiID].toLowerCase() + "/" + site, Path.of(URI.create(fullUrl).getPath()).getFileName().toString());
    	// check target already exists - exit if
    	random10secDelay(MIN_RANDOM);
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
        } catch(Exception e) {
        	System.out.println(e);
        }
		System.out.println(" Captured: " + site + ": " + fullUrl);
    }
    
    private long random10secDelay(long baseDelay) {
    	long sec = baseDelay + RANDOM.nextLong(MAX_RANDOM);
    	System.out.print(String.format("wait %d ms - ", sec));
    	try {
    		Thread.sleep(sec);
    	} catch (Exception e) {
    	}
        return sec;
    }
    
    public void downloadAllImagesFromFolder(String folderName) {
    	
    }
	
    public void uploadImage() {
    	
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
		eccCapture.capture();
	}
}
