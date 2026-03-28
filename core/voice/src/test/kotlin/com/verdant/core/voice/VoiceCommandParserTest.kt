package com.verdant.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceCommandParserTest {

    private val parser = VoiceCommandParser()

    @Test
    fun `parses done with pattern`() {
        val commands = parser.parse("done with reading")
        assertEquals(1, commands.size)
        assertEquals("reading", commands[0].habitName)
        assertNull(commands[0].value)
    }

    @Test
    fun `parses finished pattern`() {
        val commands = parser.parse("finished meditation")
        assertEquals(1, commands.size)
        assertEquals("meditation", commands[0].habitName)
    }

    @Test
    fun `parses value unit of habit`() {
        val commands = parser.parse("8 glasses of water")
        assertEquals(1, commands.size)
        assertEquals("water", commands[0].habitName)
        assertEquals(8.0, commands[0].value!!, 0.01)
        assertEquals("glasses", commands[0].unit)
    }

    @Test
    fun `parses value unit habit`() {
        val commands = parser.parse("30 minutes yoga")
        assertEquals(1, commands.size)
        assertEquals("yoga", commands[0].habitName)
        assertEquals(30.0, commands[0].value!!, 0.01)
        assertEquals("minutes", commands[0].unit)
    }

    @Test
    fun `parses habit for value unit`() {
        val commands = parser.parse("studied for 2 hours")
        assertEquals(1, commands.size)
        assertEquals("studied", commands[0].habitName)
        assertEquals(2.0, commands[0].value!!, 0.01)
        assertEquals("hours", commands[0].unit)
    }

    @Test
    fun `parses multiple habits with and`() {
        val commands = parser.parse("done with reading and finished meditation")
        assertEquals(2, commands.size)
        assertEquals("reading", commands[0].habitName)
        assertEquals("meditation", commands[1].habitName)
    }

    @Test
    fun `parses multiple habits with comma`() {
        val commands = parser.parse("done with reading, finished exercise")
        assertEquals(2, commands.size)
    }

    @Test
    fun `simple habit name treated as binary`() {
        val commands = parser.parse("meditation")
        assertEquals(1, commands.size)
        assertEquals("meditation", commands[0].habitName)
        assertNull(commands[0].value)
    }

    @Test
    fun `empty string returns empty list`() {
        val commands = parser.parse("")
        assertEquals(0, commands.size)
    }

    @Test
    fun `log prefix is stripped`() {
        val commands = parser.parse("log reading")
        assertEquals(1, commands.size)
        assertEquals("reading", commands[0].habitName)
    }
}
