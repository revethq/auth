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

import com.revethq.auth.core.api.dto.ScimApplicationRequest
import com.revethq.auth.core.api.dto.ScimApplicationsResponse
import com.revethq.auth.core.api.dto.ScimDeliveryStatusesResponse
import com.revethq.auth.core.api.interfaces.ScimApplicationsApi
import com.revethq.auth.core.domain.Page
import com.revethq.auth.core.services.ScimApplicationService
import com.revethq.auth.core.services.ScimDeliveryService
import com.revethq.auth.web.api.routes.Constants.LIMIT_DEFAULT
import com.revethq.auth.web.api.routes.Constants.OFFSET_DEFAULT
import com.revethq.auth.web.api.routes.Pagination
import com.revethq.auth.web.api.routes.mappers.ScimApplicationMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import java.util.UUID

@ApplicationScoped
open class ScimApplicationResource @Inject constructor(
    private val scimApplicationService: ScimApplicationService,
    private val scimDeliveryService: ScimDeliveryService
) : ScimApplicationsApi {

    override fun createScimApplication(scimApplicationRequest: ScimApplicationRequest): Response {
        val scimApplication = ScimApplicationMapper.from(scimApplicationRequest)
        val autoCreate = scimApplicationRequest.autoCreateApplication ?: true

        val result = scimApplicationService.createScimApplication(scimApplication, autoCreate)

        return Response
            .ok()
            .entity(ScimApplicationMapper.toResponse(result.scimApplication, result.applicationSecret))
            .build()
    }

    override fun listScimApplications(
        authorizationServerIds: List<UUID>?,
        limit: Int?,
        offset: Int?
    ): Response {
        val actualLimit = limit ?: LIMIT_DEFAULT
        val actualOffset = offset ?: OFFSET_DEFAULT
        val serverIds = authorizationServerIds ?: emptyList()

        val scimApplications = scimApplicationService.getScimApplications(
            serverIds,
            Page(actualLimit, actualOffset)
        )

        val response = ScimApplicationsResponse(
            scimApplications = scimApplications.map { ScimApplicationMapper.toResponse(it) },
            page = Pagination.getPage("scim-applications", serverIds, actualLimit, actualOffset)
        )

        return Response.ok().entity(response).build()
    }

    override fun getScimApplication(scimApplicationId: UUID): Response {
        val scimApplication = scimApplicationService.getScimApplication(scimApplicationId)

        return Response
            .ok()
            .entity(ScimApplicationMapper.toResponse(scimApplication))
            .build()
    }

    override fun updateScimApplication(
        scimApplicationId: UUID,
        scimApplicationRequest: ScimApplicationRequest
    ): Response {
        val scimApplication = ScimApplicationMapper.from(scimApplicationRequest).copy(id = scimApplicationId)
        val updated = scimApplicationService.updateScimApplication(scimApplication)

        return Response
            .ok()
            .entity(ScimApplicationMapper.toResponse(updated))
            .build()
    }

    override fun deleteScimApplication(scimApplicationId: UUID): Response {
        scimApplicationService.deleteScimApplication(scimApplicationId)
        return Response.noContent().build()
    }

    override fun getDeliveryStatuses(
        scimApplicationId: UUID,
        limit: Int?,
        offset: Int?
    ): Response {
        val actualLimit = limit ?: LIMIT_DEFAULT
        val actualOffset = offset ?: OFFSET_DEFAULT

        // Verify the SCIM application exists
        scimApplicationService.getScimApplication(scimApplicationId)

        val statuses = scimDeliveryService.getDeliveryStatusesByScimApplication(
            scimApplicationId,
            Page(actualLimit, actualOffset)
        )

        val response = ScimDeliveryStatusesResponse(
            deliveryStatuses = statuses.map { ScimApplicationMapper.toDeliveryStatusResponse(it) },
            page = Pagination.getPage(
                "scim-applications/$scimApplicationId/delivery-statuses",
                emptyList(),
                actualLimit,
                actualOffset
            )
        )

        return Response.ok().entity(response).build()
    }
}
