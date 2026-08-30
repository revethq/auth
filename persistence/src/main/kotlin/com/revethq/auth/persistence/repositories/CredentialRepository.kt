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

import com.revethq.auth.core.domain.CredentialStatus
import com.revethq.auth.core.domain.CredentialType
import com.revethq.auth.persistence.entities.Credential
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class CredentialRepository : PanacheRepositoryBase<Credential, UUID> {
    fun findByUserId(userId: UUID): List<Credential> {
        return list("userId", userId)
    }

    fun findByApplicationId(applicationId: UUID): List<Credential> {
        return list("applicationId", applicationId)
    }

    fun findByUserIdAndType(userId: UUID, type: CredentialType): List<Credential> {
        return list("userId = ?1 and type = ?2", userId, type)
    }

    fun findByApplicationIdAndType(applicationId: UUID, type: CredentialType): List<Credential> {
        return list("applicationId = ?1 and type = ?2", applicationId, type)
    }

    fun findActiveByUserIdAndType(userId: UUID, type: CredentialType): List<Credential> {
        return list("userId = ?1 and type = ?2 and status = ?3", userId, type, CredentialStatus.ACTIVE)
    }
}
