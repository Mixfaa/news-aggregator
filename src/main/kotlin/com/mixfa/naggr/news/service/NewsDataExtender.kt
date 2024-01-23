package com.mixfa.naggr.news.service

import com.mixfa.naggr.news.model.News

interface NewsDataExtender {
    fun extend(news: News)
}