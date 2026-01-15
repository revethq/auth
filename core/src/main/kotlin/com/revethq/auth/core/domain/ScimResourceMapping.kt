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

package com.revethq.auth.core.domain

import java.time.OffsetDateTime
import java.util.UUID

/**
 * Maps a local Revet Auth resource (User, Group) to its corresponding
 * resource ID in a downstream SCIM server.
 *
 * This mapping is necessary because downstream SCIM servers assign their own
 * unique identifiers to resources. For UPDATE and DELETE operations, we need
 * to know the downstream resource ID.
 */
data class ScimResourceMapping(
    var id: UUID? = null,

    /**
     * The SCIM application this mapping belongs to.
     */
    var scimApplicationId: UUID? = null,

    /**
     * The type of local resource (USER, GROUP).
     */
    var localResourceType: ResourceType? = null,

    /**
     * The local resource ID in Revet Auth.
     */
    var localResourceId: UUID? = null,

    /**
     * The resource ID assigned by the downstream SCIM server.
     */
    var scimResourceId: String? = null,

    var createdOn: OffsetDateTime? = null
)
