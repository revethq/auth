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

import com.revethq.auth.core.domain.Group
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ResourceType
import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.domain.ScimDeliveryStatus
import com.revethq.auth.core.domain.User
import com.revethq.auth.core.scim.ScimDeleteAction
import com.revethq.auth.core.scim.ScimEventProcessor
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.scim.ScimProvisioningStatus
import com.revethq.auth.core.scim.ScimRelevantEvent
import com.revethq.auth.core.services.ScimDeliveryService
import com.revethq.auth.core.services.ScimTokenService
import com.revethq.auth.persistence.entities.Event
import com.revethq.auth.persistence.entities.EventType
import com.revethq.auth.persistence.entities.mappers.ScimApplicationMapper
import com.revethq.auth.persistence.repositories.EventRepository
import com.revethq.auth.persistence.repositories.ScimApplicationRepository
import com.revethq.auth.persistence.scim.client.ScimClientResponse
import com.revethq.auth.persistence.scim.operations.ScimGroupOperations
import com.revethq.auth.persistence.scim.operations.ScimResourceMappingService
import com.revethq.auth.persistence.scim.operations.ScimUserOperations
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Scheduled SCIM event processor that polls for pending deliveries.
 */
@ApplicationScoped
class ScheduledScimEventProcessor(
    private val scimApplicationRepository: ScimApplicationRepository,
    private val scimDeliveryService: ScimDeliveryService,
    private val scimTokenService: ScimTokenService,
    private val scimUserOperations: ScimUserOperations,
    private val scimGroupOperations: ScimGroupOperations,
    private val scimResourceMappingService: ScimResourceMappingService,
    private val eventRepository: EventRepository,
    @param:ConfigProperty(name = "revet.scim.enabled", defaultValue = "true")
    private val scimEnabled: Boolean
) : ScimEventProcessor {

    companion object {
        private val LOG = Logger.getLogger(ScheduledScimEventProcessor::class.java)
    }

    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override suspend fun processEvent(event: ScimRelevantEvent) {
        if (!scimEnabled) {
            LOG.debug("SCIM processing is disabled")
            return
        }

        LOG.debug("Processing SCIM event: ${event.eventId} for resource ${event.resourceType}:${event.resourceId}")

        // Create delivery statuses for all enabled SCIM applications
        scimDeliveryService.createDeliveryStatusesForEvent(event.eventId, event.authorizationServerId)
    }

    override fun start() {
        running.set(true)
        LOG.info("Started ScheduledScimEventProcessor")
    }

    override fun stop() {
        running.set(false)
        LOG.info("Stopped ScheduledScimEventProcessor")
    }

    override fun isRunning(): Boolean = running.get()

    /**
     * Scheduled polling for pending SCIM deliveries.
     * Runs every 5 seconds by default.
     */
    @Scheduled(every = "\${revet.scim.scheduled.interval:5s}")
    fun pollAndProcess() {
        if (!scimEnabled || !running.get()) {
            return
        }

        try {
            val pendingDeliveries = scimDeliveryService.getPendingOrRetryableDeliveries()

            if (pendingDeliveries.isEmpty()) {
                return
            }

            LOG.debug("Found ${pendingDeliveries.size} pending SCIM deliveries")

            // Group by event ID and process in parallel
            val groupedDeliveries = pendingDeliveries.groupBy { it.eventId }

            runBlocking {
                groupedDeliveries.forEach { (eventId, deliveries) ->
                    deliveries.map { delivery ->
                        // Use async with SupervisorJob for error isolation
                        scope.async {
                            try {
                                processDelivery(delivery)
                            } catch (e: Exception) {
                                LOG.error("Error processing delivery ${delivery.id}: ${e.message}", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Error in SCIM poll and process: ${e.message}", e)
        }
    }

    /**
     * Process a single delivery.
     */
    @Transactional
    fun processDelivery(delivery: ScimDeliveryStatus) {
        val scimAppEntity = scimApplicationRepository.findByIdOptional(delivery.scimApplicationId!!)
            .orElse(null)

        if (scimAppEntity == null) {
            LOG.warn("SCIM application not found for delivery ${delivery.id}")
            markDeliveryFailed(delivery, "SCIM application not found")
            return
        }

        val scimApplication = ScimApplicationMapper.from(scimAppEntity)

        if (!scimApplication.enabled) {
            LOG.debug("SCIM application ${scimApplication.id} is disabled, skipping delivery")
            markDeliveryFailed(delivery, "SCIM application is disabled")
            return
        }

        // Get the event
        val event = eventRepository.findById(delivery.eventId!!)
        if (event == null) {
            LOG.warn("Event not found for delivery ${delivery.id}")
            markDeliveryFailed(delivery, "Event not found")
            return
        }

        // Mark as in progress
        delivery.status = ScimProvisioningStatus.IN_PROGRESS
        scimDeliveryService.updateDeliveryStatus(delivery)

        try {
            // Generate fresh JWT for this request
            val token = scimTokenService.generateToken(scimApplication)

            // Execute the appropriate SCIM operation
            val response = executeScimOperation(scimApplication, token, event, delivery)

            if (response.isSuccess) {
                handleSuccess(delivery, scimApplication, event, response)
            } else if (response.isRetryable) {
                handleRetry(delivery, scimApplication, response)
            } else {
                markDeliveryFailed(delivery, response.errorMessage ?: "HTTP ${response.statusCode}")
            }
        } catch (e: Exception) {
            LOG.error("Error executing SCIM operation for delivery ${delivery.id}: ${e.message}", e)
            handleRetry(delivery, scimApplication, ScimClientResponse(0, null, errorMessage = e.message))
        }
    }

    private fun executeScimOperation(
        scimApplication: ScimApplication,
        token: String,
        event: Event,
        delivery: ScimDeliveryStatus
    ): ScimClientResponse {
        val resourceType = event.resourceType ?: return ScimClientResponse(0, null, errorMessage = "Unknown resource type")
        val eventType = event.eventType ?: return ScimClientResponse(0, null, errorMessage = "Unknown event type")
        val resourceData = event.resource ?: emptyMap()

        return when (resourceType) {
            ResourceType.USER -> executeUserOperation(scimApplication, token, event, delivery, eventType, resourceData)
            ResourceType.GROUP -> executeGroupOperation(scimApplication, token, event, delivery, eventType, resourceData)
            ResourceType.GROUP_MEMBER -> executeGroupMemberOperation(scimApplication, token, event, delivery, eventType, resourceData)
            else -> ScimClientResponse(0, null, errorMessage = "Unsupported resource type: $resourceType")
        }
    }

    private fun executeUserOperation(
        scimApplication: ScimApplication,
        token: String,
        event: Event,
        delivery: ScimDeliveryStatus,
        eventType: EventType,
        resourceData: Map<String, Any>
    ): ScimClientResponse {
        val enabledOps = scimApplication.enabledOperations ?: emptySet()

        // Deserialize to domain objects
        val (user, profile) = deserializeUserData(resourceData)
        if (user == null) {
            return ScimClientResponse(0, null, errorMessage = "Failed to deserialize user data")
        }

        return when (eventType) {
            EventType.CREATE -> {
                if (ScimOperation.CREATE_USER !in enabledOps) {
                    return ScimClientResponse(200, null) // Skip
                }
                scimUserOperations.createUser(scimApplication, token, user, profile)
            }
            EventType.UPDATE -> {
                if (ScimOperation.UPDATE_USER !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                val scimResourceId = scimResourceMappingService.getScimResourceId(
                    scimApplication.id!!,
                    ResourceType.USER,
                    event.resourceId!!
                ) ?: return ScimClientResponse(0, null, errorMessage = "No SCIM mapping found for user")

                scimUserOperations.updateUser(scimApplication, token, scimResourceId, user, profile)
            }
            EventType.DELETE -> {
                val scimResourceId = scimResourceMappingService.getScimResourceId(
                    scimApplication.id!!,
                    ResourceType.USER,
                    event.resourceId!!
                ) ?: return ScimClientResponse(200, null) // No mapping means nothing to delete

                if (scimApplication.deleteAction == ScimDeleteAction.DEACTIVATE) {
                    if (ScimOperation.DEACTIVATE_USER !in enabledOps) {
                        return ScimClientResponse(200, null)
                    }
                    scimUserOperations.deactivateUser(scimApplication, token, scimResourceId)
                } else {
                    if (ScimOperation.DELETE_USER !in enabledOps) {
                        return ScimClientResponse(200, null)
                    }
                    scimUserOperations.deleteUser(scimApplication, token, scimResourceId)
                }
            }
        }
    }

    private fun executeGroupOperation(
        scimApplication: ScimApplication,
        token: String,
        event: Event,
        delivery: ScimDeliveryStatus,
        eventType: EventType,
        resourceData: Map<String, Any>
    ): ScimClientResponse {
        val enabledOps = scimApplication.enabledOperations ?: emptySet()

        // Deserialize to domain object
        val group = deserializeGroupData(resourceData)
        if (group == null) {
            return ScimClientResponse(0, null, errorMessage = "Failed to deserialize group data")
        }

        return when (eventType) {
            EventType.CREATE -> {
                if (ScimOperation.CREATE_GROUP !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                scimGroupOperations.createGroup(scimApplication, token, group)
            }
            EventType.UPDATE -> {
                if (ScimOperation.UPDATE_GROUP !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                val scimResourceId = scimResourceMappingService.getScimResourceId(
                    scimApplication.id!!,
                    ResourceType.GROUP,
                    event.resourceId!!
                ) ?: return ScimClientResponse(0, null, errorMessage = "No SCIM mapping found for group")

                scimGroupOperations.updateGroup(scimApplication, token, scimResourceId, group)
            }
            EventType.DELETE -> {
                if (ScimOperation.DELETE_GROUP !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                val scimResourceId = scimResourceMappingService.getScimResourceId(
                    scimApplication.id!!,
                    ResourceType.GROUP,
                    event.resourceId!!
                ) ?: return ScimClientResponse(200, null)

                scimGroupOperations.deleteGroup(scimApplication, token, scimResourceId)
            }
        }
    }

    /**
     * Deserializes user data from Event resource map to User and Profile domain objects.
     */
    private fun deserializeUserData(resourceData: Map<String, Any>): Pair<User?, Profile?> {
        @Suppress("UNCHECKED_CAST")
        val userData = resourceData["user"] as? Map<String, Any> ?: return Pair(null, null)

        val user = User(
            id = extractUuid(userData, "id"),
            username = userData["username"] as? String,
            email = userData["email"] as? String,
            authorizationServerId = extractUuid(userData, "authorizationServerId")
        )

        @Suppress("UNCHECKED_CAST")
        val profileData = resourceData["profile"] as? Map<String, Any>
        val profile = if (profileData != null) {
            Profile(profile = profileData)
        } else {
            null
        }

        return Pair(user, profile)
    }

    /**
     * Deserializes group data from Event resource map to Group domain object.
     */
    private fun deserializeGroupData(resourceData: Map<String, Any>): Group? {
        @Suppress("UNCHECKED_CAST")
        val groupData = resourceData["group"] as? Map<String, Any> ?: return null

        return Group(
            id = extractUuid(groupData, "id"),
            displayName = groupData["displayName"] as? String,
            authorizationServerId = extractUuid(groupData, "authorizationServerId")
        )
    }

    private fun executeGroupMemberOperation(
        scimApplication: ScimApplication,
        token: String,
        event: Event,
        delivery: ScimDeliveryStatus,
        eventType: EventType,
        resourceData: Map<String, Any>
    ): ScimClientResponse {
        val enabledOps = scimApplication.enabledOperations ?: emptySet()

        @Suppress("UNCHECKED_CAST")
        val memberData = resourceData["groupMember"] as? Map<String, Any> ?: resourceData

        // Extract group and user IDs
        val groupId = extractUuid(memberData, "groupId")
        val userId = extractUuid(memberData, "userId")

        if (groupId == null || userId == null) {
            return ScimClientResponse(0, null, errorMessage = "Missing group or user ID in member data")
        }

        // Get SCIM IDs for group and user
        val groupScimId = scimResourceMappingService.getScimResourceId(
            scimApplication.id!!,
            ResourceType.GROUP,
            groupId
        ) ?: return ScimClientResponse(0, null, errorMessage = "No SCIM mapping found for group")

        val userScimId = scimResourceMappingService.getScimResourceId(
            scimApplication.id!!,
            ResourceType.USER,
            userId
        ) ?: return ScimClientResponse(0, null, errorMessage = "No SCIM mapping found for user")

        return when (eventType) {
            EventType.CREATE -> {
                if (ScimOperation.ADD_GROUP_MEMBER !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                scimGroupOperations.addMember(scimApplication, token, groupScimId, userScimId)
            }
            EventType.DELETE -> {
                if (ScimOperation.REMOVE_GROUP_MEMBER !in enabledOps) {
                    return ScimClientResponse(200, null)
                }
                scimGroupOperations.removeMember(scimApplication, token, groupScimId, userScimId)
            }
            EventType.UPDATE -> {
                // Group member updates are not typically supported
                ScimClientResponse(200, null)
            }
        }
    }

    private fun extractUuid(data: Map<String, Any>, key: String): UUID? {
        return when (val value = data[key]) {
            is UUID -> value
            is String -> try { UUID.fromString(value) } catch (e: Exception) { null }
            else -> null
        }
    }

    private fun handleSuccess(
        delivery: ScimDeliveryStatus,
        scimApplication: ScimApplication,
        event: Event,
        response: ScimClientResponse
    ) {
        delivery.status = ScimProvisioningStatus.SUCCESS
        delivery.httpStatus = response.statusCode
        delivery.scimResourceId = response.scimResourceId
        delivery.completedOn = OffsetDateTime.now()

        scimDeliveryService.updateDeliveryStatus(delivery)

        // Save resource mapping for CREATE operations
        if (event.eventType == EventType.CREATE && response.scimResourceId != null) {
            val resourceType = event.resourceType
            if (resourceType == ResourceType.USER || resourceType == ResourceType.GROUP) {
                scimResourceMappingService.saveMapping(
                    scimApplication.id!!,
                    resourceType,
                    event.resourceId!!,
                    response.scimResourceId!!
                )
            }
        }

        // Delete mapping for DELETE operations
        if (event.eventType == EventType.DELETE) {
            val resourceType = event.resourceType
            if (resourceType == ResourceType.USER || resourceType == ResourceType.GROUP) {
                scimResourceMappingService.deleteMapping(
                    scimApplication.id!!,
                    resourceType,
                    event.resourceId!!
                )
            }
        }

        LOG.debug("SCIM delivery ${delivery.id} completed successfully")
    }

    private fun handleRetry(
        delivery: ScimDeliveryStatus,
        scimApplication: ScimApplication,
        response: ScimClientResponse
    ) {
        val retryPolicy = scimApplication.retryPolicy

        if (retryPolicy.isMaxRetriesExceeded(delivery.retryCount)) {
            markDeliveryFailed(delivery, response.errorMessage ?: "Max retries exceeded")
            return
        }

        val backoffMs = retryPolicy.calculateBackoff(delivery.retryCount)
        val nextRetry = OffsetDateTime.now().plusNanos(backoffMs * 1_000_000)

        delivery.status = ScimProvisioningStatus.RETRYING
        delivery.httpStatus = response.statusCode
        delivery.retryCount = delivery.retryCount + 1
        delivery.nextRetryAt = nextRetry
        delivery.lastError = response.errorMessage

        scimDeliveryService.updateDeliveryStatus(delivery)

        LOG.debug("SCIM delivery ${delivery.id} scheduled for retry ${delivery.retryCount} at $nextRetry")
    }

    private fun markDeliveryFailed(delivery: ScimDeliveryStatus, error: String) {
        delivery.status = ScimProvisioningStatus.FAILED
        delivery.lastError = error.take(1000) // Truncate to fit column
        delivery.completedOn = OffsetDateTime.now()

        scimDeliveryService.updateDeliveryStatus(delivery)

        LOG.warn("SCIM delivery ${delivery.id} failed: $error")
    }
}
