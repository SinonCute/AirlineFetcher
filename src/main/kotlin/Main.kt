package hiencao.me

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.OkHttpClient
import java.io.File


fun main() {
    val airlinesCodes: List<String> = listOf("ASA", "AYA", "AAL", "VXP", "MXY", "DAL", "EAL", "FFT", "HAL", "JBU",
        "RVF", "SWA", "NKS", "SCX", "UAL")
    calculateJson(airlinesCodes)

}
private fun calculateJson(airlinesCodes: List<String>) {
    val currentPath = System.getProperty("user.dir")
    val aircrafts = mutableListOf<Aircraft>()

    for (month in 1..12) {
        val monthFormatted = "$month".padStart(2, '0')
        val file = File("$currentPath/${monthFormatted}.json")
        val json = file.readText()
        val aircraftsArray = Gson().fromJson(json, Array<Aircraft>::class.java)
        aircrafts.addAll(aircraftsArray)
    }

    val groupedAircrafts = aircrafts.groupBy { it.icao }

    val totalFlights = aircrafts.size
    val totalAirLines = groupedAircrafts.size
    val totalFlightsPerAirline = groupedAircrafts.map { it.value.size }

    println("Total flights: $totalFlights")
    println("Total airlines: $totalAirLines")
    println("Total flights per airline:")
    totalFlightsPerAirline.forEachIndexed { index, value ->
        println("${airlinesCodes[index]}: $value")
    }
}

private fun downloadAircrafts(airlinesCodes: List<String>) {
    val aircrafts = mutableListOf<Aircraft>()

    val client = OkHttpClient()

    //timer start
    val start = System.currentTimeMillis()
    for (month in 1..12) {
        val times = mutableListOf<String>()

        for (hour in 0..24) {
            var time = "${hour.toString().padStart(2, '0')}0000"
            if (hour == 24) {
                time = "235955"
            }
            times.add(time)
        }

        val monthFormatted = "$month".padStart(2, '0')

        times.forEach { time ->
            println("Downloading data for ${monthFormatted}/01/${time}Z...")
            val url = "https://samples.adsbexchange.com/readsb-hist/2021/${monthFormatted}/01/${time}Z.json.gz"
            val response = client.newCall(okhttp3.Request.Builder().url(url).build()).execute()

            if (response.code != 200) {
                println("Failed to download data for ${monthFormatted}/01/${time}Z")
                return@forEach
            }

            val jsonObject = Gson().fromJson(response.body?.string(), JsonObject::class.java)
            val aircraftsArray = jsonObject.getAsJsonArray("aircraft").filter { it.asJsonObject.get("flight") != null }
            response.close()
            aircraftsArray.forEach {
                val flight = it.asJsonObject.get("flight").asString
                airlinesCodes.forEach { code ->
                    if (flight.startsWith(code) && aircrafts.any { aircraft -> aircraft.flight == flight }.not()) {
                        aircrafts.add(Aircraft(code, flight.trim()))
                    }
                }
            }
        }

        println("Saving data to ${monthFormatted}.json...")
        val currentPath = System.getProperty("user.dir")
        val file = File("$currentPath/${monthFormatted}.json")

        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(aircrafts)
        file.writeText(json)
    }
    //timer end
    val end = System.currentTimeMillis()
    println("Time taken: ${(end - start) / 60000}m")
}

data class Aircraft(
    val icao: String,
    val flight: String,
)