import urllib.request
import csv

def download_and_parse_csv(url):
    """Downloads a CSV file from a URL and parses it into an array.
    Args:
        url: The URL of the CSV files
    Returns:
        A list of lists, where each inner list represents a row of the CSV.
    """

    with urllib.request.urlopen(url) as response:
        data = response.read().decode('utf-8')  # Decode as text
        csv_reader = csv.reader(data.splitlines())  # Split into lines
        list_of_rows = list(csv_reader)
        return list_of_rows

# Example usage:
url = 'https://obrienlabs.dev'
csv_data = download_and_parse_csv(url)

# Print the parsed data
for row in csv_data:
    print(row)
