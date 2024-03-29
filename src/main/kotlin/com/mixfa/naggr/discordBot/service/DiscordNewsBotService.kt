package com.mixfa.naggr.discordBot.service

import com.mixfa.naggr.discordBot.model.DiscordNewsSubscriber
import com.mixfa.naggr.newsletter.model.News
import com.mixfa.naggr.newsletter.model.flagsSet
import com.mixfa.naggr.newsletter.service.NewsletterService
import com.mixfa.naggr.utils.LambdaInputHandler
import com.mixfa.naggr.utils.defaultPageable
import com.mixfa.naggr.utils.handle
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Service
import reactor.core.scheduler.Schedulers
import java.time.Duration


private object DiscordEventPredicates {
    fun byFullCommandName(targetName: String): (GenericCommandInteractionEvent) -> Boolean {
        return func@{ event ->
            return@func targetName.contentEquals(event.fullCommandName)
        }
    }
}

@Service
final class DiscordNewsBotService(
    private val discordBot: JDA,
    private val discordSubscribersRepository: DiscordSubscribersRepository,
    newsletterService: NewsletterService
) : ListenerAdapter() {
    private val commandHandlers = listOf(
        LambdaInputHandler(DiscordEventPredicates.byFullCommandName("receive_news"), this::handleReceiveNewsCmd)
    )

    init {
        discordBot.updateCommands()
            .addCommands(
                Commands.slash("receive_news", "Receive news in this channel")
                    .setGuildOnly(true)
            )
            .queue()

        discordBot.addEventListener(this)

        newsletterService.newsFlux
            .bufferTimeout(3, Duration.ofMinutes(5))
            .onErrorContinue { throwable, obj ->
                println(throwable.localizedMessage)
                println(obj)
            }.subscribe(this::broadcastNews)
    }

    private fun broadcastNews(newsList: List<News>) {
        val allFlags = newsList.flagsSet()

        discordSubscribersRepository
            .findAllByTargetFlagsContaining(allFlags, defaultPageable)
            .onErrorContinue { throwable, obj ->
                println(throwable.localizedMessage)
                println(obj)
            }
            .publishOn(Schedulers.parallel())
            .subscribe { subscriber ->
                for (news in newsList) {
                    if (subscriber.targetFlags.none { news.flags.contains(it) })
                        continue

                    discordBot
                        .getTextChannelById(subscriber.channelId)
                        ?.sendMessage(news.textForTelegram)
                        ?.queue()
                }
            }
    }

    private fun handleReceiveNewsCmd(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        discordSubscribersRepository
            .existsByChannelId(event.channelIdLong)
            .subscribe { exists ->
                if (exists) {
                    discordSubscribersRepository.deleteByChannelId(event.channelIdLong).subscribe()
                    event.hook.sendMessage("Unsubscribed from news").queue()
                } else {
                    discordSubscribersRepository.save(
                        DiscordNewsSubscriber(
                            channelId = event.channelIdLong,
                            targetFlags = News.Flag.entries
                        )
                    ).subscribe()
                    event.hook.sendMessage("Subscribed to news").queue()
                }
            }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commandHandlers.handle(event)
    }
}