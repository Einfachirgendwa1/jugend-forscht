package org.einfachirgendwa1.jugendForscht

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.sql.DriverManager

// TODO
fun pathToReceiver(): String {
    return "C:\\Users\\Think\\IdeaProjects\\JugendForscht\\jugend-forscht-receiver\\target\\release\\jugend-forscht-receiver.exe"
}

// TODO
fun pathToDb(): String {
    return "C:\\Users\\Think\\IdeaProjects\\JugendForscht\\main.db"
}

suspend fun loadData(sensors: List<Sensor>) {
    val receiverPath = pathToReceiver()
    val dbPath = pathToDb()

    withContext(Dispatchers.IO) {
        ProcessBuilder(receiverPath, dbPath).inheritIO().start()
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

    while (true) {
        val result = select.executeQuery()
        while (result.next()) {
            sensors.find { it.id == result.getInt("sensor") }
                ?.let { it.pegel = result.getInt("value").toDouble().also { println("Setting pegel to $it") } }
        }

        delay(1000)
    }
}