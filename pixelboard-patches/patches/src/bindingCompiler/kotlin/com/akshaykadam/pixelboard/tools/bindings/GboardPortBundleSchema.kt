package com.akshaykadam.pixelboard.tools.bindings

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.math.BigDecimal

internal object GboardPortBundleSchema {
    private const val RESOURCE = "/gboard/gboard-port-bundle.schema.json"

    private val schema: JsonObject by lazy {
        val stream = checkNotNull(GboardPortBundleSchema::class.java.getResourceAsStream(RESOURCE)) {
            "Missing PortBundle schema resource $RESOURCE"
        }
        stream.bufferedReader(Charsets.UTF_8).use { reader ->
            JsonParser.parseReader(reader).asJsonObject
        }
    }

    fun validate(value: JsonObject) {
        val errors = mutableListOf<String>()
        validateNode(value, schema, "$", errors)
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString("; "))
        }
    }

    private fun validateNode(
        value: JsonElement,
        node: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        node.get("\$ref")?.asString?.let { reference ->
            validateNode(value, resolve(reference), path, errors)
            return
        }
        node.get("type")?.asString?.let { expected ->
            if (!hasType(value, expected)) {
                errors += "$path must be $expected"
                return
            }
        }
        node.get("const")?.let { expected ->
            if (value != expected) errors += "$path must equal $expected"
        }
        node.getAsJsonArray("enum")?.let { choices ->
            if (choices.none { it == value }) errors += "$path is not an allowed value"
        }
        if (value.isJsonObject) validateObject(value.asJsonObject, node, path, errors)
        if (value.isJsonArray) validateArray(value, node, path, errors)
        if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
            validateString(value.asString, node, path, errors)
        }
        if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
            node.get("minimum")?.asBigDecimal?.let { minimum ->
                if (value.asBigDecimal < minimum) errors += "$path must be at least $minimum"
            }
        }
    }

    private fun validateObject(
        value: JsonObject,
        node: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        val properties = node.getAsJsonObject("properties") ?: JsonObject()
        node.getAsJsonArray("required")?.forEach { required ->
            val name = required.asString
            if (!value.has(name)) errors += "$path is missing required property $name"
        }
        if (node.get("additionalProperties")?.let { it.isJsonPrimitive && !it.asBoolean } == true) {
            (value.keySet() - properties.keySet()).sorted().forEach { name ->
                errors += "$path has unknown property $name"
            }
        }
        properties.entrySet().forEach { (name, propertySchema) ->
            value.get(name)?.let { child ->
                validateNode(child, propertySchema.asJsonObject, "$path.$name", errors)
            }
        }
    }

    private fun validateArray(
        value: JsonElement,
        node: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        val array = value.asJsonArray
        node.get("minItems")?.asInt?.let { minimum ->
            if (array.size() < minimum) errors += "$path must contain at least $minimum item(s)"
        }
        node.get("maxItems")?.asInt?.let { maximum ->
            if (array.size() > maximum) errors += "$path must contain at most $maximum item(s)"
        }
        if (node.get("uniqueItems")?.asBoolean == true && array.toSet().size != array.size()) {
            errors += "$path must contain unique items"
        }
        node.getAsJsonObject("items")?.let { itemSchema ->
            array.forEachIndexed { index, item ->
                validateNode(item, itemSchema, "$path[$index]", errors)
            }
        }
    }

    private fun validateString(
        value: String,
        node: JsonObject,
        path: String,
        errors: MutableList<String>,
    ) {
        node.get("minLength")?.asInt?.let { minimum ->
            if (value.length < minimum) errors += "$path must contain at least $minimum character(s)"
        }
        node.get("pattern")?.asString?.let { pattern ->
            if (!Regex(pattern).containsMatchIn(value)) errors += "$path does not match $pattern"
        }
    }

    private fun hasType(value: JsonElement, expected: String): Boolean = when (expected) {
        "object" -> value.isJsonObject
        "array" -> value.isJsonArray
        "string" -> value.isJsonPrimitive && value.asJsonPrimitive.isString
        "integer" -> value.isJsonPrimitive && value.asJsonPrimitive.isNumber &&
            value.asBigDecimal.isInteger()
        "number" -> value.isJsonPrimitive && value.asJsonPrimitive.isNumber
        "boolean" -> value.isJsonPrimitive && value.asJsonPrimitive.isBoolean
        "null" -> value.isJsonNull
        else -> error("Unsupported JSON Schema type $expected")
    }

    private fun resolve(reference: String): JsonObject {
        require(reference.startsWith("#/")) { "Only local schema references are supported: $reference" }
        return reference.removePrefix("#/").split('/').fold(schema) { current, token ->
            current.getAsJsonObject(token.replace("~1", "/").replace("~0", "~"))
                ?: error("Unresolvable schema reference $reference")
        }
    }

    private fun BigDecimal.isInteger(): Boolean = stripTrailingZeros().scale() <= 0
}
