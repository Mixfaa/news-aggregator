package com.mixfa.naggr.utils

import com.mixfa.naggr.news.model.News


fun News.writeForTelegram(): String {
    return (if (additionalInfo.containsKey("ai_forecast"))
        """
            $link
            ${additionalInfo["ai_forecast"]}
        """ else
        """
             $link
        """).trimIndent()
}
