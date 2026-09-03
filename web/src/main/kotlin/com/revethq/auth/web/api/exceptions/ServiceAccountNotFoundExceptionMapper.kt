package com.revethq.auth.web.api.exceptions

import com.revethq.auth.core.exceptions.notfound.ServiceAccountNotFound
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ServiceAccountNotFoundExceptionMapper : ExceptionMapper<ServiceAccountNotFound> {
    override fun toResponse(exception: ServiceAccountNotFound): Response {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to exception.message))
            .build()
    }
}
