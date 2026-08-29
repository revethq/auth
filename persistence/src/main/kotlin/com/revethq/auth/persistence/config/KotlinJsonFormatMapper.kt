package com.revethq.auth.persistence.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.JavaType
import org.hibernate.type.format.FormatMapper

/**
 * Hibernate [FormatMapper] that can (de)serialize Kotlin data classes
 * (e.g. [com.revethq.core.Metadata]) stored in JSON columns.
 *
 * Hibernate's default `JacksonJsonFormatMapper` creates a vanilla [ObjectMapper]
 * that lacks the Kotlin module and therefore cannot construct Kotlin data classes.
 *
 * Activated via the Hibernate property `hibernate.type.json_format_mapper` in
 * `application.properties`.
 */
class KotlinJsonFormatMapper : FormatMapper {
    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())

    override fun <T : Any?> fromString(
        charSequence: CharSequence,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions,
    ): T = objectMapper.readValue(charSequence.toString(), objectMapper.constructType(javaType.javaTypeClass))

    override fun <T : Any?> toString(
        value: T,
        javaType: JavaType<T>,
        wrapperOptions: WrapperOptions,
    ): String = objectMapper.writeValueAsString(value)
}
