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

package com.revethq.auth.core.scim

/**
 * Defines the SCIM operations that can be enabled for a ScimApplication.
 * Each operation maps to a specific SCIM API endpoint and HTTP method.
 */
enum class ScimOperation {
    // User operations
    CREATE_USER,       // POST /Users
    UPDATE_USER,       // PUT /Users/{id}
    DEACTIVATE_USER,   // PATCH /Users/{id} with active=false
    DELETE_USER,       // DELETE /Users/{id}

    // Group operations
    CREATE_GROUP,      // POST /Groups
    UPDATE_GROUP,      // PUT /Groups/{id}
    DELETE_GROUP,      // DELETE /Groups/{id}

    // Group membership operations
    ADD_GROUP_MEMBER,    // PATCH /Groups/{id} - add member
    REMOVE_GROUP_MEMBER  // PATCH /Groups/{id} - remove member
}
