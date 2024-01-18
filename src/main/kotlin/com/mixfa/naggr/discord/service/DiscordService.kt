package com.mixfa.naggr.discord.service

import com.mixfa.naggr.discord.model.DiscordNewsSubscriber
import com.mixfa.naggr.news.model.News
import com.mixfa.naggr.news.service.NewsService
import com.mixfa.naggr.shared.InputHandler
import com.mixfa.naggr.shared.LambdaInputHandler
import com.mixfa.naggr.shared.handle
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.net.URI

private object DiscordEventPredicates {
    fun byFullCommandName(targetName: String): (GenericCommandInteractionEvent) -> Boolean {
        return func@{ event ->
            return@func targetName.contentEquals(event.fullCommandName)
        }
    }
}

@Service
final class DiscordService(
    private val discordBot: JDA,
    private val discordSubscribersRepository: DiscordSubscribersRepository,
    private val newsService: NewsService
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
        newsService.newsFlux.subscribe(this::broadcastNews)
    }

    private fun broadcastNews(news: News) {
        val fileUpload = FileUpload.fromData(URI(news.imageRef).toURL().openStream(), news.imageRef)

        discordSubscribersRepository
            .findAllByTargetFlagsContaining(news.flags)
            .subscribe {
                discordBot
                    .getTextChannelById(it.channelId)
                    ?.sendMessage(news.caption)
                    ?.setFiles(fileUpload)
                    ?.queue()
            }
    }

    private fun handleReceiveNewsCmd(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()
        discordSubscribersRepository.findByChannelId(event.channelIdLong)
            .publishOn(Schedulers.boundedElastic())
            .switchIfEmpty(Mono.error(Throwable("Not found")))
            .doOnError {
                discordSubscribersRepository.save(
                    DiscordNewsSubscriber(
                        channelId = event.channelIdLong,
                        targetFlags = emptyList()
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