package com.example.kotlinbootlabs

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class KotlinBootLabsApplication

fun main(args: Array<String>) {
	runApplication<KotlinBootLabsApplication>(*args)
}