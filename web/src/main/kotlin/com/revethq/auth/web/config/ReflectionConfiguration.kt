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

package com.revethq.auth.web.config

import com.revethq.core.Identifier
import com.revethq.core.Metadata
import com.revethq.core.SchemaValidation
import com.revethq.iam.user.domain.MemberType
import com.revethq.iam.user.domain.ProfileType
import io.quarkus.runtime.annotations.RegisterForReflection

/**
 * Registers classes from external dependencies for GraalVM native image reflection.
 * These classes are used for JSON serialization/deserialization and need to be
 * accessible via reflection at runtime.
 */
@RegisterForReflection(targets = [
    // com.revethq.core
    Metadata::class,
    Identifier::class,
    SchemaValidation::class,
    // com.revethq.iam.user.domain
    MemberType::class,
    ProfileType::class
])
class ReflectionConfiguration
