package com.mixfa.naggr.discordBot.model

import com.mixfa.naggr.news.model.News
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("discord_news_subscriber")
data class DiscordSubscriber(
    @Id val id: ObjectId = ObjectId(),
    val channelId: Long,
    val targetFlags: List<News.Flag>
)