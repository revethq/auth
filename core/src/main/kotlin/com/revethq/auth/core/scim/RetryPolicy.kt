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

import kotlin.math.min
import kotlin.math.pow

/**
 * Configures retry behavior for SCIM delivery attempts.
 * Uses exponential backoff with configurable parameters.
 */
data class RetryPolicy(
    /**
     * Maximum number of retry attempts before marking delivery as FAILED.
     */
    val maxRetries: Int = 5,

    /**
     * Initial backoff delay in milliseconds.
     */
    val initialBackoffMs: Long = 1000L,

    /**
     * Maximum backoff delay in milliseconds (5 minutes).
     */
    val maxBackoffMs: Long = 300000L,

    /**
     * Multiplier applied to backoff delay after each retry.
     */
    val backoffMultiplier: Double = 2.0
) {
    companion object {
        /**
         * Default retry policy with standard exponential backoff settings.
         */
        val DEFAULT = RetryPolicy()
    }

    /**
     * Calculates the backoff delay for a given retry count.
     *
     * @param retryCount The current retry attempt number (0-indexed)
     * @return The delay in milliseconds before the next retry attempt
     */
    fun calculateBackoff(retryCount: Int): Long {
        val backoff = initialBackoffMs * backoffMultiplier.pow(retryCount.toDouble())
        return min(backoff.toLong(), maxBackoffMs)
    }

    /**
     * Checks if the retry count has exceeded the maximum allowed retries.
     *
     * @param retryCount The current retry attempt count
     * @return true if max retries exceeded, false otherwise
     */
    fun isMaxRetriesExceeded(retryCount: Int): Boolean = retryCount >= maxRetries
}
