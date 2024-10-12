package com.example.kotlinbootlabs.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {

    @GetMapping("/api/hello")
    fun sayHello(@RequestParam name: String): String {
        return if (name == "hello") "kotlin" else "unknown"
    }
}