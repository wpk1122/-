package com.chrona.ai.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleTaskDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ScheduleTaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.scheduleTaskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndObservePendingTasks() = runTest {
        dao.insert(
            ScheduleTask(
                title = "拿快递",
                note = "根据事项类型使用默认时间",
                startAt = 1_779_433_200_000,
                endAt = 1_779_436_800_000,
                status = TaskStatus.PENDING,
                sourceText = "明天提醒我拿快递",
                createdAt = 1_779_300_000_000,
                updatedAt = 1_779_300_000_000
            )
        )

        val tasks = dao.observeActiveTasks().first()

        assertEquals(1, tasks.size)
        assertEquals("拿快递", tasks.first().title)
        assertEquals(TaskStatus.PENDING, tasks.first().status)
    }
}
