package org.example.kotlinbootreactivelabs.controller

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Controller")
class ChatController {

    @PostMapping("/add-custom-field")
    @Operation(summary = "Add a custom field to a conversation")
    fun addCustomField(): String {
        // Implementation here
        return "Custom field added"
    }

    @PostMapping("/add-customer")
    @Operation(summary = "Add or update a customer using mobile number and pageid")
    fun addCustomer(): String {
        // Implementation here
        return "Customer added or updated"
    }

    @PostMapping("/add-tag")
    @Operation(summary = "Add a new tag to a customer conversation")
    fun addTag(): String {
        // Implementation here
        return "Tag added"
    }

    @GetMapping("/fetch-chats")
    @Operation(summary = "Fetch the last 10 messages interacted between Customer and Channel")
    fun fetchChats(): String {
        // Implementation here
        return "Fetched last 10 messages"
    }

    @GetMapping("/fetch-conversations")
    @Operation(summary = "Fetch all conversations by channel")
    fun fetchConversations(): String {
        // Implementation here
        return "Fetched conversations"
    }

    @GetMapping("/fetch-tags")
    @Operation(summary = "Fetch all custom fields associated with a specific conversation")
    fun fetchTags(): String {
        // Implementation here
        return "Fetched custom fields"
    }

    @GetMapping("/fetch-kakao-templates")
    @Operation(summary = "Fetch all Kakao templates designed within the merchant account")
    fun fetchKakaoTemplates(): String {
        // Implementation here
        return "Fetched Kakao templates"
    }

    @GetMapping("/get-conversation")
    @Operation(summary = "Fetch the conversation for the provided conversation ID")
    fun getConversation(): String {
        // Implementation here
        return "Fetched conversation"
    }

    @GetMapping("/get-message")
    @Operation(summary = "Retrieve sent message and message information")
    fun getMessage(): String {
        // Implementation here
        return "Fetched message"
    }

    @GetMapping("/get-social-channels")
    @Operation(summary = "Fetch connected social channels of the merchant")
    fun getSocialChannels(): String {
        // Implementation here
        return "Fetched social channels"
    }

    @GetMapping("/get-kakao-template")
    @Operation(summary = "Get more info for the selected Kakao template")
    fun getKakaoTemplate(): String {
        // Implementation here
        return "Fetched Kakao template info"
    }

    @PostMapping("/send-message")
    @Operation(summary = "Send a message using Conversation ID and Unique connection IDs")
    fun sendMessage(): String {
        // Implementation here
        return "Message sent"
    }
}