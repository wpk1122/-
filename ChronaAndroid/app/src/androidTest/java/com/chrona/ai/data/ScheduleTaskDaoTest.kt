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
                title = "Pick up package",
                note = "Default time from task type",
                startAt = 1_779_433_200_000,
                endAt = 1_779_436_800_000,
                status = TaskStatus.PENDING,
                sourceText = "Remind me tomorrow to pick up package",
                createdAt = 1_779_300_000_000,
                updatedAt = 1_779_300_000_000
            )
        )

        val tasks = dao.observeActiveTasks().first()

        assertEquals(1, tasks.size)
        assertEquals("Pick up package", tasks.first().title)
        assertEquals(TaskStatus.PENDING, tasks.first().status)
    }

    @Test
    fun observeActiveTasksExcludesDoneAndDeletedTasks() = runTest {
        dao.insert(task(title = "Pending task", status = TaskStatus.PENDING, createdAt = 300))
        dao.insert(task(title = "Done task", status = TaskStatus.DONE, createdAt = 100))
        dao.insert(task(title = "Deleted task", status = TaskStatus.DELETED, createdAt = 200))

        val tasks = dao.observeActiveTasks().first()

        assertEquals(listOf("Pending task"), tasks.map { it.title })
    }

    @Test
    fun markDoneOnlyTransitionsPendingTasks() = runTest {
        val pendingId = dao.insert(task(title = "Pending task", status = TaskStatus.PENDING, createdAt = 100))
        val deletedId = dao.insert(task(title = "Deleted task", status = TaskStatus.DELETED, createdAt = 200))

        val pendingRows = dao.markDone(pendingId, updatedAt = 500)
        val deletedRows = dao.markDone(deletedId, updatedAt = 600)

        assertEquals(1, pendingRows)
        assertEquals(TaskStatus.DONE, dao.getById(pendingId)?.status)
        assertEquals(500, dao.getById(pendingId)?.updatedAt)
        assertEquals(0, deletedRows)
        assertEquals(TaskStatus.DELETED, dao.getById(deletedId)?.status)
        assertEquals(200, dao.getById(deletedId)?.updatedAt)
    }

    @Test
    fun markDeletedIgnoresAlreadyDeletedTasks() = runTest {
        val doneId = dao.insert(task(title = "Done task", status = TaskStatus.DONE, createdAt = 100))
        val deletedId = dao.insert(task(title = "Deleted task", status = TaskStatus.DELETED, createdAt = 200))

        val doneRows = dao.markDeleted(doneId, updatedAt = 500)
        val deletedRows = dao.markDeleted(deletedId, updatedAt = 600)

        assertEquals(1, doneRows)
        assertEquals(TaskStatus.DELETED, dao.getById(doneId)?.status)
        assertEquals(500, dao.getById(doneId)?.updatedAt)
        assertEquals(0, deletedRows)
        assertEquals(TaskStatus.DELETED, dao.getById(deletedId)?.status)
        assertEquals(200, dao.getById(deletedId)?.updatedAt)
    }

    private fun task(
        title: String,
        status: TaskStatus,
        createdAt: Long
    ): ScheduleTask {
        return ScheduleTask(
            title = title,
            note = null,
            startAt = null,
            endAt = null,
            status = status,
            sourceText = title,
            createdAt = createdAt,
            updatedAt = createdAt
        )
    }
}
