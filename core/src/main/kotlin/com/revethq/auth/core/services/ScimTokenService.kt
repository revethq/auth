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

package com.revethq.auth.core.services

import com.revethq.auth.core.domain.ScimApplication

/**
 * Service for generating JWT tokens for outbound SCIM requests.
 */
interface ScimTokenService {

    /**
     * Generate a JWT Bearer token for authenticating outbound SCIM requests.
     *
     * The token will include:
     * - iss: Authorization server issuer URL
     * - sub: Application ID
     * - aud: SCIM application base URL
     * - scope: Space-separated SCIM scopes based on enabled operations
     * - client_id: Application ID
     * - iat/exp: Issued at and expiration times
     *
     * @param scimApplication The SCIM application configuration
     * @return JWT token string
     */
    fun generateToken(scimApplication: ScimApplication): String

    /**
     * Generate a JWT Bearer token with specific scopes.
     *
     * @param scimApplication The SCIM application configuration
     * @param scopes The scopes to include in the token
     * @return JWT token string
     */
    fun generateToken(scimApplication: ScimApplication, scopes: Set<String>): String
}
