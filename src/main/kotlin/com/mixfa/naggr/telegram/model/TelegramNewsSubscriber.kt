package com.mixfa.naggr.telegram.model

import com.mixfa.naggr.news.model.Flag
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("tgNewsSubscriber")
data class TelegramNewsSubscriber(
    @Id val id: ObjectId = ObjectId(),
    var chatId: Long,
    var targetFlags: List<Flag>,
//    var language: String
)