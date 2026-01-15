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

import com.revethq.auth.core.domain.Scope
import com.revethq.auth.core.scim.ScimOperation
import com.revethq.auth.core.scim.ScimScopes
import com.revethq.auth.core.services.ScopeService
import com.revethq.auth.persistence.repositories.ApplicationRepository
import com.revethq.auth.persistence.repositories.AuthorizationServerRepository
import com.revethq.auth.persistence.repositories.ScopeRepository
import com.revethq.auth.persistence.entities.mappers.AuthorizationServerMapper
import com.revethq.auth.persistence.entities.mappers.ScopeMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.OffsetDateTime
import java.util.UUID
import com.revethq.auth.persistence.entities.Scope as ScopeEntity

@ApplicationScoped
class ScimScopeService(
    private val scopeRepository: ScopeRepository,
    private val authorizationServerRepository: AuthorizationServerRepository,
    private val applicationRepository: ApplicationRepository
) : com.revethq.auth.core.services.ScimScopeService {

    companion object {
        private val LOG = Logger.getLogger(ScimScopeService::class.java)
    }

    @Transactional
    override fun ensureScimScopes(authorizationServerId: UUID): List<Scope> {
        val authServer = authorizationServerRepository.findByIdOptional(authorizationServerId)
            .orElseThrow { IllegalArgumentException("Authorization server not found: $authorizationServerId") }

        val existingScopes = scopeRepository.findAllByAuthorizationServerIdIn(listOf(authorizationServerId))
            .filter { it.name in ScimScopes.ALL_SCOPES }
            .associateBy { it.name }

        val result = mutableListOf<Scope>()

        for (scopeName in ScimScopes.ALL_SCOPES) {
            val existingScope = existingScopes[scopeName]
            if (existingScope != null) {
                result.add(ScopeMapper.from(existingScope))
                LOG.debug("SCIM scope already exists: $scopeName")
            } else {
                val newScope = ScopeEntity().apply {
                    authorizationServer = authServer
                    name = scopeName
                    createdOn = OffsetDateTime.now()
                    updatedOn = OffsetDateTime.now()
                }
                scopeRepository.persist(newScope)
                result.add(ScopeMapper.from(newScope))
                LOG.info("Created SCIM scope: $scopeName for authorization server: $authorizationServerId")
            }
        }

        return result
    }

    @Transactional
    override fun getScimScopes(authorizationServerId: UUID): List<Scope> {
        return scopeRepository.findAllByAuthorizationServerIdIn(listOf(authorizationServerId))
            .filter { it.name in ScimScopes.ALL_SCOPES }
            .map { ScopeMapper.from(it) }
    }

    @Transactional
    override fun getScopesForOperations(authorizationServerId: UUID, operations: Set<ScimOperation>): List<Scope> {
        val requiredScopeNames = ScimScopes.getRequiredScopes(operations)
        return scopeRepository.findAllByAuthorizationServerIdIn(listOf(authorizationServerId))
            .filter { it.name in requiredScopeNames }
            .map { ScopeMapper.from(it) }
    }

    @Transactional
    override fun validateApplicationScopes(applicationId: UUID, operations: Set<ScimOperation>): Boolean {
        val application = applicationRepository.findByIdOptional(applicationId)
            .orElse(null) ?: return false

        val applicationScopeNames = application.scopes.mapNotNull { it.name }.toSet()
        val requiredScopeNames = ScimScopes.getRequiredScopes(operations)

        return applicationScopeNames.containsAll(requiredScopeNames)
    }
}
