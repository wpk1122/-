package com.chrona.ai.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun workNameIncludesTaskId() {
        assertEquals("reminder-42", ReminderWork.uniqueName(42))
    }

    @Test
    fun delayIsClampedToZeroWhenTriggerIsInThePast() {
        assertEquals(0L, ReminderWork.delayMillis(triggerAtMillis = 900, nowMillis = 1_000))
    }

    @Test
    fun delayUsesFutureTriggerDelta() {
        assertEquals(500L, ReminderWork.delayMillis(triggerAtMillis = 1_500, nowMillis = 1_000))
    }
}
