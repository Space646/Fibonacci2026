package com.fibonacci.fibohealth

import com.fibonacci.fibohealth.service.BleChunkReassembler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BleChunkReassemblerTest {
    @Test fun `single-chunk payload returns immediately`() {
        val r = BleChunkReassembler()
        val payload = "hello".toByteArray()
        val frame = byteArrayOf(0xFB.toByte(), 0, 1) + payload
        assertEquals("hello", r.feed(frame)?.toString(Charsets.UTF_8))
    }

    @Test fun `two chunks reassemble in order`() {
        val r = BleChunkReassembler()
        assertNull(r.feed(byteArrayOf(0xFB.toByte(), 0, 2) + "hel".toByteArray()))
        assertEquals("hello", r.feed(byteArrayOf(0xFB.toByte(), 1, 2) + "lo".toByteArray())?.toString(Charsets.UTF_8))
    }

    @Test fun `non-framed data returns as-is`() {
        val r = BleChunkReassembler()
        val raw = "[1,2,3]".toByteArray()
        assertEquals("[1,2,3]", r.feed(raw)?.toString(Charsets.UTF_8))
    }
}
