package com.example.kotlinbootlabs.basic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrimitiveTypeTest {
    @Test
    fun testIntegerComparison() {
        val number: Int = 10
        val number1: Integer = Integer(10)
        assertEquals(number, number1.toInt(), "The values should be equal")
        assertEquals(number, number1, "The values should be equal")
    }

    @Test
    fun testSameReference() {
        val number1: Integer = Integer(10)
        val number2: Int = 10
        val result = if (number1.toInt() === number2) "Same" else "Different"
        assertEquals(result, "Same", "The references should be the same")
    }
}