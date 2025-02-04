package com.kapivara.adapters.file.database

import com.eventhub.domain.eventstore.EventIdentity.IdentityId
import com.eventhub.domain.eventstore.EventMessage
import com.eventhub.domain.eventstore.EventMessage.EventMessageId
import com.eventhub.domain.eventstore.EventStream
import com.eventhub.domain.eventstore.EventStream.EventStreamId
import com.eventhub.domain.eventstore.Publisher.PublisherId
import com.eventhub.domain.eventstore.ports.EventStreamRepository
import com.kapivara.adapters.file.database.FileDatabaseVariables.streamPath
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Service
import java.io.File
import java.util.UUID

@Service
class FileDatabaseEventStreamRepository(
    private val database: FileDatabase,
) : EventStreamRepository {
    override suspend fun store(eventMessage: EventMessage) {
        val streamId = eventMessage.eventStreamId.toString()
        val messageId = eventMessage.id.toString()
        val folderStreamHash = streamId.hashCode()
        val streamFile = File(streamPath() + "$folderStreamHash/$streamId/$messageId.gz")

        streamFile.parentFile?.mkdirs()

        database.writeFileAsync(
            filePath = streamFile.path,
            content = Json.encodeToString(eventMessage.toMap()),
        )
    }

    override suspend fun fetch(eventStreamId: EventStreamId): EventStream? {
        val folderStreamHash = eventStreamId.toString().hashCode()
        val streamFile = File(streamPath() + "$folderStreamHash/$eventStreamId")

        if (streamFile.exists().not()) return null

        val messages =
            streamFile
                .listFiles()
                .toList()
                .map { file -> file.toMap() }
                .map { json -> json.toEventMessage() }
                .toSet()

        return EventStream(eventMessages = messages)
    }

    private suspend fun File.toMap(): Map<String, Any> = Json.decodeFromString<Map<String, Any>>(database.readFileAsync(this.path))

    private fun EventMessage.toMap(): Map<String, Any> =
        mapOf(
            "id" to id.value,
            "identityId" to identityId.value,
            "publisherId" to publisherId.value,
            "eventStreamId" to eventStreamId.value,
            "payload" to payload,
            "position" to position,
            "isFinal" to isFinal,
            "occurredOn" to occurredOn.toString(),
        )

    private fun Map<String, Any>.toEventMessage(): EventMessage =
        EventMessage(
            id = EventMessageId(getValue("id") as UUID),
            identityId = IdentityId(getValue("identityId") as Long),
            publisherId = PublisherId(getValue("publisherId") as Long),
            eventStreamId = EventStreamId(getValue("eventStreamId") as UUID),
            payload = getValue("payload") as String,
            position = (getValue("position") as Number).toLong(),
            isFinal = getValue("isFinal") as Boolean,
            occurredOn = Instant.parse(getValue("occurredOn") as String),
        )
}
