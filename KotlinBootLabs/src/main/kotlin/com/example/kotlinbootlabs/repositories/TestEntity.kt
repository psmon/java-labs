package com.example.kotlinbootlabs.repositories

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "test")
data class TestEntity(
    @Id val id: String? = null,
    val name: String,
    val value: Int
)