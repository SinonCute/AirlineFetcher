package hiencao.me

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

private var dataSource: HikariDataSource? = null
fun main() {
    val config = HikariConfig()
    config.jdbcUrl = "jdbc:h2:file:./data/database"
    config.driverClassName = "org.h2.Driver"
    dataSource = HikariDataSource(config)
    createTable()

//    var successes = 0
//    runBlocking {
//        launch {
//            val aircrafts = fetchTable().filter { it.model == null }
//            aircrafts.forEach {
//                println("\u001B[32mGetting aircraft ${it.flight} - ${it.date}...\u001B[0m")
//                val aircraft = fetchAPIWithRateLimit(it)
//                if (aircraft.model != null) {
//                    successes++
//                    inputData(aircraft)
//                } else {
//                    deleteData(aircraft)
//                }
//                println("\u001B[32mCurrent successes: $successes out of ${aircrafts.size} | ${successes.toFloat() / aircrafts.size * 100}%\u001B[0m")
//            }
//        }
//    }
}

private fun tableToJSON(aircrafts: List<Aircraft>) {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val json = gson.toJson(aircrafts)
    val currentPath = System.getProperty("user.dir")
    val file = File("$currentPath/data/aircrafts.json")
    file.writeText(json)
}

private suspend fun fetchAPIWithRateLimit(aircraft: Aircraft): Aircraft {
    val startTime = System.currentTimeMillis()
    val response  = fetchAPI(aircraft)
    val elapsedTime = System.currentTimeMillis() - startTime
    val remainingDelay = (TimeUnit.MINUTES.toMillis(1) / 80  - elapsedTime).coerceAtLeast(0)
    delay(remainingDelay)
    return response
}

private fun getRandomAirscraft() {
    val checkEachAirlinePerYear = listOf(103, 2492, 3264, 2753, 2934, 723, 5, 1085, 667, 693, 101, 134, 42, 4)
    val airlines = listOf("HAL", "UAL", "AAL", "DAL", "SWA", "ASA", "RVF", "JBU", "FFT", "NKS", "MXY", "SCX", "VXP", "EAL")

    val currentPath = System.getProperty("user.dir")

    val aircrafts = mutableListOf<Aircraft>()
    val aircraftsSorted = mutableListOf<Aircraft>()

    val files = File("$currentPath/data").listFiles()?.filter { it.extension == "json" }

    files?.forEach { file ->
        val json = file.readText()
        val aircraftsArray = Gson().fromJson(json, Array<Aircraft>::class.java)
        aircrafts.addAll(aircraftsArray)
    }

    airlines.forEachIndexed { index, code ->
        val needToTake = checkEachAirlinePerYear[index]
        val aircraftsMatched = aircrafts.filter { it.icao == code }
        val randomAircrafts = aircraftsMatched.shuffled().take(needToTake)

        randomAircrafts.forEach {
            it.id = UUID.randomUUID().toString()
        }
        aircraftsSorted.addAll(randomAircrafts)
    }
    aircraftsSorted.forEach(::inputData)
}

private fun calculateJson() {
    val currentPath = System.getProperty("user.dir")
    val aircrafts = mutableListOf<Aircraft>()

    if (File(currentPath).listFiles().isNullOrEmpty()) {
        println("No json files found")
        return
    }

    val files = File("$currentPath/data").listFiles()?.filter { it.extension == "json" }

    files?.forEach { file ->
        val json = file.readText()
        val aircraftsArray = Gson().fromJson(json, Array<Aircraft>::class.java)
        aircrafts.addAll(aircraftsArray)
    }

    val groupedAircrafts = aircrafts.groupBy { it.icao }

    val totalFlights = aircrafts.size
    val totalAirLines = groupedAircrafts.size

    println("Total flights: $totalFlights")
    println("Total airlines: $totalAirLines")
    println("Total flights per airline:")
    groupedAircrafts.forEach { (key, value) ->
        println("$key: ${value.size}")
    }
}

private fun downloadAircrafts(airlinesCodes: List<String>) = runBlocking {
    val jobs = mutableListOf<Job>()

    //start from May 2023 to April 2024
    var monthStart = 5
    var monthEnd = 12
    for (year in 2023..2024) {
        for (month in monthStart..monthEnd) {
            //launch 3 threads at a time
            jobs += launch(Dispatchers.Default) {
                println("Starting thread for ${year}/${month}...")
                val times = mutableListOf<String>()
                val aircrafts = mutableListOf<Aircraft>()

                for (hour in 0..24) {
                    var time = "${hour.toString().padStart(2, '0')}0000"
                    if (hour == 24) {
                        time = "235955"
                    }
                    times.add(time)
                }

                val monthFormatted = "$month".padStart(2, '0')
                val client = OkHttpClient.Builder().readTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()

                times.forEach { time ->
                    println("Downloading data for ${monthFormatted}/01/${time}Z...")
                    val url =
                        "https://samples.adsbexchange.com/readsb-hist/${year}/${monthFormatted}/01/${time}Z.json.gz"
                    val response = client.newCall(Request.Builder().url(url).build()).execute()
                    if (response.code != 200) {
                        println("Failed to download data for ${year}/${monthFormatted}/01/${time}Z")
                        return@forEach
                    }

                    val jsonObject = Gson().fromJson(response.body?.string(), JsonObject::class.java)
                    val aircraftsArray =
                        jsonObject.getAsJsonArray("aircraft").filter { it.asJsonObject.get("flight") != null }
                    response.close()
                    aircraftsArray.forEach {
                        val flight = it.asJsonObject.get("flight").asString
                        airlinesCodes.forEach { code ->
                            if (flight.startsWith(code) && aircrafts.any { aircraft -> aircraft.flight == flight }
                                    .not()) {
                                aircrafts.add(
                                    Aircraft(
                                        UUID.randomUUID().toString(),
                                        code,
                                        flight.trim(),
                                        "${year}-${monthFormatted}-01"
                                    )
                                )
                            }
                        }
                    }
                }
                println("Saving data to ${year}-${monthFormatted}.json...")
                val currentPath = System.getProperty("user.dir")
                val file = File("$currentPath/data/${year}-${monthFormatted}.json")

                val gson = GsonBuilder().setPrettyPrinting().create()
                val json = gson.toJson(aircrafts)
                file.writeText(json)
            }
            if (jobs.size == 3) {
                jobs.forEach { it.join() }
                jobs.clear()
            }
        }
        monthStart = 1
        monthEnd = 4
    }
}

private fun createTable() {
    val connection = dataSource!!.connection
    val statement = connection.createStatement()
    statement.execute("""
        CREATE TABLE IF NOT EXISTS aircrafts (
            id VARCHAR(36) PRIMARY KEY, 
            icao VARCHAR(4), 
            flight VARCHAR(10), 
            date VARCHAR(10), 
            model VARCHAR(50) NULL, 
            distance VARCHAR(100) NULL
            )
    """.trimIndent())
    statement.close()
    connection.close()
}

fun inputData(aircraft: Aircraft) {
    val connection = dataSource!!.connection
    val statement = connection.prepareStatement("""
        MERGE INTO aircrafts 
        (id, icao, flight, date, model, distance) 
        KEY(id) 
        VALUES (?, ?, ?, ?, ?, ?)
    """.trimIndent())
    statement.setString(1, aircraft.id)
    statement.setString(2, aircraft.icao)
    statement.setString(3, aircraft.flight)
    statement.setString(4, aircraft.date)
    statement.setString(5, aircraft.model)
    statement.setString(6, aircraft.distance?.let { Gson().toJson(it) })

    statement.execute()
    statement.close()
    connection.close()
}

fun deleteData(aircraft: Aircraft) {
    val connection = dataSource!!.connection
    val statement = connection.prepareStatement("DELETE FROM aircrafts WHERE id = ?")
    statement.setString(1, aircraft.id)
    statement.execute()
    statement.close()
    connection.close()
}

fun fetchTable(): List<Aircraft> {
    val connection = dataSource!!.connection
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT * FROM aircrafts")

    val aircrafts = mutableListOf<Aircraft>()
    while (resultSet.next()) {
        val distance = resultSet.getString("distance")
        val aircraft = Aircraft(
            resultSet.getString("id"),
            resultSet.getString("icao"),
            resultSet.getString("flight"),
            resultSet.getString("date"),
            resultSet.getString("model"),
            Gson().fromJson(distance, GreatCircleDistance::class.java)
        )
        aircrafts.add(aircraft)
    }

    resultSet.close()
    statement.close()
    connection.close()

    return aircrafts
}

suspend fun fetchAPI(aircraft: Aircraft): Aircraft {
    val client = OkHttpClient()

    val request: Request = Request.Builder()
        .url("https://aerodatabox.p.rapidapi.com/flights/callsign/${aircraft.flight}/${aircraft.date}?withAircraftImage=false&withLocation=false")
        .get()
        .addHeader("X-RapidAPI-Key", "API_KEY")
        .addHeader("X-RapidAPI-Host", "aerodatabox.p.rapidapi.com")
        .build()

    val response: Response = client.newCall(request).execute()

    if (response.code != 200 && response.code != 429) {
        println("\u001B[31mFailed to fetch data for ${aircraft.flight} - ${aircraft.date}.\u001B[0m")
        return aircraft
    }

    if (response.code == 429) {
        println("\u001B[31mRate limit exceeded. Please try again later.\u001B[0m")
        delay(60000)
        println("\u001B[31mRetrying...\u001B[0m")
        return fetchAPI(aircraft)
    }

    val rawData = response.body?.string()
    val jsonArray = Gson().fromJson(rawData, JsonArray::class.java)
    var greatCircleDistance: JsonObject? = null
    var model: String? = null
    for (index in 0 until jsonArray.size()) {
        val obj = jsonArray[index].asJsonObject
        if (obj.get("greatCircleDistance") != null) {
            greatCircleDistance = obj.get("greatCircleDistance").asJsonObject
        }
        if (obj.get("aircraft") != null && obj.get("aircraft").isJsonNull.not()) {
            model = obj.get("aircraft").asJsonObject.get("model").asString
        }
        if (greatCircleDistance != null && model != null) {
            break
        }
    }

    if (greatCircleDistance == null) {
        greatCircleDistance = JsonObject()
        greatCircleDistance.addProperty("km", "NaN")
        greatCircleDistance.addProperty("mile", "NaN")
        greatCircleDistance.addProperty("nm", "NaN")
    }

    response.close()
    client.dispatcher.executorService.shutdown()

    return aircraft.copy(
        model = model, distance = GreatCircleDistance(
            km = if (greatCircleDistance.get("km").asString != "NaN") greatCircleDistance.get("km").asDouble else 0.0,
            mile = if (greatCircleDistance.get("mile").asString != "NaN") greatCircleDistance.get("mile").asDouble else 0.0,
            nm = if (greatCircleDistance.get("nm").asString!= "NaN") greatCircleDistance.get("nm").asDouble else 0.0
        )
    )
}


data class Aircraft(
    var id: String,
    val icao: String,
    val flight: String,
    val date: String,
    val model: String? = null,
    val distance: GreatCircleDistance? = null
)

data class GreatCircleDistance(
    val km: Double,
    val mile: Double,
    val nm: Double
)