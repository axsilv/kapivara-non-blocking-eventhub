package com.kapivara.domain.eventstore

import com.kapivara.domain.Identifier
import com.kapivara.domain.eventstore.ports.EventPublisherRepository

data class Publisher(
    val id: PublisherId,
    val publisherName: String,
) {
    data class PublisherId(
        val value: Long,
    ) : Identifier<Long>(value = value)

    suspend fun store(eventPublisherRepository: EventPublisherRepository) {
        eventPublisherRepository.store(this)
    }
}
