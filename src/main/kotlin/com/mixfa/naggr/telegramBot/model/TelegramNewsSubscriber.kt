package com.mixfa.naggr.telegramBot.model

import com.mixfa.naggr.newsletter.model.News
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable

@Document("TelegramNewsSubscriber")
data class TelegramNewsSubscriber(
    @Id val id: ObjectId = ObjectId(),
    var chatId: Long,
    var targetFlags: List<News.Flag> = News.Flag.entries,
) : Serializable