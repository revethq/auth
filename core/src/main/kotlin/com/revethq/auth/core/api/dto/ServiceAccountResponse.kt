package com.revethq.auth.core.api.dto

import jakarta.json.bind.annotation.JsonbNillable
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import java.util.UUID

@JsonbNillable(false)
data class ServiceAccountResponse(
    var id: UUID? = null,

    @field:NotNull
    var authorizationServerId: UUID? = null,

    var name: String? = null,

    var description: String? = null,

    var profile: Any? = null,

    var createdOn: OffsetDateTime? = null,

    var updatedOn: OffsetDateTime? = null,

    var metadata: Metadata? = null,

    @field:NotNull
    var scopes: List<ScopeResponse>? = null
)
