package com.vamshi.field.ui.util

fun youtubeThumbnailUrl(youtubeId: String): String =
    "https://img.youtube.com/vi/$youtubeId/hqdefault.jpg"

fun youtubeEmbedUrl(youtubeId: String): String =
    "https://www.youtube.com/embed/$youtubeId?autoplay=1&playsinline=1"
