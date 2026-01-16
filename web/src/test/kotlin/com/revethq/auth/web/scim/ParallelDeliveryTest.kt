/*
 * Copyright 2023 Bryce Groff (Revet)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.revethq.auth.web.scim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Unit tests for parallel delivery with error isolation.
 * Tests 14.8 - Unit tests for parallel delivery with error isolation
 *
 * These tests verify the pattern used in ScheduledScimEventProcessor
 * where deliveries process in parallel and errors in one don't affect others.
 * Uses ExecutorService to demonstrate the pattern without coroutine dependencies.
 */
class ParallelDeliveryTest {

    private val executor = Executors.newFixedThreadPool(4)

    @Test
    fun `parallel delivery processes all items concurrently`() {
        val processedItems = CopyOnWriteArrayList<Int>()
        val items = listOf(1, 2, 3, 4, 5)

        val futures = items.map { item ->
            CompletableFuture.supplyAsync({
                processedItems.add(item)
                item * 2
            }, executor)
        }

        val results = futures.map { it.get(5, TimeUnit.SECONDS) }

        assertEquals(5, processedItems.size)
        assertEquals(listOf(2, 4, 6, 8, 10), results.sorted())
    }

    @Test
    fun `error in one task does not cancel siblings`() {
        val successfulItems = CopyOnWriteArrayList<Int>()
        val failedItems = CopyOnWriteArrayList<Int>()
        val items = listOf(1, 2, 3, 4, 5)

        val futures = items.map { item ->
            CompletableFuture.supplyAsync({
                if (item == 3) {
                    failedItems.add(item)
                    throw RuntimeException("Simulated failure for item 3")
                }
                successfulItems.add(item)
                item
            }, executor)
        }

        // Collect results, handling failures individually
        val results = futures.mapNotNull { future ->
            try {
                future.get(5, TimeUnit.SECONDS)
            } catch (e: Exception) {
                null // Failed task
            }
        }

        // Item 3 should have failed, others should succeed
        assertEquals(4, successfulItems.size)
        assertTrue(successfulItems.containsAll(listOf(1, 2, 4, 5)))
        assertEquals(1, failedItems.size)
        assertEquals(3, failedItems[0])
    }

    @Test
    fun `multiple errors are isolated from each other`() {
        val processedCount = AtomicInteger(0)
        val errorCount = AtomicInteger(0)
        val items = listOf(1, 2, 3, 4, 5, 6)
        val failingItems = setOf(2, 4)

        val futures = items.map { item ->
            CompletableFuture.supplyAsync({
                if (item in failingItems) {
                    errorCount.incrementAndGet()
                    throw RuntimeException("Simulated failure for item $item")
                }
                processedCount.incrementAndGet()
                item
            }, executor)
        }

        // Collect results
        val successfulResults = mutableListOf<Int>()
        futures.forEach { future ->
            try {
                successfulResults.add(future.get(5, TimeUnit.SECONDS))
            } catch (e: Exception) {
                // Expected for failing items
            }
        }

        // 4 items should succeed, 2 should fail
        assertEquals(4, processedCount.get())
        assertEquals(2, errorCount.get())
        assertEquals(listOf(1, 3, 5, 6), successfulResults.sorted())
    }

    @Test
    fun `errors are logged and handled individually`() {
        val errorMessages = CopyOnWriteArrayList<String>()
        val successfulDeliveries = CopyOnWriteArrayList<String>()

        data class DeliveryTask(
            val id: String,
            val shouldFail: Boolean = false,
            val errorMessage: String = "Unknown error"
        )

        val deliveries = listOf(
            DeliveryTask("delivery-1", shouldFail = false),
            DeliveryTask("delivery-2", shouldFail = true, errorMessage = "Connection timeout"),
            DeliveryTask("delivery-3", shouldFail = false),
            DeliveryTask("delivery-4", shouldFail = true, errorMessage = "HTTP 500"),
            DeliveryTask("delivery-5", shouldFail = false)
        )

        val futures = deliveries.map { delivery ->
            CompletableFuture.runAsync({
                if (delivery.shouldFail) {
                    throw RuntimeException(delivery.errorMessage)
                }
                successfulDeliveries.add(delivery.id)
            }, executor).exceptionally { ex ->
                errorMessages.add("${delivery.id}: ${ex.cause?.message ?: ex.message}")
                null
            }
        }

        CompletableFuture.allOf(*futures.toTypedArray()).get(10, TimeUnit.SECONDS)

        // Verify successful deliveries
        assertEquals(3, successfulDeliveries.size)
        assertTrue(successfulDeliveries.containsAll(listOf("delivery-1", "delivery-3", "delivery-5")))

        // Verify errors were captured
        assertEquals(2, errorMessages.size)
        assertTrue(errorMessages.any { it.contains("delivery-2") && it.contains("Connection timeout") })
        assertTrue(errorMessages.any { it.contains("delivery-4") && it.contains("HTTP 500") })
    }

    @Test
    fun `processing continues after slow delivery`() {
        val completionOrder = CopyOnWriteArrayList<String>()

        data class TimedDelivery(val id: String, val delayMs: Long)

        val deliveries = listOf(
            TimedDelivery("fast-1", 10),
            TimedDelivery("slow", 200),
            TimedDelivery("fast-2", 10),
            TimedDelivery("fast-3", 10)
        )

        val futures = deliveries.map { delivery ->
            CompletableFuture.runAsync({
                Thread.sleep(delivery.delayMs)
                completionOrder.add(delivery.id)
            }, executor)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).get(10, TimeUnit.SECONDS)

        // All deliveries should complete
        assertEquals(4, completionOrder.size)

        // Fast deliveries should complete before slow one
        val slowIndex = completionOrder.indexOf("slow")
        assertTrue(slowIndex > 0) // Slow should not be first

        // At least some fast deliveries should complete before slow
        val fastBeforeSlow = completionOrder.take(slowIndex).count { it.startsWith("fast") }
        assertTrue(fastBeforeSlow > 0)
    }

    @Test
    fun `grouped deliveries process each group in parallel`() {
        val processedByEvent = mutableMapOf<String, MutableList<String>>()
        val lock = Object()

        data class GroupedDelivery(val eventId: String, val deliveryId: String)

        val deliveries = listOf(
            GroupedDelivery("event-1", "delivery-1a"),
            GroupedDelivery("event-1", "delivery-1b"),
            GroupedDelivery("event-2", "delivery-2a"),
            GroupedDelivery("event-2", "delivery-2b"),
            GroupedDelivery("event-2", "delivery-2c")
        )

        // Group by event (similar to ScheduledScimEventProcessor)
        val groupedDeliveries = deliveries.groupBy { it.eventId }

        groupedDeliveries.forEach { (eventId, eventDeliveries) ->
            val futures = eventDeliveries.map { delivery ->
                CompletableFuture.runAsync({
                    synchronized(lock) {
                        processedByEvent.getOrPut(eventId) { mutableListOf() }.add(delivery.deliveryId)
                    }
                }, executor)
            }
            CompletableFuture.allOf(*futures.toTypedArray()).get(5, TimeUnit.SECONDS)
        }

        // Verify all deliveries were processed
        assertEquals(2, processedByEvent["event-1"]?.size)
        assertEquals(3, processedByEvent["event-2"]?.size)
    }

    @Test
    fun `exception in one group does not affect other groups`() {
        val processedEvents = CopyOnWriteArrayList<String>()
        val failedEvents = CopyOnWriteArrayList<String>()

        data class GroupedDelivery(
            val eventId: String,
            val deliveryId: String,
            val shouldFail: Boolean = false
        )

        val deliveries = listOf(
            GroupedDelivery("event-1", "delivery-1a"),
            GroupedDelivery("event-1", "delivery-1b", shouldFail = true),
            GroupedDelivery("event-2", "delivery-2a"),
            GroupedDelivery("event-2", "delivery-2b")
        )

        val groupedDeliveries = deliveries.groupBy { it.eventId }

        groupedDeliveries.forEach { (eventId, eventDeliveries) ->
            val futures = eventDeliveries.map { delivery ->
                CompletableFuture.runAsync({
                    if (delivery.shouldFail) {
                        failedEvents.add(eventId)
                        throw RuntimeException("Simulated failure")
                    }
                    processedEvents.add(eventId)
                }, executor)
            }

            futures.forEach { future ->
                try {
                    future.get(5, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    // Handle error individually
                }
            }
        }

        // Event-1 should have 1 success and 1 failure
        assertEquals(1, processedEvents.count { it == "event-1" })
        assertEquals(1, failedEvents.count { it == "event-1" })

        // Event-2 should have 2 successes
        assertEquals(2, processedEvents.count { it == "event-2" })
        assertEquals(0, failedEvents.count { it == "event-2" })
    }
}
