# doppler-radar-ml
Doppler Radar ML
- see https://github.com/ObrienlabsDev/biometric-backend/issues
- see https://github.com/ObrienlabsDev/biometric-backend-python/issues

## Historical Radar Data
- https://climate.weather.gc.ca/radar/index_e.html
- 
## Use Cases
15 years of 500k 2k images from a doppler radar station will be processed and used to train an deep neural network that will be used to generate future radar images.

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
## References
- Older Java based project - https://github.com/obrienlabs/radar
