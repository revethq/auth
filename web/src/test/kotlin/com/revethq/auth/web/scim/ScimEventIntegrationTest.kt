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

import com.revethq.auth.core.domain.AuthorizationServer
import com.revethq.auth.core.domain.Group
import com.revethq.auth.core.domain.GroupMember
import com.revethq.auth.core.domain.Pair
import com.revethq.auth.core.domain.Profile
import com.revethq.auth.core.domain.ResourceType
import com.revethq.auth.core.domain.User
import com.revethq.auth.core.scim.ScimRelevantEvent
import com.revethq.auth.core.services.AuthorizationServerService
import com.revethq.auth.core.services.GroupMemberService
import com.revethq.auth.core.services.GroupService
import com.revethq.auth.core.services.UserService
import com.revethq.iam.user.domain.MemberType
import io.quarkus.test.junit.QuarkusTest
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Integration tests for SCIM event creation and CDI event firing.
 *
 * Tests that creating/updating/deleting Users, Groups, and GroupMembers
 * through the service layer fires the appropriate ScimRelevantEvent CDI events.
 */
@QuarkusTest
class ScimEventIntegrationTest {

    @Inject
    lateinit var authorizationServerService: AuthorizationServerService

    @Inject
    lateinit var userService: UserService

    @Inject
    lateinit var groupService: GroupService

    @Inject
    lateinit var groupMemberService: GroupMemberService

    @Inject
    lateinit var scimEventObserver: ScimEventTestObserver

    private var authServerId: UUID? = null

    @BeforeEach
    fun setUp() {
        scimEventObserver.clear()

        // Create an authorization server for the tests
        val authServer = AuthorizationServer(
            name = "Test Auth Server ${UUID.randomUUID()}",
            serverUrl = URI("https://auth.test.com").toURL(),
            audience = "test-audience"
        )
        val created = authorizationServerService.createAuthorizationServer(authServer)
        authServerId = created.id!!

        // Clear events from auth server creation
        scimEventObserver.clear()
    }

    // =========================================================================
    // User Event Tests
    // =========================================================================

    @Test
    fun `user creation fires ScimRelevantEvent with CREATE type`() {
        val user = User(
            authorizationServerId = authServerId,
            username = "testuser-${UUID.randomUUID()}",
            email = "test@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }

        userService.createUser(Pair(user, profile))

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)

        val scimEvent = events.first()
        assertEquals(ResourceType.USER, scimEvent.resourceType)
        assertEquals(authServerId, scimEvent.authorizationServerId)
        assertEquals("CREATE", scimEvent.eventType)
        assertNotNull(scimEvent.resourceId)
        assertNotNull(scimEvent.eventId)
    }

    @Test
    fun `user update fires ScimRelevantEvent with UPDATE type`() {
        // Create user first
        val user = User(
            authorizationServerId = authServerId,
            username = "updateuser-${UUID.randomUUID()}",
            email = "update@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val created = userService.createUser(Pair(user, profile))
        val userId = created.left!!.id!!

        scimEventObserver.clear()

        // Update the user
        val updatedUser = created.left!!.copy(email = "updated@example.com")
        userService.updateUser(userId, Pair(updatedUser, profile))

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals("UPDATE", events.first().eventType)
        assertEquals(ResourceType.USER, events.first().resourceType)
        assertEquals(userId, events.first().resourceId)
    }

    @Test
    fun `user deletion fires ScimRelevantEvent with DELETE type`() {
        // Create user first
        val user = User(
            authorizationServerId = authServerId,
            username = "deleteuser-${UUID.randomUUID()}",
            email = "delete@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val created = userService.createUser(Pair(user, profile))
        val userId = created.left!!.id!!

        scimEventObserver.clear()

        // Delete the user
        userService.deleteUser(userId)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals("DELETE", events.first().eventType)
        assertEquals(ResourceType.USER, events.first().resourceType)
        assertEquals(userId, events.first().resourceId)
    }

    // =========================================================================
    // Group Event Tests
    // =========================================================================

    @Test
    fun `group creation fires ScimRelevantEvent with CREATE type`() {
        val group = Group(
            authorizationServerId = authServerId,
            displayName = "Test Group ${UUID.randomUUID()}"
        )

        groupService.createGroup(group)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)

        val scimEvent = events.first()
        assertEquals(ResourceType.GROUP, scimEvent.resourceType)
        assertEquals(authServerId, scimEvent.authorizationServerId)
        assertEquals("CREATE", scimEvent.eventType)
        assertNotNull(scimEvent.resourceId)
    }

    @Test
    fun `group update fires ScimRelevantEvent with UPDATE type`() {
        // Create group first
        val group = Group(
            authorizationServerId = authServerId,
            displayName = "Update Group ${UUID.randomUUID()}"
        )
        val created = groupService.createGroup(group)
        val groupId = created.id!!

        scimEventObserver.clear()

        // Update the group
        val updatedGroup = created.copy(displayName = "Updated Group Name")
        groupService.updateGroup(groupId, updatedGroup)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals("UPDATE", events.first().eventType)
        assertEquals(ResourceType.GROUP, events.first().resourceType)
        assertEquals(groupId, events.first().resourceId)
    }

    @Test
    fun `group deletion fires ScimRelevantEvent with DELETE type`() {
        // Create group first
        val group = Group(
            authorizationServerId = authServerId,
            displayName = "Delete Group ${UUID.randomUUID()}"
        )
        val created = groupService.createGroup(group)
        val groupId = created.id!!

        scimEventObserver.clear()

        // Delete the group
        groupService.deleteGroup(groupId)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals("DELETE", events.first().eventType)
        assertEquals(ResourceType.GROUP, events.first().resourceType)
        assertEquals(groupId, events.first().resourceId)
    }

    // =========================================================================
    // Group Member Event Tests
    // =========================================================================

    @Test
    fun `group member creation fires ScimRelevantEvent with CREATE type`() {
        // Create a group and user first
        val group = Group(
            authorizationServerId = authServerId,
            displayName = "Member Group ${UUID.randomUUID()}"
        )
        val createdGroup = groupService.createGroup(group)

        val user = User(
            authorizationServerId = authServerId,
            username = "memberuser-${UUID.randomUUID()}",
            email = "member@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdUser = userService.createUser(Pair(user, profile))

        scimEventObserver.clear()

        // Add member to group
        val groupMember = GroupMember(
            authorizationServerId = authServerId,
            groupId = createdGroup.id!!,
            memberId = createdUser.left!!.id!!,
            memberType = MemberType.USER
        )
        groupMemberService.createGroupMember(groupMember)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)

        val scimEvent = events.first()
        assertEquals(ResourceType.GROUP_MEMBER, scimEvent.resourceType)
        assertEquals(authServerId, scimEvent.authorizationServerId)
        assertEquals("CREATE", scimEvent.eventType)
    }

    @Test
    fun `group member deletion fires ScimRelevantEvent with DELETE type`() {
        // Create a group, user, and membership first
        val group = Group(
            authorizationServerId = authServerId,
            displayName = "Delete Member Group ${UUID.randomUUID()}"
        )
        val createdGroup = groupService.createGroup(group)

        val user = User(
            authorizationServerId = authServerId,
            username = "deletememberuser-${UUID.randomUUID()}",
            email = "deletemember@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        val createdUser = userService.createUser(Pair(user, profile))

        val groupMember = GroupMember(
            authorizationServerId = authServerId,
            groupId = createdGroup.id!!,
            memberId = createdUser.left!!.id!!,
            memberType = MemberType.USER
        )
        val createdMember = groupMemberService.createGroupMember(groupMember)
        val memberId = createdMember.id!!

        scimEventObserver.clear()

        // Delete the membership
        groupMemberService.deleteGroupMember(memberId)

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals("DELETE", events.first().eventType)
        assertEquals(ResourceType.GROUP_MEMBER, events.first().resourceType)
        assertEquals(memberId, events.first().resourceId)
    }

    // =========================================================================
    // Event Metadata Tests
    // =========================================================================

    @Test
    fun `ScimRelevantEvent includes occurred timestamp`() {
        val before = OffsetDateTime.now()

        val user = User(
            authorizationServerId = authServerId,
            username = "timestampuser-${UUID.randomUUID()}",
            email = "timestamp@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        userService.createUser(Pair(user, profile))

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)

        val scimEvent = events.first()
        assertNotNull(scimEvent.occurredAt)
        assertTrue(scimEvent.occurredAt >= before)
    }

    @Test
    fun `ScimRelevantEvent includes event ID`() {
        val user = User(
            authorizationServerId = authServerId,
            username = "eventiduser-${UUID.randomUUID()}",
            email = "eventid@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        userService.createUser(Pair(user, profile))

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertNotNull(events.first().eventId)
    }

    @Test
    fun `ScimRelevantEvent includes authorization server ID`() {
        val user = User(
            authorizationServerId = authServerId,
            username = "authserveruser-${UUID.randomUUID()}",
            email = "authserver@example.com"
        )
        val profile = Profile().apply { this.profile = emptyMap() }
        userService.createUser(Pair(user, profile))

        val events = scimEventObserver.receivedEvents
        assertEquals(1, events.size)
        assertEquals(authServerId, events.first().authorizationServerId)
    }

    // =========================================================================
    // Non-SCIM Event Tests
    // =========================================================================

    @Test
    fun `authorization server creation does not fire ScimRelevantEvent`() {
        scimEventObserver.clear()

        val authServer = AuthorizationServer(
            name = "Non-SCIM Auth Server ${UUID.randomUUID()}",
            serverUrl = URI("https://nonscim.test.com").toURL(),
            audience = "non-scim-audience"
        )
        authorizationServerService.createAuthorizationServer(authServer)

        // AuthorizationServer is not SCIM-relevant
        assertTrue(scimEventObserver.receivedEvents.isEmpty())
    }
}

/**
 * CDI observer bean to capture ScimRelevantEvent events for testing.
 */
@Singleton
class ScimEventTestObserver {
    val receivedEvents: MutableList<ScimRelevantEvent> = CopyOnWriteArrayList()

    fun onScimEvent(@Observes event: ScimRelevantEvent) {
        receivedEvents.add(event)
    }

    fun clear() {
        receivedEvents.clear()
    }
}
