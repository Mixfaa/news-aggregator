package ua.helpme.naggr.discord.service

import jakarta.annotation.PostConstruct
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import ua.helpme.naggr.discord.model.DiscordNewsSubscriber
import ua.helpme.naggr.news.model.News
import ua.helpme.naggr.news.service.NewsService
import ua.helpme.naggr.shared.DiscordEventPredicates
import ua.helpme.naggr.shared.EventHandler
import ua.helpme.naggr.shared.handleEvent
import java.net.URI


@Service
class DiscordService(
    private val discordBot: JDA,
    private val discordSubscribersRepository: DiscordSubscribersRepository,
    private val newsService: NewsService
) : ListenerAdapter() {
    private lateinit var commandHandlers: List<EventHandler<SlashCommandInteractionEvent>>

    @PostConstruct
    fun initialize() {
        discordBot.updateCommands()
            .addCommands(
                Commands.slash("receive_news", "Receive news in this channel")
                    .setGuildOnly(true)
            )
            .queue()

        discordBot.addEventListener(this)

        commandHandlers = listOf(
            EventHandler(DiscordEventPredicates.byFullCommandName("receive_news"), this::handleReceiveNewsCmd)
        )
        newsService.newsFlux.subscribe(this::broadcastNews)
    }

    private fun broadcastNews(news: News) {
        val fileUpload = FileUpload.fromData(URI(news.imageRef).toURL().openStream(), news.imageRef)

        discordSubscribersRepository.findAll()
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
                discordSubscribersRepository.save(DiscordNewsSubscriber(channelId = event.channelIdLong)).subscribe()
                event.hook.sendMessage("Subscribed to news").queue()
            }
            .onErrorComplete()
            .subscribe {
                discordSubscribersRepository.delete(it).subscribe()
                event.hook.sendMessage("Unsubscribed from news").queue()
            }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commandHandlers.handleEvent(event)
    }
}