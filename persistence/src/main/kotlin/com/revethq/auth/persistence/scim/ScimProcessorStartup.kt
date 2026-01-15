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

package com.revethq.auth.persistence.scim

import com.revethq.auth.persistence.scim.processors.ScheduledScimEventProcessor
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

/**
 * Startup bean to initialize the SCIM event processor.
 */
@ApplicationScoped
@Startup
class ScimProcessorStartup(
    private val scheduledProcessor: ScheduledScimEventProcessor,
    @ConfigProperty(name = "revet.scim.enabled", defaultValue = "true")
    private val scimEnabled: Boolean
) {

    companion object {
        private val LOG = Logger.getLogger(ScimProcessorStartup::class.java)
    }

    @PostConstruct
    fun init() {
        if (scimEnabled) {
            scheduledProcessor.start()
            LOG.info("SCIM outbound provisioning is enabled")
        } else {
            LOG.info("SCIM outbound provisioning is disabled")
        }
    }

    @PreDestroy
    fun shutdown() {
        scheduledProcessor.stop()
        LOG.info("SCIM processor shutdown")
    }
}
