package hiencao.me


fun main() {
    val airlinesCodes: List<String> = listOf("ASA", "AYA", "AAL", "VXP", "MXY", "DAL", "EAL", "FFT", "HAL", "JBU",
        "RVF", "SWA", "NKS", "SCX", "UAL")
    val aircrafts: List<Aircraft> = listOf()

    val times = mutableListOf<String>()

    // Generate all possible times
    for (hour in 0..23) {
        for (minute in 0..59 step 5) {
            for (second in 0..55 step 5) {
                val time = "$hour".padStart(2, '0') + "$minute".padStart(2, '0') + "$second".padStart(2, '0')
                if (minute == 55 && second == 0) {
                    times.add(time)
                }
            }
        }
    }




}

data class Aircraft(
    val icao: String,
    val flight: String,
)