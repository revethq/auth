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

package com.revethq.auth.core.api.interfaces

import com.revethq.auth.core.api.dto.CredentialRequest
import com.revethq.auth.core.api.dto.CredentialResponse
import com.revethq.auth.core.api.dto.CredentialsResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@Path("/api/v1/credentials")
@Tag(name = "Credentials")
interface CredentialsApi {

    @Operation(summary = "Create a new credential")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Credential created successfully",
            content = [Content(schema = Schema(implementation = CredentialResponse::class))]
        ),
        APIResponse(responseCode = "400", description = "Invalid request")
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createCredential(@Valid @NotNull credentialRequest: CredentialRequest): Response

    @Operation(summary = "Get a credential by ID")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Credential retrieved successfully",
            content = [Content(schema = Schema(implementation = CredentialResponse::class))]
        ),
        APIResponse(responseCode = "404", description = "Credential not found")
    )
    @GET
    @Path("/{credentialId}/")
    @Produces(MediaType.APPLICATION_JSON)
    fun getCredential(@PathParam("credentialId") @Parameter(description = "Credential ID") credentialId: UUID): Response

    @Operation(summary = "Delete a credential by ID")
    @APIResponses(
        APIResponse(responseCode = "204", description = "Credential deleted successfully"),
        APIResponse(responseCode = "404", description = "Credential not found")
    )
    @DELETE
    @Path("/{credentialId}/")
    fun deleteCredential(@PathParam("credentialId") @Parameter(description = "Credential ID") credentialId: UUID): Response

    @Operation(summary = "List credentials with optional filtering")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Credentials retrieved successfully",
            content = [Content(schema = Schema(implementation = CredentialsResponse::class))]
        )
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun listCredentials(
        @QueryParam("principalId") @Parameter(required = false, description = "Filter by principal ID (user or service account)") principalId: UUID?,
        @QueryParam("type") @Parameter(required = false, description = "Filter by credential type") type: String?
    ): Response
}
