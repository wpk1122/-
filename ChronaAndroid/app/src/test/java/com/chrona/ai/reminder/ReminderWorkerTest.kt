package com.chrona.ai.reminder

import android.app.PendingIntent
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderWorkerTest {
    @Test
    fun invalidTaskIdsAreRejected() {
        assertFalse(ReminderNotification.isValidTaskId(-1))
        assertTrue(ReminderNotification.isValidTaskId(0))
    }

    @Test
    fun launchIntentUsesTaskStackFlags() {
        assertEquals(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            ReminderNotification.launchIntentFlags
        )
    }

    @Test
    fun pendingIntentUsesUpdateAndImmutableFlags() {
        assertEquals(
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ReminderNotification.pendingIntentFlags
        )
    }
}
