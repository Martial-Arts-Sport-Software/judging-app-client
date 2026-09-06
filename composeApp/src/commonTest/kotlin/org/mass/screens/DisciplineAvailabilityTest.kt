package org.mass.screens

import org.mass.enums.Disciplines
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DisciplineAvailabilityTest {
    @Test
    fun offlineModeBlocksCombatDisciplinesOnly() {
        assertFalse(isDisciplineAvailable(Disciplines.KERUGI, isOffline = true))
        assertFalse(isDisciplineAvailable(Disciplines.TANBON, isOffline = true))
        assertTrue(isDisciplineAvailable(Disciplines.HOSINSOOL, isOffline = true))
    }

    @Test
    fun onlineModeKeepsCombatDisciplinesAvailable() {
        assertTrue(isDisciplineAvailable(Disciplines.KERUGI, isOffline = false))
        assertTrue(isDisciplineAvailable(Disciplines.TANBON, isOffline = false))
    }
}
