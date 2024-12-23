# Prayer times watchface

![screenshot](Screenshot_1734951551.png "Screenshot")

## API_URL
See `terraform/` for details on setting up server and getting api url.

## Debugging
    export API_URL=(cd terraform && tf output -raw api_url)

option 1)

    ./gradlew clean installDebug

option 2)

    ./gradlew clean assembleDebug
    adb -s emulator-5554 install watchface/build/outputs/apk/debug/watchface-debug.apk
