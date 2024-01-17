package ua.helpme.naggr.telegram.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("tgNewsSubscriber")
data class TelegramNewsSubscriber
(
    @Id val id: ObjectId = ObjectId(),
    val chatId: Long
)