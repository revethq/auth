package com.revethq.auth.core.api.dto

import jakarta.json.bind.annotation.JsonbNillable
import jakarta.validation.constraints.NotNull

@JsonbNillable(false)
data class ServiceAccountsResponse(
    var serviceAccounts: List<ServiceAccountResponse>? = null,

    @field:NotNull
    var page: Page? = null
)
