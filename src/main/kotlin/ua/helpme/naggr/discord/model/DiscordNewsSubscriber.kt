package ua.helpme.naggr.discord.model

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("discord_news_subscriber")
data class DiscordNewsSubscriber(
    @Id val id: ObjectId = ObjectId(),
    val channelId: Long
)