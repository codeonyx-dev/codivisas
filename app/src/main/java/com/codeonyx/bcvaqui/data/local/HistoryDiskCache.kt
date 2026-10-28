package com.codeonyx.bcvaqui.data.local

import com.codeonyx.bcvaqui.data.model.BcvRateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** Guarda el histórico BCV en disco para verlo sin conexión (máx. [MAX_ENTRIES] días). */
class HistoryDiskCache(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(BcvRateResponse.serializer())

    suspend fun read(): List<BcvRateResponse> = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) emptyList() else json.decodeFromString(serializer, file.readText())
        }.getOrDefault(emptyList())
    }

    suspend fun write(entries: List<BcvRateResponse>) = withContext(Dispatchers.IO) {
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(serializer, entries.takeLast(MAX_ENTRIES)))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        Unit
    }

    companion object {
        const val MAX_ENTRIES = 400
    }
}
