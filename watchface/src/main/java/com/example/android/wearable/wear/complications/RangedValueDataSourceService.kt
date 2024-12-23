package com.example.android.wearable.wear.complications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class RangedValueDataSourceService : ComplicationDataSourceService() {
    // Companion object to define constants and shared methods
    companion object {
        private const val PREFS_NAME = "ComplicationDataSourcePrefs"
        private const val LAST_REQUEST_TIME_KEY = "last_request_time"
        private const val CACHED_RESPONSE_KEY = "cached_response"
        private const val CACHE_DURATION_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
        private val API_URL = BuildConfig.API_URL
    }

    // Get SharedPreferences for persistent storage
    private fun getSharedPrefs(): SharedPreferences =
        applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationDataSourceService.ComplicationRequestListener,
    ) {
        var complicationData = runBlocking { getComplicationData() }
        listener.onComplicationData(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        runBlocking {
            getComplicationData()
        }

    private suspend fun getComplicationData(): ComplicationData =
        withContext(Dispatchers.IO) {
            val prefs = getSharedPrefs()
            val currentTimeMillis = System.currentTimeMillis()
            val lastRequestTime = prefs.getLong(LAST_REQUEST_TIME_KEY, 0L)
            var response = prefs.getString(CACHED_RESPONSE_KEY, null)

            val dataExpired: Boolean = (currentTimeMillis - lastRequestTime) > CACHE_DURATION_MILLIS
            val currentTime = Calendar.getInstance()

            var displayText = ""

            if (response == null || dataExpired) {
                Log.d("RangedValueDataSource", "making request ...")
                Log.d("RangedValueDataSource", "API_URL: ${API_URL}")

                try {
                    val url = URL(API_URL)
                    val connection = url.openConnection() as HttpURLConnection

                    connection.requestMethod = "GET"

                    val responseCode = connection.responseCode
                    Log.d("RangedValueDataSource", "Response Code: $responseCode")

                    response =
                        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                            reader.readText()
                        }

                    connection.disconnect()

                    prefs
                        .edit()
                        .putLong(LAST_REQUEST_TIME_KEY, currentTimeMillis)
                        .putString(CACHED_RESPONSE_KEY, response)
                        .apply()
                } catch (e: Exception) {
                    Log.e("RangedValueDataSource", "Error fetching data", e)
                }
            }

            var upcomingPrayers: List<Calendar> = listOf()
            if (response == null) {
                displayText = "Error"
            } else {
                Log.d("RangedValueDataSource", "response: $response")

                val allPrayerTimes: List<String> =
                    Json.decodeFromString(
                        response ?: error("response is null"),
                    )

                upcomingPrayers = getUpcomingPrayers(allPrayerTimes)
            }

            Log.d(
                "RangedValueDataSource",
                "upcomingPrayers: ${
                    upcomingPrayers.map { prayer ->
                        prayer.toHHmm()
                    }
                }",
            )

            /** values between 000 - 360 representing location of marks
             * 999 -> disable
             * AAABBBCCCDDD
             * AAA (0..2): position of hour mark of next prayer
             * BBB (3..5): position of hour mark of 2nd upcoming prayer
             * CCC (6..8): position of hour mark of 3rd upcoming prayer
             * DDD (9..11): position of minute mark of next prayer
             */
            var contentDescriptionText = "999999999999"

            if (upcomingPrayers.size >= 1) {
                val nextPrayerTime = upcomingPrayers[0]
                val tick = Tick.fromCalendar(nextPrayerTime)

                // set hour mark of next prayer
                contentDescriptionText =
                    contentDescriptionText.replaceRange(0..2, String.format("%03d", tick.hour))

                // Calculate difference in milliseconds and convert to minutes
                val minutesLeft =
                    (
                        (
                            nextPrayerTime.timeInMillis -
                                currentTime.timeInMillis
                        ) / (1000 * 60)
                    ).toInt()

                if (minutesLeft < 60) {
                    // set minute mark of next prayer
                    contentDescriptionText =
                        contentDescriptionText.replaceRange(
                            9..11,
                            String.format("%03d", tick.minute),
                        )
                }
            }

            // set hour mark of second and third upcoming prayers
            for (i in 1..2) {
                if (upcomingPrayers.size >= i + 1) {
                    val prayerTime = upcomingPrayers[i]
                    val tick = Tick.fromCalendar(prayerTime)

                    contentDescriptionText =
                        contentDescriptionText.replaceRange(
                            (3 * i)..(3 * i + 2),
                            String.format("%03d", tick.hour),
                        )
                }
            }

            Log.d("RangedValueDataSource", "contentDescriptionText: $contentDescriptionText")

            RangedValueComplicationData
                .Builder(
                    value = 0f,
                    min = 0f,
                    max = 0f,
                    contentDescription = ComplicationText.EMPTY,
                ).setText(
                    PlainComplicationText.Builder(text = displayText).build(),
                ).setTitle(
                    PlainComplicationText
                        .Builder(text = contentDescriptionText)
                        .build(),
                ).build()
        }
}

/**
 * Represents the angular positions of two markers on a clock face relative to 12 o'clock.
 *
 * @property hour The clockwise angle in degrees from 12 o'clock to the hour tick mark (0.0 to 360.0)
 * @property minute The clockwise angle in degrees from 12 o'clock to the minute tick mark (0.0 to 360.0)
 *
 * Example:
 * ```
 * Tick(90f, 180f) // Places hour marker at 3 o'clock (90°) and minute marker at 6 o'clock (180°)
 * ```
 */
data class Tick(
    val hour: Int,
    val minute: Int,
) {
    companion object {
        /**
         * Calculates the angular positions of hour and minute markers on a clock face
         * based on a given Calendar time.
         *
         * @param time Calendar instance representing the time to convert to angular positions
         * @return Tick instance with angles in degrees from 12 o'clock (0.0 to 360.0)
         */
        fun fromCalendar(time: Calendar): Tick {
            val minute = time.get(Calendar.MINUTE).toFloat()
            Log.d("RangedValueDataSource", "minute: $minute")
            val hour = time.get(Calendar.HOUR).toFloat()
            Log.d("RangedValueDataSource", "hour: $hour")
            val minuteTick = (minute / 60 * 360).toInt()
            Log.d("RangedValueDataSource", "minuteTick: $minuteTick")
            val hourTick = ((hour + minute / 60) / 12 * 360).toInt()
            Log.d("RangedValueDataSource", "hourTick: $hourTick")
            return Tick(hourTick, minuteTick)
        }
    }
}

/**
 * Creates a Calendar instance set to the next occurrence of the specified time.
 * If the given time has already passed today, the Calendar will be set to tomorrow's date.
 *
 * @param timeString A string representing time in "HH:mm" format (24-hour clock)
 *                   e.g., "13:30" for 1:30 PM, "09:45" for 9:45 AM
 * @return Calendar instance set to the next occurrence of the specified time,
 *         with seconds set to 0
 * @throws IllegalArgumentException if the time string is not in the correct format
 *         or contains invalid hour/minute values (hours must be 0-23, minutes 0-59)
 */
fun getNextOccurrenceOf(timeString: String): Calendar {
    val currentTime = Calendar.getInstance()
    val (hours, minutes) = timeString.split(":").map { it.toInt() }

    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.MINUTE, minutes)
        set(Calendar.SECOND, 0)

        if (before(currentTime)) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

/**
* Gets a list of Calendar instances for upcoming prayer times within the next 12 hours.
*
* @param allPrayerTimes List of prayer times in "HH:mm" format (e.g. ["05:00", "12:30", "15:45"])
* @return List of Calendar instances for prayer times occurring within the next 12 hours,
*         in chronological order, with seconds set to 0
*
* The function works by:
* 1. Finding the first prayer time that hasn't occurred yet today
* 2. Collecting prayer times until we reach one that's more than 12 hours away
* 3. If needed, wrapping around to the start of the next day's prayers
* 4. Converting each prayer time string into a Calendar instance set to its next occurrence
*
* Example usage:
* ```
* val prayerTimes = listOf("05:00", "12:30", "15:45", "18:20", "20:00")
* // If current time is 14:00:
* val upcomingPrayers = getUpcomingPrayers(prayerTimes)
* // Returns Calendars for 15:45 today, 18:20 today, and 20:00 today
* ```
*/
fun getUpcomingPrayers(allPrayerTimes: List<String>): List<Calendar> {
    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val cutoffTime = LocalTime.now().plusHours(12).format(DateTimeFormatter.ofPattern("HH:mm"))

    // Find the index of the first prayer time that hasn't occurred yet
    val startIndex =
        allPrayerTimes.indexOfFirst { it >= currentTime }.let {
            if (it == -1) 0 else it
        }

    val upcomingPrayers = mutableListOf<Calendar>()

    // Check both remaining prayers today and early prayers tomorrow
    for (i in (startIndex..5) + (0 until startIndex)) {
        val prayerTime = allPrayerTimes[i]

        // Stop if we've reached a prayer time more than 12 hours away
        if ((cutoffTime < prayerTime && prayerTime < currentTime) ||
            (currentTime < cutoffTime && cutoffTime < prayerTime)
        ) {
            break
        }

        upcomingPrayers.add(getNextOccurrenceOf(prayerTime))
    }

    return upcomingPrayers
}

fun Calendar.toHHmm(): String {
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    return dateFormat.format(this.time)
}
