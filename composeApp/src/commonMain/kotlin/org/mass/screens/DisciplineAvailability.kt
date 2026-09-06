package org.mass.screens

import org.mass.enums.Disciplines

fun isDisciplineAvailable(discipline: Disciplines, isOffline: Boolean): Boolean =
    !isOffline || discipline !in setOf(Disciplines.KERUGI, Disciplines.TANBON)
