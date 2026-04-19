package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.data.model.FoodLogEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodLogEntryDecoderTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun `decodes full payload with macros`() {
        val raw = """{"id":1,"food_name":"Apple","weight_g":150.0,"calories":52.0,
            "is_healthy":true,"timestamp":"2026-04-16T08:30:00Z",
            "protein_g":0.3,"fat_g":0.2,"sugar_g":10.4,"fiber_g":2.1}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Apple", entry.foodName)
        assertEquals(0.3f, entry.proteinG ?: 0f, 0.01f)
    }

    @Test fun `decodes legacy payload missing macros`() {
        val raw = """{"id":2,"food_name":"Salad","weight_g":300.0,"calories":90.0,
            "is_healthy":1,"timestamp":"2026-04-16T12:00:00Z"}"""
        val entry = json.decodeFromString<FoodLogEntry>(raw)
        assertEquals("Salad", entry.foodName)
        assertNull(entry.proteinG)
    }
}
