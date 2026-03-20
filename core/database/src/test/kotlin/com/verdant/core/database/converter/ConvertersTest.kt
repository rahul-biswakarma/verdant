package com.verdant.core.database.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    // --- LocalDate <-> Long ---

    @Test
    fun `localDateToLong converts date to epoch day`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals(date.toEpochDay(), converters.localDateToLong(date))
    }

    @Test
    fun `longToLocalDate converts epoch day to LocalDate`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals(date, converters.longToLocalDate(date.toEpochDay()))
    }

    @Test
    fun `localDateToLong returns null for null input`() {
        assertNull(converters.localDateToLong(null))
    }

    @Test
    fun `longToLocalDate returns null for null input`() {
        assertNull(converters.longToLocalDate(null))
    }

    @Test
    fun `localDate roundtrip is lossless`() {
        val original = LocalDate.of(2025, 6, 21)
        val roundTripped = converters.longToLocalDate(converters.localDateToLong(original))
        assertEquals(original, roundTripped)
    }

    @Test
    fun `localDate roundtrip for epoch zero`() {
        val origin = LocalDate.of(1970, 1, 1)
        assertEquals(origin, converters.longToLocalDate(converters.localDateToLong(origin)))
    }

    @Test
    fun `localDate roundtrip for far future date`() {
        val future = LocalDate.of(2099, 12, 31)
        assertEquals(future, converters.longToLocalDate(converters.localDateToLong(future)))
    }

    // --- List<String> <-> String ---

    @Test
    fun `stringListToString joins with comma`() {
        assertEquals("a,b,c", converters.stringListToString(listOf("a", "b", "c")))
    }

    @Test
    fun `stringToStringList splits by comma`() {
        assertEquals(listOf("a", "b", "c"), converters.stringToStringList("a,b,c"))
    }

    @Test
    fun `stringListToString returns empty string for empty list`() {
        assertEquals("", converters.stringListToString(emptyList()))
    }

    @Test
    fun `stringToStringList returns empty list for empty string`() {
        assertEquals(emptyList<String>(), converters.stringToStringList(""))
    }

    @Test
    fun `stringListToString handles single element`() {
        assertEquals("habit-id-123", converters.stringListToString(listOf("habit-id-123")))
    }

    @Test
    fun `stringToStringList handles single element`() {
        assertEquals(listOf("habit-id-123"), converters.stringToStringList("habit-id-123"))
    }

    @Test
    fun `stringList roundtrip is lossless`() {
        val list = listOf("id-1", "id-2", "id-3")
        assertEquals(list, converters.stringToStringList(converters.stringListToString(list)))
    }
}
