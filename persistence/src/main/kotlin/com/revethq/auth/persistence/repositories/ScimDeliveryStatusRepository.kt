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

package com.revethq.auth.persistence.repositories

import com.revethq.auth.core.scim.ScimProvisioningStatus
import com.revethq.auth.persistence.entities.ScimDeliveryStatus
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ScimDeliveryStatusRepository : PanacheRepositoryBase<ScimDeliveryStatus, UUID> {

    /**
     * Find delivery statuses that are pending or ready for retry.
     * Returns status=PENDING or (status=RETRYING and nextRetryAt <= now).
     */
    fun findPendingOrRetryable(): List<ScimDeliveryStatus> {
        val now = OffsetDateTime.now()
        return find(
            "(status = ?1) or (status = ?2 and nextRetryAt <= ?3)",
            Sort.ascending("createdOn"),
            ScimProvisioningStatus.PENDING,
            ScimProvisioningStatus.RETRYING,
            now
        ).list()
    }

    /**
     * Find delivery statuses for a specific SCIM application.
     */
    fun findByScimApplicationId(scimApplicationId: UUID): List<ScimDeliveryStatus> {
        return find("scimApplicationId", Sort.descending("createdOn"), scimApplicationId).list()
    }

    /**
     * Find delivery statuses for a specific event.
     */
    fun findByEventId(eventId: UUID): List<ScimDeliveryStatus> {
        return find("eventId", eventId).list()
    }

    /**
     * Find in-progress deliveries (used for detecting stuck deliveries).
     */
    fun findInProgress(): List<ScimDeliveryStatus> {
        return find("status", ScimProvisioningStatus.IN_PROGRESS).list()
    }

    /**
     * Count pending deliveries for a SCIM application.
     */
    fun countPendingByScimApplicationId(scimApplicationId: UUID): Long {
        return count(
            "scimApplicationId = ?1 and (status = ?2 or status = ?3)",
            scimApplicationId,
            ScimProvisioningStatus.PENDING,
            ScimProvisioningStatus.RETRYING
        )
    }
}
