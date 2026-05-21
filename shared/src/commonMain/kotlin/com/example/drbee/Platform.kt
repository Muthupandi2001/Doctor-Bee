package com.example.drbee

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform