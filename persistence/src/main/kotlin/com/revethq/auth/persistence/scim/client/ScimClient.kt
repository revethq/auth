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

package com.revethq.auth.persistence.scim.client

import com.revethq.auth.core.domain.ScimApplication
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Result of a SCIM HTTP request.
 */
data class ScimClientResponse(
    val statusCode: Int,
    val body: String?,
    val scimResourceId: String? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean
        get() = statusCode in 200..299

    val isRetryable: Boolean
        get() = statusCode in 500..599 || statusCode == 429 || statusCode == 408
}

/**
 * HTTP client for outbound SCIM requests.
 */
@ApplicationScoped
class ScimClient {

    companion object {
        private val LOG = Logger.getLogger(ScimClient::class.java)
        private const val CONTENT_TYPE_SCIM = "application/scim+json"
        private const val CONTENT_TYPE_JSON = "application/json"
    }

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    /**
     * POST request to create a SCIM resource.
     */
    fun createResource(
        scimApplication: ScimApplication,
        token: String,
        resourcePath: String,
        body: String
    ): ScimClientResponse {
        val url = buildUrl(scimApplication.baseUrl!!, resourcePath)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", CONTENT_TYPE_SCIM)
            .header("Accept", CONTENT_TYPE_SCIM)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build()

        return executeRequest(request, "POST", url)
    }

    /**
     * PUT request to replace a SCIM resource.
     */
    fun replaceResource(
        scimApplication: ScimApplication,
        token: String,
        resourcePath: String,
        resourceId: String,
        body: String
    ): ScimClientResponse {
        val url = buildUrl(scimApplication.baseUrl!!, resourcePath, resourceId)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", CONTENT_TYPE_SCIM)
            .header("Accept", CONTENT_TYPE_SCIM)
            .PUT(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build()

        return executeRequest(request, "PUT", url)
    }

    /**
     * PATCH request to modify a SCIM resource.
     */
    fun patchResource(
        scimApplication: ScimApplication,
        token: String,
        resourcePath: String,
        resourceId: String,
        body: String
    ): ScimClientResponse {
        val url = buildUrl(scimApplication.baseUrl!!, resourcePath, resourceId)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Content-Type", CONTENT_TYPE_SCIM)
            .header("Accept", CONTENT_TYPE_SCIM)
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build()

        return executeRequest(request, "PATCH", url)
    }

    /**
     * DELETE request to remove a SCIM resource.
     */
    fun deleteResource(
        scimApplication: ScimApplication,
        token: String,
        resourcePath: String,
        resourceId: String
    ): ScimClientResponse {
        val url = buildUrl(scimApplication.baseUrl!!, resourcePath, resourceId)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", CONTENT_TYPE_SCIM)
            .DELETE()
            .timeout(Duration.ofSeconds(30))
            .build()

        return executeRequest(request, "DELETE", url)
    }

    /**
     * GET request to retrieve a SCIM resource.
     */
    fun getResource(
        scimApplication: ScimApplication,
        token: String,
        resourcePath: String,
        resourceId: String
    ): ScimClientResponse {
        val url = buildUrl(scimApplication.baseUrl!!, resourcePath, resourceId)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer $token")
            .header("Accept", CONTENT_TYPE_SCIM)
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build()

        return executeRequest(request, "GET", url)
    }

    private fun buildUrl(baseUrl: String, resourcePath: String, resourceId: String? = null): String {
        val base = baseUrl.trimEnd('/')
        val path = resourcePath.trimStart('/')
        return if (resourceId != null) {
            "$base/$path/$resourceId"
        } else {
            "$base/$path"
        }
    }

    private fun executeRequest(request: HttpRequest, method: String, url: String): ScimClientResponse {
        return try {
            LOG.debug("Executing SCIM $method request to $url")

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            LOG.debug("SCIM $method response: status=${response.statusCode()}")

            val body = response.body()
            val scimResourceId = if (response.statusCode() == 201 && body != null) {
                extractScimResourceId(body)
            } else null

            val errorMessage = if (!response.statusCode().let { it in 200..299 } && body != null) {
                extractErrorMessage(body)
            } else null

            ScimClientResponse(
                statusCode = response.statusCode(),
                body = body,
                scimResourceId = scimResourceId,
                errorMessage = errorMessage
            )
        } catch (e: Exception) {
            LOG.error("SCIM $method request failed: ${e.message}", e)
            ScimClientResponse(
                statusCode = 0,
                body = null,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Extract the SCIM resource ID from a response body.
     * The ID is typically in the "id" field of the JSON response.
     */
    private fun extractScimResourceId(body: String): String? {
        return try {
            // Simple extraction - look for "id" field
            val regex = """"id"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(body)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            LOG.warn("Failed to extract SCIM resource ID from response", e)
            null
        }
    }

    /**
     * Extract error message from a SCIM error response.
     */
    private fun extractErrorMessage(body: String): String? {
        return try {
            // Look for "detail" field in SCIM error response
            val detailRegex = """"detail"\s*:\s*"([^"]+)"""".toRegex()
            detailRegex.find(body)?.groupValues?.getOrNull(1)
        } catch (e: Exception) {
            null
        }
    }
}
