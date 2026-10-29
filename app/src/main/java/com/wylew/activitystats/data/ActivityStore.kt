package com.wylew.activitystats.data

import com.wylew.activitystats.domain.Activity
import com.wylew.activitystats.domain.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Local cache of activity summaries as one JSON file.
 *
 * A few years of activities is a few hundred KB, so this keeps the first version free of
 * a database. Swap in Room when Health Connect data (and per-second streams) arrive.
 */
class ActivityStore(private val file: File, private val json: Json) {

    private val lock = Mutex()
    private val state = MutableStateFlow<List<Activity>?>(null)
    private val serializer = ListSerializer(Activity.serializer())

    /** All cached activities, newest first. Loads from disk on first collection. */
    val activities: Flow<List<Activity>> = state.onStart { ensureLoaded() }.filterNotNull()

    suspend fun all(): List<Activity> {
        ensureLoaded()
        return state.value.orEmpty()
    }

    suspend fun upsert(items: List<Activity>) {
        lock.withLock {
            val current = state.value ?: read()
            val merged = (current.associateBy { it.id } + items.associateBy { it.id })
                .values.sortedByDescending { it.startEpochSec }
            write(merged)
            state.value = merged
        }
    }

    suspend fun clear(source: Source) {
        lock.withLock {
            val remaining = (state.value ?: read()).filter { it.source != source }
            write(remaining)
            state.value = remaining
        }
    }

    private suspend fun ensureLoaded() {
        if (state.value != null) return
        lock.withLock { if (state.value == null) state.value = read() }
    }

    private suspend fun read(): List<Activity> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        runCatching { json.decodeFromString(serializer, file.readText()) }.getOrElse { emptyList() }
    }

    private suspend fun write(items: List<Activity>) {
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, items))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }
}
