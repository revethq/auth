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

package com.revethq.auth.persistence.services

import com.revethq.auth.core.domain.ScimApplication
import com.revethq.auth.core.exceptions.TokenGenerationException
import com.revethq.auth.core.scim.ScimScopes
import com.revethq.auth.core.services.AuthorizationServerService
import com.revethq.auth.persistence.repositories.AuthorizationServerRepository
import com.revethq.auth.persistence.repositories.SigningKeyRepository
import io.smallrye.jwt.algorithm.SignatureAlgorithm
import io.smallrye.jwt.build.Jwt
import io.smallrye.jwt.util.KeyUtils
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.util.Optional

@ApplicationScoped
class ScimTokenService(
    private val authorizationServerRepository: AuthorizationServerRepository,
    private val signingKeyRepository: SigningKeyRepository,
    private val authorizationServerService: AuthorizationServerService,
    @ConfigProperty(name = "revet.scim.token.lifetime", defaultValue = "3600")
    private val defaultTokenLifetime: Long
) : com.revethq.auth.core.services.ScimTokenService {

    companion object {
        private val LOG = Logger.getLogger(ScimTokenService::class.java)
    }

    @Transactional
    override fun generateToken(scimApplication: ScimApplication): String {
        val enabledOperations = scimApplication.enabledOperations ?: emptySet()
        val scopes = ScimScopes.getRequiredScopes(enabledOperations)
        return generateToken(scimApplication, scopes)
    }

    @Transactional
    override fun generateToken(scimApplication: ScimApplication, scopes: Set<String>): String {
        val authorizationServerId = scimApplication.authorizationServerId
            ?: throw IllegalArgumentException("Authorization server ID is required")

        val authorizationServer = authorizationServerRepository.findByIdOptional(authorizationServerId)
            .orElseThrow { IllegalArgumentException("Authorization server not found: $authorizationServerId") }

        val signingKey = authorizationServerService.getSigningKeysForAuthorizationServer(authorizationServerId)

        try {
            val issuer = "${authorizationServer.serverUrl!!.toExternalForm()}/${authorizationServer.id}/"

            val jwt = Jwt
                .issuer(issuer)
                .audience(scimApplication.baseUrl ?: "")
                .subject(scimApplication.applicationId.toString())
                .expiresIn(defaultTokenLifetime)
                .claim("client_id", scimApplication.applicationId.toString())
                .claim("scope", scopes.joinToString(" "))

            jwt.jws()
                .algorithm(
                    when (signingKey.keyType) {
                        else -> SignatureAlgorithm.RS256
                    }
                )
                .keyId(signingKey.id.toString())

            val token = jwt.sign(KeyUtils.decodePrivateKey(signingKey.privateKey))

            LOG.debug("Generated SCIM token for application ${scimApplication.applicationId} with scopes: $scopes")

            return token
        } catch (e: Exception) {
            LOG.error("Failed to generate SCIM token for application ${scimApplication.applicationId}", e)
            throw TokenGenerationException("Failed to generate SCIM token", e)
        }
    }
}
