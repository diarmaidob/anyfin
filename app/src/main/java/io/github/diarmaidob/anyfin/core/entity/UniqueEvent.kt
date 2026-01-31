package io.github.diarmaidob.anyfin.core.entity

import java.util.UUID

data class UniqueEvent<out T>(
    val id: String = UUID.randomUUID().toString(),
    val content: T
)