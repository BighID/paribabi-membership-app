package bighid.romatazok.adherentapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform