package com.mixfa.naggr.discordBot.service

import com.mixfa.naggr.discordBot.model.DiscordSubscriber
import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.model.flagsSet
import com.mixfa.naggr.news.service.NewsletterService
import com.mixfa.naggr.utils.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import reactor.core.publisher.Mono
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
final class DiscordBotService(
    private val discordBot: JDA,
    private val discordSubscribersRepository: DiscordSubscribersRepository,
    private val newsletterService: NewsletterService
) : ListenerAdapter() {
    private val commandHandlers: List<InputHandler<SlashCommandInteractionEvent, Unit>>

    init {
        discordBot.updateCommands()
            .addCommands(
                Commands.slash("receive_news", "Receive news in this channel")
                    .setGuildOnly(true)
            )
            .queue()

        discordBot.addEventListener(this)

        commandHandlers = listOf(
            LambdaInputHandler(DiscordEventPredicates.byFullCommandName("receive_news"), this::handleReceiveNewsCmd)
        )

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
        discordSubscribersRepository.findByChannelId(event.channelIdLong)
            .publishOn(Schedulers.boundedElastic())
            .switchIfEmpty(Mono.error(EmptyMonoError))
            .doOnError {
                discordSubscribersRepository.save(
                    DiscordSubscriber(
                        channelId = event.channelIdLong,
                        targetFlags = News.Flag.entries
                    )
                ).subscribe()
                event.hook.sendMessage("Subscribed to news").queue()
            }
            .onErrorComplete()
            .subscribe {
                discordSubscribersRepository.delete(it).subscribe()
                event.hook.sendMessage("Unsubscribed from news").queue()
            }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commandHandlers.handle(event)
    }
}