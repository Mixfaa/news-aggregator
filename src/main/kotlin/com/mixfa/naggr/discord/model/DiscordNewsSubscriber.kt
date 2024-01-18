package com.mixfa.naggr.discord.model

import com.mixfa.naggr.news.model.Flag
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("discord_news_subscriber")
data class DiscordNewsSubscriber(
    @Id val id: ObjectId = ObjectId(),
    val channelId: Long,
    val targetFlags: List<Flag>
)