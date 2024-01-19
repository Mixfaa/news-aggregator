package com.mixfa.naggr.utils

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
class KeepAliveService {
    private val logger = Logger.getLogger("Keep alive service")

    @Scheduled(fixedRate = 1000 * 60 * 60)
    fun keepAlive() {
        logger.info("Im alive")
    }
}