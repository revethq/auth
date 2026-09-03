package com.revethq.auth.core.api.dto

import jakarta.json.bind.annotation.JsonbNillable
import jakarta.validation.constraints.NotNull
import java.util.UUID

@JsonbNillable(false)
data class ServiceAccountRequest(
    @field:NotNull
    var authorizationServerId: UUID? = null,

    @field:NotNull
    var name: String? = null,

    var description: String? = null,

    var profile: Any? = null,

    var metadata: Metadata? = null,

    var scopes: List<UUID>? = null
)
