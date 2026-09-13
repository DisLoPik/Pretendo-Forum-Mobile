package com.dislopik.pretendo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform