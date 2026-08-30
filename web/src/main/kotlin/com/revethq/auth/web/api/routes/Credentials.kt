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
package com.revethq.auth.web.api.routes

import com.revethq.auth.core.api.dto.CredentialRequest
import com.revethq.auth.core.api.dto.CredentialsResponse
import com.revethq.auth.core.api.interfaces.CredentialsApi
import com.revethq.auth.core.domain.CredentialType
import com.revethq.auth.core.services.CredentialService
import com.revethq.auth.web.api.routes.mappers.CredentialMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.UUID

@ApplicationScoped
open class Credentials @Inject constructor(
    private val credentialService: CredentialService
) : CredentialsApi {

    override fun createCredential(credentialRequest: CredentialRequest): Response {
        val credential = CredentialMapper.from(credentialRequest)
        return Response
            .status(Response.Status.CREATED)
            .entity(CredentialMapper.toResponse(credentialService.createCredential(credential)))
            .build()
    }

    override fun getCredential(credentialId: UUID): Response {
        return Response
            .ok()
            .entity(CredentialMapper.toResponse(credentialService.getCredential(credentialId)))
            .build()
    }

    override fun deleteCredential(credentialId: UUID): Response {
        credentialService.deleteCredential(credentialId)
        return Response.noContent().build()
    }

    override fun listCredentials(userId: UUID?, applicationId: UUID?, type: String?): Response {
        val credentialType = type?.let { CredentialType.valueOf(it) }
        val credentials = credentialService.listCredentials(userId, applicationId, credentialType)
        val response = CredentialsResponse()
        response.credentials = credentials.map { CredentialMapper.toResponse(it) }
        return Response.ok().entity(response).build()
    }
}
