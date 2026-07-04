package dev.dai.room3poc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform