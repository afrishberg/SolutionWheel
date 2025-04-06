package org.ayala.wheel

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform