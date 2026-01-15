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

package com.revethq.auth.core.scim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RetryPolicyTest {

    @Test
    fun `calculateBackoff returns initial backoff for first retry`() {
        val policy = RetryPolicy(initialBackoffMs = 1000L, backoffMultiplier = 2.0)

        val backoff = policy.calculateBackoff(0)

        assertEquals(1000L, backoff)
    }

    @Test
    fun `calculateBackoff applies exponential multiplier`() {
        val policy = RetryPolicy(initialBackoffMs = 1000L, backoffMultiplier = 2.0)

        assertEquals(1000L, policy.calculateBackoff(0))  // 1000 * 2^0 = 1000
        assertEquals(2000L, policy.calculateBackoff(1))  // 1000 * 2^1 = 2000
        assertEquals(4000L, policy.calculateBackoff(2))  // 1000 * 2^2 = 4000
        assertEquals(8000L, policy.calculateBackoff(3))  // 1000 * 2^3 = 8000
        assertEquals(16000L, policy.calculateBackoff(4)) // 1000 * 2^4 = 16000
    }

    @Test
    fun `calculateBackoff respects maxBackoffMs ceiling`() {
        val policy = RetryPolicy(
            initialBackoffMs = 1000L,
            maxBackoffMs = 5000L,
            backoffMultiplier = 2.0
        )

        assertEquals(1000L, policy.calculateBackoff(0))
        assertEquals(2000L, policy.calculateBackoff(1))
        assertEquals(4000L, policy.calculateBackoff(2))
        assertEquals(5000L, policy.calculateBackoff(3))  // Would be 8000, but capped at 5000
        assertEquals(5000L, policy.calculateBackoff(10)) // Still capped
    }

    @Test
    fun `calculateBackoff with custom multiplier`() {
        val policy = RetryPolicy(initialBackoffMs = 100L, backoffMultiplier = 3.0)

        assertEquals(100L, policy.calculateBackoff(0))   // 100 * 3^0 = 100
        assertEquals(300L, policy.calculateBackoff(1))   // 100 * 3^1 = 300
        assertEquals(900L, policy.calculateBackoff(2))   // 100 * 3^2 = 900
        assertEquals(2700L, policy.calculateBackoff(3))  // 100 * 3^3 = 2700
    }

    @Test
    fun `isMaxRetriesExceeded returns false when under limit`() {
        val policy = RetryPolicy(maxRetries = 5)

        assertFalse(policy.isMaxRetriesExceeded(0))
        assertFalse(policy.isMaxRetriesExceeded(1))
        assertFalse(policy.isMaxRetriesExceeded(4))
    }

    @Test
    fun `isMaxRetriesExceeded returns true when at or over limit`() {
        val policy = RetryPolicy(maxRetries = 5)

        assertTrue(policy.isMaxRetriesExceeded(5))
        assertTrue(policy.isMaxRetriesExceeded(6))
        assertTrue(policy.isMaxRetriesExceeded(100))
    }

    @Test
    fun `DEFAULT policy has expected values`() {
        val policy = RetryPolicy.DEFAULT

        assertEquals(5, policy.maxRetries)
        assertEquals(1000L, policy.initialBackoffMs)
        assertEquals(300000L, policy.maxBackoffMs)
        assertEquals(2.0, policy.backoffMultiplier)
    }

    @Test
    fun `DEFAULT policy backoff sequence is correct`() {
        val policy = RetryPolicy.DEFAULT

        // Expected sequence: 1s, 2s, 4s, 8s, 16s, 32s, 64s, 128s, 256s (capped at 300s)
        assertEquals(1000L, policy.calculateBackoff(0))
        assertEquals(2000L, policy.calculateBackoff(1))
        assertEquals(4000L, policy.calculateBackoff(2))
        assertEquals(8000L, policy.calculateBackoff(3))
        assertEquals(16000L, policy.calculateBackoff(4))
        assertEquals(32000L, policy.calculateBackoff(5))
        assertEquals(64000L, policy.calculateBackoff(6))
        assertEquals(128000L, policy.calculateBackoff(7))
        assertEquals(256000L, policy.calculateBackoff(8))
        assertEquals(300000L, policy.calculateBackoff(9)) // Capped at maxBackoffMs
    }
}
