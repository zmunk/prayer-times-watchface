import re
import json
import requests

api_url = "https://namazvakitleri.diyanet.gov.tr/tr-TR/9541/istanbul-icin-namaz-vakti"


def lambda_handler(event, context):
    prayer_times = get_prayer_times()
    return {
        "statusCode": 200,
        "body": json.dumps(prayer_times),
    }


def get_prayer_times():
    response = requests.get(api_url)
    response.raise_for_status()

    prayer_times = []
    for var_name in [
        "_imsakTime",
        "_gunesTime",
        "_ogleTime",
        "_ikindiTime",
        "_aksamTime",
        "_yatsiTime",
    ]:
        match_ = re.search(f'^.*var {var_name} = "(.*)";\r$', response.text, re.M)
        assert match_ is not None, f"{var_name!r} not found"
        prayer_times.append(match_.group(1))
    return prayer_times
