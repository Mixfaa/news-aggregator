package com.mixfa.naggr.newsletter.service

import com.mixfa.naggr.newsletter.model.News

interface NewsDataExtender {
    fun extend(news: News)
}