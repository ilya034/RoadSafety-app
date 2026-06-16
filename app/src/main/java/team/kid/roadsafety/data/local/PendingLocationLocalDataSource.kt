package team.kid.roadsafety.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import team.kid.roadsafety.data.dto.SubmitLocationRequestDto
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingLocationLocalDataSource @Inject constructor(
    @ApplicationContext context: Context,
    private val json: Json
) {
    private val prefs = context.getSharedPreferences("pending_locations", Context.MODE_PRIVATE)

    @Synchronized
    fun getPendingLocations(): List<SubmitLocationRequestDto> {
        val queue = readQueue()
        val trimmed = trimQueue(queue)
        if (trimmed.size != queue.size) {
            saveQueue(trimmed)
        }
        return trimmed
    }

    @Synchronized
    fun enqueue(location: SubmitLocationRequestDto) {
        saveQueue(trimQueue(readQueue() + location))
    }

    @Synchronized
    fun removeFirst(count: Int) {
        if (count <= 0) return
        saveQueue(trimQueue(readQueue().drop(count)))
    }

    private fun readQueue(): List<SubmitLocationRequestDto> {
        val value = prefs.getString(QueueKey, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SubmitLocationRequestDto>>(value)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveQueue(queue: List<SubmitLocationRequestDto>) {
        prefs.edit()
            .putString(QueueKey, json.encodeToString(queue))
            .commit()
    }

    private fun trimQueue(queue: List<SubmitLocationRequestDto>): List<SubmitLocationRequestDto> {
        val oldestAllowed = Instant.now().minus(MaxAge)
        val recentLocations = queue.filter { location ->
            location.recordedAt?.toInstantOrNull()?.let { !it.isBefore(oldestAllowed) } == true
        }
        return recentLocations.takeLast(MaxLocations)
    }

    private fun String.toInstantOrNull(): Instant? {
        return try {
            Instant.parse(this)
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val QueueKey = "queue"
        const val MaxLocations = 5_760
        val MaxAge: Duration = Duration.ofHours(24)
    }
}
