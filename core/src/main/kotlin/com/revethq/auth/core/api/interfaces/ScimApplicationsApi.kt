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

import com.revethq.auth.core.api.dto.ScimApplicationRequest
import com.revethq.auth.core.api.dto.ScimApplicationResponse
import com.revethq.auth.core.api.dto.ScimApplicationsResponse
import com.revethq.auth.core.api.dto.ScimDeliveryStatusesResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.util.UUID

@Path("/scim-applications")
@Tag(name = "SCIM Applications", description = "Manage SCIM outbound provisioning configurations")
interface ScimApplicationsApi {

    @Operation(summary = "Create a new SCIM application")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "SCIM application created successfully",
            content = [Content(schema = Schema(implementation = ScimApplicationResponse::class))]
        ),
        APIResponse(responseCode = "400", description = "Invalid request or missing required scopes"),
        APIResponse(responseCode = "404", description = "Authorization server or application not found")
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createScimApplication(@Valid @NotNull scimApplicationRequest: ScimApplicationRequest): Response

    @Operation(summary = "List SCIM applications")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "SCIM applications retrieved successfully",
            content = [Content(schema = Schema(implementation = ScimApplicationsResponse::class))]
        )
    )
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun listScimApplications(
        @QueryParam("authorizationServerIds") authorizationServerIds: List<UUID>?,
        @QueryParam("limit") limit: Int?,
        @QueryParam("offset") offset: Int?
    ): Response

    @Operation(summary = "Get a SCIM application by ID")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "SCIM application found",
            content = [Content(schema = Schema(implementation = ScimApplicationResponse::class))]
        ),
        APIResponse(responseCode = "404", description = "SCIM application not found")
    )
    @GET
    @Path("/{scimApplicationId}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getScimApplication(@PathParam("scimApplicationId") scimApplicationId: UUID): Response

    @Operation(summary = "Update a SCIM application")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "SCIM application updated successfully",
            content = [Content(schema = Schema(implementation = ScimApplicationResponse::class))]
        ),
        APIResponse(responseCode = "400", description = "Invalid request or missing required scopes"),
        APIResponse(responseCode = "404", description = "SCIM application not found")
    )
    @PUT
    @Path("/{scimApplicationId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateScimApplication(
        @PathParam("scimApplicationId") scimApplicationId: UUID,
        @Valid @NotNull scimApplicationRequest: ScimApplicationRequest
    ): Response

    @Operation(summary = "Delete a SCIM application")
    @APIResponses(
        APIResponse(responseCode = "204", description = "SCIM application deleted successfully"),
        APIResponse(responseCode = "404", description = "SCIM application not found")
    )
    @DELETE
    @Path("/{scimApplicationId}")
    fun deleteScimApplication(@PathParam("scimApplicationId") scimApplicationId: UUID): Response

    @Operation(summary = "Get delivery statuses for a SCIM application")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "Delivery statuses retrieved successfully",
            content = [Content(schema = Schema(implementation = ScimDeliveryStatusesResponse::class))]
        ),
        APIResponse(responseCode = "404", description = "SCIM application not found")
    )
    @GET
    @Path("/{scimApplicationId}/deliveries")
    @Produces(MediaType.APPLICATION_JSON)
    fun getDeliveryStatuses(
        @PathParam("scimApplicationId") scimApplicationId: UUID,
        @QueryParam("limit") limit: Int?,
        @QueryParam("offset") offset: Int?
    ): Response
}
