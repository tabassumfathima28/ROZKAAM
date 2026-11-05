package com.rozkaam.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskModelTest {

    @Test
    fun task_defaultValues() {
        val task = Task()
        assertEquals("", task.id)
        assertEquals("", task.title)
        assertEquals("", task.description)
        assertEquals("", task.category)
        assertEquals(0L, task.price)
        assertEquals(0.0, task.priceRupees, 0.001)
        assertEquals("₹0", task.formattedPrice)
        assertEquals(0.0, task.lat, 0.0001)
        assertEquals(0.0, task.lng, 0.0001)
        assertEquals("", task.geohash)
        assertEquals("", task.address)
        assertEquals("", task.posterId)
        assertNull(task.workerId)
        assertEquals("open", task.status)
        assertTrue(task.isOpen)
        assertFalse(task.isAccepted)
        assertFalse(task.isCompleted)
        assertFalse(task.rated)
    }

    @Test
    fun task_priceFormattingAndConversions() {
        val task = Task(
            id = "t_101",
            price = 35000L // ₹350
        )
        assertEquals(35000L, task.price)
        assertEquals(350.0, task.priceRupees, 0.001)
        assertEquals("₹350", task.formattedPrice)
    }

    @Test
    fun task_statusHelpers() {
        assertEquals(true, Task(status = Task.STATUS_OPEN).isOpen)
        assertEquals(true, Task(status = Task.STATUS_ACCEPTED).isAccepted)
        assertEquals(true, Task(status = Task.STATUS_STARTED).isStarted)
        assertEquals(true, Task(status = Task.STATUS_SUBMITTED).isSubmitted)
        assertEquals(true, Task(status = Task.STATUS_COMPLETED).isCompleted)
        assertEquals(true, Task(status = Task.STATUS_CANCELLED).isCancelled)
    }

    @Test
    fun task_fromMap_parsesCorrectly() {
        val map = mapOf(
            "title" to "Move household cartons",
            "description" to "Help shift 10 cartons downstairs to parking.",
            "category" to "moving",
            "price" to 45000L,
            "lat" to 12.9716,
            "lng" to 77.5946,
            "geohash" to "tdr1v9qtj",
            "address" to "Brigade Road, Bengaluru",
            "posterId" to "poster_456",
            "workerId" to "worker_789",
            "status" to "accepted",
            "scheduledFor" to "Today, 5 PM",
            "rated" to false
        )

        val task = Task.fromMap("task_abc", map)
        assertEquals("task_abc", task.id)
        assertEquals("Move household cartons", task.title)
        assertEquals("Help shift 10 cartons downstairs to parking.", task.description)
        assertEquals("moving", task.category)
        assertEquals(45000L, task.price)
        assertEquals(450.0, task.priceRupees, 0.001)
        assertEquals(12.9716, task.lat, 0.0001)
        assertEquals(77.5946, task.lng, 0.0001)
        assertEquals("tdr1v9qtj", task.geohash)
        assertEquals("Brigade Road, Bengaluru", task.address)
        assertEquals("poster_456", task.posterId)
        assertEquals("worker_789", task.workerId)
        assertEquals("accepted", task.status)
        assertTrue(task.isAccepted)
        assertEquals("Today, 5 PM", task.scheduledFor)
        assertFalse(task.rated)
    }

    @Test
    fun taskCategory_fromMap_parsesCorrectly() {
        val map = mapOf(
            "id" to "cleaning",
            "label" to "Home Cleaning",
            "icon" to "cleaning_services"
        )

        val category = TaskCategory.fromMap("cleaning", map)
        assertEquals("cleaning", category.id)
        assertEquals("Home Cleaning", category.label)
        assertEquals("cleaning_services", category.icon)
    }
}

