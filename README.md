# doppler-radar-ml
The Government of Canada site for doppler radar images published by ECCC is located at https://dd.weather.gc.ca/
This project details a method of predicting the next sequence of radar image data in the immediate future (6, 12, 18... min).

## Related Projects
Doppler Radar ML
- https://arxiv.org/abs/1706.03762
- see https://github.com/ObrienlabsDev/foundation-transformer-llm
- see https://github.com/ObrienlabsDev/doppler-radar-ml
- see https://github.com/ObrienlabsDev/machine-learning/issues/5
- see https://github.com/ObrienlabsDev/biometric-backend/issues
- see https://github.com/ObrienlabsDev/biometric-backend-python/issues

## Architecture
### Motivations
This project is as much a discovery/relearning exercise on transitioning from 2000 era Genetic Algorithms to Machine Learning techniques supplemented with the latest Transformer architecture worked out by Google in 2017 in "Attention is all you need" https://en.wikipedia.org/wiki/Attention_Is_All_You_Need

## Deployment

### Local Deployment

### Google Cloud Deployment

## Historical Radar Data
- https://climate.weather.gc.ca/radar/index_e.html
- https://eccc-msc.github.io/open-data/msc-data/obs_radar/readme_radarimage-datamart_en/
- https://dd.weather.gc.ca/
- 
- 

## Design Issues
- https://github.com/ObrienlabsDev/doppler-radar-ml/blob/main/design-issues.md
### DI01: Radar Image Download
See 
Downloading both CAPPI and DPQPE images and removing all non-precip levels requires 2 photos per 6 min interval or around 14400 images for 30 sites in 24 hours or 900k images per month.


### DI09: Rate Limiting ECCC Requests
I currently implement a randomized 6 second delay when downloading images from ECCC.  Introduce a formal map/count or token based rate limiter to break out the logic.
see https://github.com/ObrienlabsDev/doppler-radar-ml/issues/15

### DI10: Process Images into Numerical content
see preliminary filtering of only radar levels

<img width="2830" height="1060" alt="Image" src="https://github.com/user-attachments/assets/5cf9a626-0a65-4417-b65c-926445d86eb2" />

### DI11: Representing Image Sequencing - date/time stamp embedding

### DI12: Vectorizing Single or multiple pixel groupswe

### DI13: Model per site
Training models directly on particular site images may work better than a generic model that can process any of the 30 sites.

## Use Cases
15 years of 500k 2k images from a doppler radar station may be processed and used to train a deep neural network that will be used to generate future radar images - However the new format at ECCC allows for images without the historical format artifacts of names and borders covering key pixel areas.  We will use use live and 30 day historical data to train the model.

## Design
  As of 20250821 the ECC site has added an easier way to capture live radar data.  Instead of parsing the consumer site we can go directly to the ecc server and it's secondary. 
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

  
  

## Design Issues

### DI01: Base64 encoding added to historical site
example: https://climate.weather.gc.ca/radar/index_e.html?site=CASFT&year=2024&month=4&day=6&hour=01&minute=54&duration=2&image_type=DPQPE_RAIN_WEATHEROFFICE
```
<script>
  (function () {
  'use strict';
  animationInfo = [
        'DPQPE - Rain - 2024-04-05, 21:54 EDT, 1/21',
        'DPQPE - Rain - 2024-04-05, 22:00 EDT, 2/21',
        'DPQPE - Rain - 2024-04-05, 22:06 EDT, 3/21',
        'DPQPE - Rain - 2024-04-05, 22:12 EDT, 4/21',
        'DPQPE - Rain - 2024-04-05, 22:18 EDT, 5/21',
        'DPQPE - Rain - 2024-04-05, 22:24 EDT, 6/21',
        'DPQPE - Rain - 2024-04-05, 22:30 EDT, 7/21',
        'DPQPE - Rain - 2024-04-05, 22:36 EDT, 8/21',
        'DPQPE - Rain - 2024-04-05, 22:42 EDT, 9/21',
        'DPQPE - Rain - 2024-04-05, 22:48 EDT, 10/21',
        'DPQPE - Rain - 2024-04-05, 22:54 EDT, 11/21',
        'DPQPE - Rain - 2024-04-05, 23:00 EDT, 12/21',
        'DPQPE - Rain - 2024-04-05, 23:06 EDT, 13/21',
        'DPQPE - Rain - 2024-04-05, 23:12 EDT, 14/21',
        'DPQPE - Rain - 2024-04-05, 23:18 EDT, 15/21',
        'DPQPE - Rain - 2024-04-05, 23:24 EDT, 16/21',
        'DPQPE - Rain - 2024-04-05, 23:30 EDT, 17/21',
        'DPQPE - Rain - 2024-04-05, 23:36 EDT, 18/21',
        'DPQPE - Rain - 2024-04-05, 23:42 EDT, 19/21',
        'DPQPE - Rain - 2024-04-05, 23:48 EDT, 20/21',
        'DPQPE - Rain - 2024-04-05, 23:54 EDT, 21/21',
      ];
  imageArray = [
        'data:image/gif;base64,R0lGODlhRALgAecAAAAAAN04Mfbu8f////r6/ON4eP7+/vLd29w4Mf8AAE9PT/.....QgAOw==',
        'data:image/gif;base64,ON4eP7+/vLd29w4Mf8AAE9PT/39/e3My9s3MPv7+/38/ZiYZjMzZurFwe3Py+Js.....CADs=',
      ];
  })();
</script>

```

## Google Cloud
### Upload historical images to cloud storage
```
gcloud auth login
gcloud config unset auth/impersonate_service_account
gsutil -m cp -r ./ gs://doppler1_old/cappi
Operation completed over 78.9k objects/1.2 GiB. 
```
<img width="1276" height="136" alt="Screenshot 2025-09-10 at 12 25 48" src="https://github.com/user-attachments/assets/b2575a35-a5b4-476f-89e4-4f0cfca6fcda" />


## References
- Older Java based project - https://github.com/obrienlabs/radar
