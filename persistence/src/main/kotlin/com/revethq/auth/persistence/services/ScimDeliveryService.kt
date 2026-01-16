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

package com.revethq.auth.persistence.services

import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.domain.ScimDeliveryStatus
import com.revethq.auth.core.scim.ScimProvisioningStatus
import com.revethq.auth.persistence.entities.mappers.ScimDeliveryStatusMapper
import com.revethq.auth.persistence.repositories.ScimApplicationRepository
import com.revethq.auth.persistence.repositories.ScimDeliveryStatusRepository
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ScimDeliveryService(
    private val scimDeliveryStatusRepository: ScimDeliveryStatusRepository,
    private val scimApplicationRepository: ScimApplicationRepository
) : com.revethq.auth.core.services.ScimDeliveryService {

    companion object {
        private val LOG = Logger.getLogger(ScimDeliveryService::class.java)
    }

    @Transactional
    override fun getDeliveryStatusesByScimApplication(scimApplicationId: UUID, page: Page): List<ScimDeliveryStatus> {
        val allStatuses = scimDeliveryStatusRepository.findByScimApplicationId(scimApplicationId)
        val fromIndex = page.offset().coerceAtMost(allStatuses.size)
        val toIndex = (page.offset() + page.limit()).coerceAtMost(allStatuses.size)
        return allStatuses.subList(fromIndex, toIndex).map { ScimDeliveryStatusMapper.from(it) }
    }

    @Transactional
    override fun getDeliveryStatusesByEvent(eventId: UUID): List<ScimDeliveryStatus> {
        return scimDeliveryStatusRepository.findByEventId(eventId)
            .map { ScimDeliveryStatusMapper.from(it) }
    }

    @Transactional
    override fun getPendingOrRetryableDeliveries(): List<ScimDeliveryStatus> {
        return scimDeliveryStatusRepository.findPendingOrRetryable()
            .map { ScimDeliveryStatusMapper.from(it) }
    }

    @Transactional
    override fun createDeliveryStatus(deliveryStatus: ScimDeliveryStatus): ScimDeliveryStatus {
        deliveryStatus.createdOn = OffsetDateTime.now()
        val entity = ScimDeliveryStatusMapper.to(deliveryStatus)
        scimDeliveryStatusRepository.persist(entity)
        return ScimDeliveryStatusMapper.from(entity)
    }

    @Transactional
    override fun updateDeliveryStatus(deliveryStatus: ScimDeliveryStatus): ScimDeliveryStatus {
        val entity = scimDeliveryStatusRepository.findByIdOptional(deliveryStatus.id)
            .orElseThrow { IllegalArgumentException("Delivery status not found: ${deliveryStatus.id}") }

        entity.status = deliveryStatus.status
        entity.scimResourceId = deliveryStatus.scimResourceId
        entity.httpStatus = deliveryStatus.httpStatus
        entity.retryCount = deliveryStatus.retryCount
        entity.nextRetryAt = deliveryStatus.nextRetryAt
        entity.lastError = deliveryStatus.lastError
        entity.completedOn = deliveryStatus.completedOn

        scimDeliveryStatusRepository.persist(entity)
        return ScimDeliveryStatusMapper.from(entity)
    }

    @Transactional
    override fun createDeliveryStatusesForEvent(eventId: UUID, authorizationServerId: UUID): List<ScimDeliveryStatus> {
        val enabledApps = scimApplicationRepository.findEnabledByAuthorizationServerId(authorizationServerId)

        if (enabledApps.isEmpty()) {
            LOG.debug("No enabled SCIM applications for authorization server: $authorizationServerId")
            return emptyList()
        }

        val createdStatuses = mutableListOf<ScimDeliveryStatus>()

        for (app in enabledApps) {
            val status = ScimDeliveryStatus(
                eventId = eventId,
                scimApplicationId = app.id,
                status = ScimProvisioningStatus.PENDING,
                createdOn = OffsetDateTime.now()
            )

            val entity = ScimDeliveryStatusMapper.to(status)
            scimDeliveryStatusRepository.persist(entity)
            createdStatuses.add(ScimDeliveryStatusMapper.from(entity))

            LOG.debug("Created delivery status for event $eventId to SCIM application ${app.id}")
        }

        return createdStatuses
    }
}
