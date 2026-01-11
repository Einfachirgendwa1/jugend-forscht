package org.einfachirgendwa1.jugendForscht

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.ResultSet

// TODO: implement receiver path
fun pathToReceiver(): String {
    return "C:\\Users\\Think\\IdeaProjects\\JugendForscht\\jugend-forscht-receiver\\target\\release\\jugend-forscht-receiver.exe"
}

// TODO: implement database path
fun pathToDb(): String {
    return "C:\\Users\\Think\\IdeaProjects\\JugendForscht\\main.db"
}

suspend fun loadData(sensors: List<Sensor>) {
    val receiverPath = pathToReceiver()
    val dbPath = pathToDb()

    val child = withContext(Dispatchers.IO) {
        ProcessBuilder(receiverPath, dbPath, "--spoof", "--reset-data").inheritIO().start()
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
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