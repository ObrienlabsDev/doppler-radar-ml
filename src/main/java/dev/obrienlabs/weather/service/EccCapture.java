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

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;
import com.google.cloud.storage.StorageOptions;

/**
 * https://github.com/ObrienlabsDev/doppler-radar-ml/issues/7
 */
public class EccCapture {
	
	public static String BASE_URL = "https://dd.weather.gc.ca/";
	//https://console.cloud.google.com/storage/overview;tab=overview?hl=en&project=doppler-radar-old
	public static String CLOUD_STORAGE_URL = "";
	public static String GCS_BUCKET_NAME = "doppler1_old";
		
    private static final Logger logger = Logger.getLogger(EccCapture.class.getName());
    private final Storage storage;
	
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
	public void capture() throws IOException, InterruptedException {
		//createGCSBucket(GCS_BUCKET_NAME);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHH");//mm");
		// GMT-4 check DST - align to 00+6min intervals
		LocalDateTime offsetTime = LocalDateTime.now().minusMinutes(0).plusHours(4);
		String formattedDateTime = offsetTime.format(formatter);
		String urlPostfix = "today/radar/CAPPI/GIF/CASFT/" + formattedDateTime + "00_CASFT_CAPPI_1.5_RAIN.gif";
		System.out.println("Capturing: " + urlPostfix);
		captureImage(BASE_URL + urlPostfix);
	}
	
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
    
    public void uploadImage() {
    	
    }
    
    public void captureImage(String baseUrl) throws IOException, InterruptedException {
    	//Path target = Path.of("~/_radar_unprocessed_image2025/cappi/casft", Path.of(URI.create(baseUrl).getPath()).getFileName().toString());
    	Path target = Path.of(".", Path.of(URI.create(baseUrl).getPath()).getFileName().toString());

        Thread.sleep(5000);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl))
                .header("User-Agent", "obrienlabs-EccCapture/0.9 (+java.net.http)")
                .timeout(Duration.ofMinutes(1))
                .GET()
                .build();

        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " while downloading " + baseUrl);
        }

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        try (InputStream in = response.body()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    
    public void downloadAllImagesFromFolder(String folderName) {
    	
    }
	
	// setup HttpClient
	
	// compute url (don't parse the directory)
	
	// schedule for every 6 min with offset 2 min from 00
	
	// iterate across all radar sites with 5-10 sec random delay - don't overload and don't look like a robot
	
	// save images in mirror subfolders
	

	
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
		try {
			eccCapture.capture();
		} catch (Exception e) {
			System.out.println(e);
		}
		System.out.println(eccCapture);
	}
}
