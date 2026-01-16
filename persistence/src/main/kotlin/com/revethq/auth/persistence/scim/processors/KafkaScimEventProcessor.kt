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
import org.jboss.logging.Logger

/**
 * Kafka-based SCIM processor (STUB).
 *
 * This processor would consume SCIM events from a Kafka topic.
 * To enable, set revet.scim.processor=kafka and configure Kafka connection.
 *
 * Note: This is a stub implementation for educational purposes.
 * Full implementation would require:
 * - quarkus-smallrye-reactive-messaging-kafka dependency
 * - Kafka connection configuration
 * - Message consumer implementation
 */
@ApplicationScoped
class KafkaScimEventProcessor : ScimEventProcessor {

    companion object {
        private val LOG = Logger.getLogger(KafkaScimEventProcessor::class.java)
    }

    private var running = false

    override suspend fun processEvent(event: ScimRelevantEvent) {
        LOG.debug("Kafka SCIM processor received event: ${event.eventId}")
        // In a full implementation, this would be called by the Kafka consumer
        throw UnsupportedOperationException("Kafka SCIM processor is not implemented")
    }

    override fun start() {
        running = true
        LOG.info("Started KafkaScimEventProcessor (stub - not functional)")
    }

    override fun stop() {
        running = false
        LOG.info("Stopped KafkaScimEventProcessor (stub)")
    }

    override fun isRunning(): Boolean = running
}
