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

import com.revethq.auth.core.domain.Credential
import com.revethq.auth.core.domain.CredentialStatus
import com.revethq.auth.core.domain.CredentialType
import com.revethq.auth.core.services.ScopeService
import com.revethq.auth.persistence.entities.ScopeReference
import com.revethq.auth.persistence.entities.mappers.CredentialMapper
import com.revethq.auth.persistence.repositories.CredentialRepository
import com.revethq.auth.persistence.repositories.ScopeReferenceRepository
import io.quarkus.elytron.security.common.BcryptUtil
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.math.BigInteger
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class CredentialService(
    private val credentialRepository: CredentialRepository,
    private val scopeReferenceRepository: ScopeReferenceRepository,
    private val scopeService: ScopeService
) : com.revethq.auth.core.services.CredentialService {

    @Transactional
    override fun createCredential(credential: Credential): Credential {
        credential.id = UUID.randomUUID()
        credential.createdOn = OffsetDateTime.now()
        credential.updatedOn = OffsetDateTime.now()
        credential.status = CredentialStatus.ACTIVE

        // Hash the credential value
        val plainValue = credential.credentialValue
        if (credential.type == CredentialType.PASSWORD) {
            credential.credentialHash = BcryptUtil.bcryptHash(plainValue)
        } else {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            credential.credentialHash = BigInteger(1, messageDigest.digest(plainValue!!.toByteArray())).toString(16)
        }

        // For PASSWORD type, revoke any existing ACTIVE passwords for the same principal
        if (credential.type == CredentialType.PASSWORD && credential.principalId != null) {
            val existingPasswords = credentialRepository.findActiveByPrincipalIdAndType(
                credential.principalId!!, CredentialType.PASSWORD
            )
            for (existing in existingPasswords) {
                existing.status = CredentialStatus.REVOKED
                existing.updatedOn = OffsetDateTime.now()
                credentialRepository.persist(existing)
            }
        }

        val _credential = CredentialMapper.to(credential)
        credentialRepository.persist(_credential)

        // Persist scope references
        val scopeReferences = credential.scopes.orEmpty()
            .map { scope ->
                val scopeReference = ScopeReference()
                scopeReference.scopeId = scope.id
                scopeReference.resourceId = _credential.id
                scopeReference.scopeReferenceType = ScopeReference.ScopeReferenceType.CREDENTIAL
                scopeReference
            }
        scopeReferenceRepository.persist(scopeReferences)

        // Flush and refresh to get scopes back
        credentialRepository.flush()

        val refreshedCredential = credentialRepository.findById(_credential.id)
        val result = CredentialMapper.from(refreshedCredential)
        result.credentialValue = plainValue
        return result
    }

    @Transactional
    override fun getCredential(credentialId: UUID): Credential {
        return credentialRepository
            .findByIdOptional(credentialId)
            .map { CredentialMapper.from(it) }
            .orElseThrow { RuntimeException("Credential not found") }
    }

    @Transactional
    override fun listCredentials(principalId: UUID?, type: CredentialType?): List<Credential> {
        val results: List<com.revethq.auth.persistence.entities.Credential> = when {
            principalId != null && type != null ->
                credentialRepository.findByPrincipalIdAndType(principalId, type)
            principalId != null ->
                credentialRepository.findByPrincipalId(principalId)
            else ->
                credentialRepository.listAll()
        }
        return results.map { CredentialMapper.from(it) }
    }

    @Transactional
    override fun deleteCredential(credentialId: UUID) {
        val credential = credentialRepository
            .findByIdOptional(credentialId)
            .orElseThrow { RuntimeException("Credential not found") }
        credentialRepository.delete(credential)
    }

    @Transactional
    override fun validate(credentialId: UUID, value: String): Boolean {
        val credential = credentialRepository
            .findByIdOptional(credentialId)
            .orElseThrow { RuntimeException("Credential not found") }

        if (credential.status != CredentialStatus.ACTIVE) {
            return false
        }

        return if (credential.type == CredentialType.PASSWORD) {
            BcryptUtil.matches(value, credential.credentialHash)
        } else {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val valueHash = BigInteger(1, messageDigest.digest(value.toByteArray())).toString(16)
            valueHash == credential.credentialHash
        }
    }
}
