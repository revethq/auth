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

package com.revethq.auth.web.api.routes.mappers

import com.revethq.auth.core.api.dto.CredentialRequest
import com.revethq.auth.core.api.dto.CredentialResponse
import com.revethq.auth.core.domain.Credential
import com.revethq.auth.core.domain.CredentialType
import com.revethq.auth.core.domain.Scope

object CredentialMapper {

    @JvmStatic
    fun from(request: CredentialRequest): Credential {
        return Credential(
            userId = request.userId,
            applicationId = request.applicationId,
            authorizationServerId = request.authorizationServerId,
            type = request.type?.let { CredentialType.valueOf(it) },
            credentialValue = request.value,
            name = request.name,
            scopes = request.scopes?.map { Scope(it) } ?: emptyList(),
            expiresIn = request.expiresIn
        )
    }

    @JvmStatic
    fun toResponse(credential: Credential): CredentialResponse {
        return CredentialResponse().apply {
            id = credential.id
            userId = credential.userId
            applicationId = credential.applicationId
            authorizationServerId = credential.authorizationServerId
            type = credential.type?.name
            status = credential.status?.name
            name = credential.name
            credentialValue = credential.credentialValue
            scopes = credential.scopes?.map { ScopeMapper.toResponse(it) } ?: emptyList()
            expiresIn = credential.expiresIn
            createdOn = credential.createdOn
            updatedOn = credential.updatedOn
        }
    }
}
