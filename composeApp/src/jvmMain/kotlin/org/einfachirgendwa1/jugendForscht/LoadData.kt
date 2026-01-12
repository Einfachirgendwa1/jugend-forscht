package org.einfachirgendwa1.jugendForscht

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.sql.DriverManager
import java.sql.ResultSet

fun isWindows() = System.getProperty("os.name").lowercase().contains("windows")

fun appDir(): File {
    val appDir = when {
        isWindows() -> File(System.getenv("APPDATA"), "JugendForscht")
        else -> File(System.getProperty("user.home"), "Library/Application Support/JugendForscht")
    }

    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    return appDir
}

fun extractReceiver(): File {
    val receiverName = "jugend-forscht-receiver${if (isWindows()) ".exe" else ""}"
    val receiverFile = File(appDir(), receiverName)

    if (!receiverFile.exists()) {
        println("Extracting receiver to ${receiverFile.absolutePath} because it does not exist.")
       
        val inputStream = object {}.javaClass.getResourceAsStream("/$receiverName")
            ?: throw IllegalStateException("Receiver binary not found in resources")

        receiverFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        receiverFile.setExecutable(true)
    }

    return receiverFile
}

fun dB(): File = File(appDir(), "main.db")

suspend fun loadData(sensors: List<Sensor>) {
    val receiverPath = extractReceiver()

    val child = withContext(Dispatchers.IO) {
        ProcessBuilder(receiverPath.absolutePath, dB().absolutePath, "--spoof", "--reset-data").inheritIO().start()
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:${dB()}")
    val statement = connection.createStatement()
    statement.execute("PRAGMA journal_mode=WAL")
    statement.execute(
        """
        CREATE TABLE IF NOT EXISTS data (
        id          INTEGER PRIMARY KEY,
        sensor      INTEGER,
        timestamp   INTEGER,
        value       INTEGER
    )""".trimIndent()
    )

    statement.close()

    val select = connection.prepareStatement(
        """
        SELECT id, sensor, timestamp, value
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (PARTITION BY sensor ORDER BY timestamp DESC) AS rn
            FROM data
        ) sub
        WHERE rn = 1;
    """.trimIndent()
    )

    onAppClose.add { select.close() }
    onAppClose.add { connection.close() }
    onAppClose.add { child.destroy() }

    while (true) {
        val result = select.executeQuery()
        while (result.next()) {
            val sensor = DbSensor(result)
            sensors.find { it.sensorId == sensor.sensor }?.let { it.pegel = sensor.value.toDouble() / 1000 }
        }

        delay(50)
    }
}

class DbSensor(resultSet: ResultSet) {
    val id: Int = resultSet.getInt("id")
    val sensor: Int = resultSet.getInt("sensor")
    val timestamp: Int = resultSet.getInt("timestamp")
    val value: Int = resultSet.getInt("value")
}