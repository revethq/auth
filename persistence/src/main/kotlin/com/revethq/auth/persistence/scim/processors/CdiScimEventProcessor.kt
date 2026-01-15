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

package com.revethq.auth.persistence.scim.processors

import com.revethq.auth.core.scim.ScimEventProcessor
import com.revethq.auth.core.scim.ScimRelevantEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import kotlinx.coroutines.runBlocking
import org.jboss.logging.Logger

/**
 * CDI Event-based SCIM processor (STUB).
 *
 * This processor observes CDI events and immediately processes them.
 * To enable, set revet.scim.processor=cdi
 *
 * Note: This is a stub implementation for educational purposes.
 * The default ScheduledScimEventProcessor is recommended for production use.
 */
@ApplicationScoped
class CdiScimEventProcessor : ScimEventProcessor {

    companion object {
        private val LOG = Logger.getLogger(CdiScimEventProcessor::class.java)
    }

    private var running = false

    /**
     * CDI event observer for SCIM-relevant events.
     * When enabled, this immediately processes events as they are fired.
     */
    fun onScimEvent(@Observes event: ScimRelevantEvent) {
        if (!running) {
            LOG.trace("CDI SCIM processor is not running, ignoring event")
            return
        }

        runBlocking {
            processEvent(event)
        }
    }

    override suspend fun processEvent(event: ScimRelevantEvent) {
        LOG.debug("CDI SCIM processor received event: ${event.eventId}")
        // In a full implementation, this would process the event immediately
        // For now, this is a stub that relies on the scheduled processor
    }

    override fun start() {
        running = true
        LOG.info("Started CdiScimEventProcessor (stub)")
    }

    override fun stop() {
        running = false
        LOG.info("Stopped CdiScimEventProcessor (stub)")
    }

    override fun isRunning(): Boolean = running
}
