package com.revethq.auth.core.api.interfaces

import com.revethq.auth.core.api.dto.ServiceAccountRequest
import com.revethq.auth.core.api.dto.ServiceAccountResponse
import com.revethq.auth.core.api.dto.ServiceAccountsResponse
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

@Path("/api/v1/service-accounts")
@Tag(name = "Service Accounts")
interface ServiceAccountsApi {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new service account")
    @APIResponses(
        APIResponse(
            responseCode = "201",
            description = "Service account created successfully",
            content = [Content(schema = Schema(implementation = ServiceAccountResponse::class))]
        ),
        APIResponse(responseCode = "400", description = "Invalid request")
    )
    fun createServiceAccount(@Valid @NotNull serviceAccountRequest: ServiceAccountRequest): Response

    @DELETE
    @Path("/{serviceAccountId}")
    @Operation(summary = "Delete a service account")
    @APIResponses(
        APIResponse(responseCode = "204", description = "Service account deleted successfully"),
        APIResponse(responseCode = "404", description = "Service account not found")
    )
    fun deleteServiceAccount(@PathParam("serviceAccountId") serviceAccountId: UUID): Response

    @GET
    @Path("/{serviceAccountId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a service account by ID")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Service account found",
            content = [Content(schema = Schema(implementation = ServiceAccountResponse::class))]
        ),
        APIResponse(responseCode = "404", description = "Service account not found")
    )
    fun getServiceAccount(@PathParam("serviceAccountId") serviceAccountId: UUID): Response

    @PUT
    @Path("/{serviceAccountId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a service account")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Service account updated successfully",
            content = [Content(schema = Schema(implementation = ServiceAccountResponse::class))]
        ),
        APIResponse(responseCode = "404", description = "Service account not found")
    )
    fun updateServiceAccount(
        @PathParam("serviceAccountId") serviceAccountId: UUID,
        @Valid @NotNull serviceAccountRequest: ServiceAccountRequest
    ): Response

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List service accounts")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "List of service accounts",
            content = [Content(schema = Schema(implementation = ServiceAccountsResponse::class))]
        )
    )
    fun listServiceAccounts(
        @QueryParam("authorizationServerIds") @Parameter(required = false) authorizationServerIds: List<UUID>?,
        @QueryParam("limit") @Parameter(required = false) limit: Int?,
        @QueryParam("offset") @Parameter(required = false) offset: Int?
    ): Response
}
