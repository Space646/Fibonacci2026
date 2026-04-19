package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.service.diffFoodLog
import com.fibonacci.fibohealth.data.model.FoodLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

// Helper to make a minimal FoodLogEntry
private fun entry(id: Int) = FoodLogEntry(id, "Food$id", 100f, 50f, true, "2026-04-16T00:00:00Z")

class FoodLogDiffTest {
    @Test fun `new entries are inserted`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1), entry(2)),
            existingIds = emptySet()
        )
        assertEquals(setOf(1, 2), inserts.map { it.id }.toSet())
        assertEquals(emptySet<Int>(), deletes)
    }

    @Test fun `removed entries are deleted`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1)),
            existingIds = setOf(1, 2)
        )
        assertEquals(emptyList<FoodLogEntry>(), inserts)
        assertEquals(setOf(2), deletes)
    }

    @Test fun `no change produces empty diff`() {
        val (inserts, deletes) = diffFoodLog(
            piEntries   = listOf(entry(1)),
            existingIds = setOf(1)
        )
        assertEquals(emptyList<FoodLogEntry>(), inserts)
        assertEquals(emptySet<Int>(), deletes)
    }
}
