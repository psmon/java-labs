package com.example.kotlinbootlabs.repositories

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.UUID

@Document(collection = "test")
data class TestEntity(
    @Id val id: String = UUID.randomUUID().toString(),
    val name: String,
    val value: Int
)