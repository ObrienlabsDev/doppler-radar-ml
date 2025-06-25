
# Download images from the following on a timed cycle
# https://github.com/ObrienlabsDev/doppler-radar-ml/issues/5
# - https://weather.gc.ca/satellite/index_e.html
# - NA https://weather.gc.ca/data/satellite/goes_nam_1070_100.jpg

import requests

def download_image(url_prefix, url, url_postfix, filename):
    try:
        response = requests.get(url_prefix + url + url_postfix, stream = True)
        response.raise_for_status()  # Check for HTTP errors
        with open(filename, 'wb') as file:
            for part in response.iter_content(chunk_size=2048):
                file.write(part)
        print(f"{filename} : {url}")
    except requests.exceptions.RequestException as e:
        print(f"exception on {filename} : {url}: {e}")

# main method
if __name__ == "__main__":
    # URLs to download
    url_prefix = "https://weather.gc.ca/data/satellite/"
    url_postfix = ".jpg"
    urls = [
        "goes_nam_1070_100",
        "goes_enam_1070_100",
        "goes_gwdisk11_1070_100",
        "hrpt_ont1_ir_100"
    ]

    # iterate the urls list
    for i, url in enumerate(urls):
        download_image(url_prefix, url, url_postfix, f"{url}{url_postfix}")


# setup
# python3 -m venv dev
# source dev/bin/activate
# pip3 install requests==2.31.0

# history
# 2025-06-25: Initial version - 