package com.janphkre.laminat.retrofit.body

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslJsonArray
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslJsonRootValue
import au.com.dius.pact.external.PactBuildException
import au.com.dius.pact.model.BasePact
import au.com.dius.pact.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.model.matchingrules.MinTypeMatcher
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import okio.Buffer

object DslJsonBodyConverter : DslBodyConverter {

    override fun toPactDsl(retrofitBody: Buffer, bodyMatches: BodyMatchElement?): DslPart {
        val jsonBody = retrofitBody.inputStream().use { BasePact.jsonParser.parse(it.reader()) }
        return jsonRootToDsl(jsonBody, bodyMatches)
    }

    private fun jsonRootToDsl(jsonElement: JsonElement, bodyMatches: BodyMatchElement?): DslPart {
        return when {
            jsonElement.isJsonObject -> jsonObjectToDsl("", jsonElement.asJsonObject, null, bodyMatches?.asObject())
            jsonElement.isJsonArray -> jsonArrayToDsl("", jsonElement.asJsonArray, null, bodyMatches?.asArray())
            jsonElement.isJsonPrimitive -> jsonPrimitiveToDslRoot(jsonElement.asJsonPrimitive, bodyMatches?.asString())
            jsonElement.isJsonNull -> PactDslJsonRootValue.matchNull()
            else -> raiseException(jsonElement)
        }
    }

    private fun jsonElementToDsl(keyInParent: String?, jsonElement: JsonElement, parent: DslPart, bodyMatches: BodyMatchElement?): DslPart? {
        return when {
            jsonElement.isJsonObject -> jsonObjectToDsl(keyInParent, jsonElement.asJsonObject, parent, bodyMatches?.asObject())
            jsonElement.isJsonArray -> jsonArrayToDsl(keyInParent, jsonElement.asJsonArray, parent, bodyMatches?.asArray())
            jsonElement.isJsonPrimitive -> jsonPrimitiveToDsl(keyInParent, jsonElement.asJsonPrimitive, parent, bodyMatches?.asString())
            jsonElement.isJsonNull -> jsonNullToDsl(keyInParent, parent)
            else -> raiseException(jsonElement)
        }
    }

    private fun jsonObjectToDsl(keyInParent: String?, jsonObject: JsonObject, parent: DslPart?, bodyMatches: BodyMatchElement.BodyMatchObject?): DslPart {
        //TODO USE EACH KEY LIKE?
        val dslObject = if (keyInParent != null) {
            parent?.`object`(keyInParent)
        } else {
            parent?.`object`()
        } ?: PactDslJsonBody()
        jsonObjectToDirectDsl(jsonObject, dslObject, bodyMatches)
        return dslObject.closeObject() ?: dslObject
    }

    private fun jsonArrayToDsl(keyInParent: String?, jsonArray: JsonArray, parent: DslPart?, bodyMatches: BodyMatchElement.BodyMatchArray?): DslPart {
        //TODO: THIS IS ONLY WORKING FOR ARRAYS OF OBJECTS ATM
        val dslArray = when (bodyMatches) {
            is BodyMatchElement.BodyMatchMinArray -> {
                val dslArrayElement = if (keyInParent != null) {
                    parent?.minArrayLike(keyInParent, bodyMatches.minCount)
                } else {
                    parent?.minArrayLike(bodyMatches.minCount)
                } ?: PactDslJsonArray("", "", null, true).let {
                    it.matchers.addRule("", MinTypeMatcher(bodyMatches.minCount))
                    it.numberExamples = bodyMatches.minCount
                    PactDslJsonBody(".", "", it)
                }

                jsonArrayToDirectDsl(jsonArray.first(), dslArrayElement, bodyMatches.at(0))
            }
            is BodyMatchElement.BodyMatchMaxArray -> {
                val dslArrayElement = if (keyInParent != null) {
                    parent?.maxArrayLike(keyInParent, bodyMatches.maxCount)
                } else {
                    parent?.maxArrayLike(bodyMatches.maxCount)
                } ?: PactDslJsonArray("", "", null, true).let {
                    it.matchers.addRule("", MaxTypeMatcher(bodyMatches.maxCount))
                    it.numberExamples = bodyMatches.maxCount
                    PactDslJsonBody(".", "", it)
                }

                jsonArrayToDirectDsl(jsonArray.first(), dslArrayElement, bodyMatches.at(0))
            }
            else -> {
                val dslArray = if (keyInParent != null) {
                    parent?.array(keyInParent)
                } else {
                    parent?.array()
                } ?: PactDslJsonArray()

                jsonArray.forEachIndexed { index, jsonElement ->
                    jsonElementToDsl(null, jsonElement, dslArray, bodyMatches?.at(index))
                }
                dslArray
            }
        }
        return dslArray.closeArray() ?: dslArray
    }

    private fun jsonArrayToDirectDsl(jsonArrayElement: JsonElement, dslArrayElement: PactDslJsonBody, arrayElementMatches: BodyMatchElement?): DslPart {
        if (!jsonArrayElement.isJsonObject) {
            throw PactBuildException("Arrays of ${jsonArrayElement.javaClass.name} are not supported by pact dsl with arrayLike!")
        }
        if (arrayElementMatches !is BodyMatchElement.BodyMatchObject?) {
            throw PactBuildException("Arrays of ${arrayElementMatches?.javaClass?.name} are not supported by pact dsl with arrayLike!")
        }
        jsonObjectToDirectDsl(jsonArrayElement.asJsonObject, dslArrayElement, arrayElementMatches)
        return dslArrayElement.closeObject() ?: throw PactBuildException("Closing the inner object of an JsonArray returned null!")
    }

    private fun jsonObjectToDirectDsl(jsonObject: JsonObject, dslObject: PactDslJsonBody, bodyMatches: BodyMatchElement.BodyMatchObject?) {
        jsonObject.entrySet().forEach {
            jsonElementToDsl(it.key, it.value, dslObject, bodyMatches?.entry(it.key))
        }
    }

    private fun jsonPrimitiveToDsl(keyInParent: String?, jsonPrimitive: JsonPrimitive, parent: DslPart, bodyMatches: BodyMatchElement.BodyMatchString?): DslPart {
        return when (parent) {
            is PactDslJsonBody -> jsonPrimitiveToDslObject(keyInParent ?: raiseException(jsonPrimitive), jsonPrimitive, parent, bodyMatches)
            is PactDslJsonArray -> jsonPrimitiveToDslArray(jsonPrimitive, parent, bodyMatches)
            else -> raiseException(jsonPrimitive)
        }
    }

    private fun jsonPrimitiveToDslArray(jsonPrimitive: JsonPrimitive, parent: PactDslJsonArray, bodyMatches: BodyMatchElement.BodyMatchString?): DslPart {
        return when {
            jsonPrimitive.isBoolean && bodyMatches == null -> parent.booleanType(jsonPrimitive.asBoolean)
            jsonPrimitive.isNumber && bodyMatches == null -> parent.numberType(jsonPrimitive.asNumber)
            jsonPrimitive.isString && bodyMatches == null -> parent.stringType(jsonPrimitive.asString)
            jsonPrimitive.isString && bodyMatches != null -> parent.stringMatcher(bodyMatches.regex, jsonPrimitive.asString)
            else -> raiseException(jsonPrimitive)
        }
    }

    private fun jsonPrimitiveToDslObject(keyInParent: String, jsonPrimitive: JsonPrimitive, parent: PactDslJsonBody, bodyMatches: BodyMatchElement.BodyMatchString?): DslPart {
        return when {
            jsonPrimitive.isBoolean && bodyMatches == null -> parent.booleanType(keyInParent, jsonPrimitive.asBoolean)
            jsonPrimitive.isNumber && bodyMatches == null -> parent.numberType(keyInParent, jsonPrimitive.asNumber)
            jsonPrimitive.isString && bodyMatches == null -> parent.stringType(keyInParent, jsonPrimitive.asString)
            jsonPrimitive.isString && bodyMatches != null -> parent.stringMatcher(keyInParent, bodyMatches.regex, jsonPrimitive.asString)
            else -> raiseException(jsonPrimitive)
        }
    }

    private fun jsonPrimitiveToDslRoot(jsonPrimitive: JsonPrimitive, bodyMatches: BodyMatchElement.BodyMatchString?): DslPart {
        return when {
            jsonPrimitive.isBoolean && bodyMatches == null -> PactDslJsonRootValue.booleanType(jsonPrimitive.asBoolean)
            jsonPrimitive.isNumber && bodyMatches == null -> PactDslJsonRootValue.numberType(jsonPrimitive.asNumber)
            jsonPrimitive.isString && bodyMatches == null -> PactDslJsonRootValue.stringType(jsonPrimitive.asString)
            jsonPrimitive.isString && bodyMatches != null -> PactDslJsonRootValue.stringMatcher(bodyMatches.regex, jsonPrimitive.asString)
            else -> raiseException(jsonPrimitive)
        }
    }

    private fun jsonNullToDsl(keyInParent: String?, parent: DslPart): DslPart {
        return when (parent) {
            is PactDslJsonBody -> parent.nullValue(keyInParent ?: raiseException(JsonNull.INSTANCE))
            is PactDslJsonArray -> parent.nullValue()
            else -> raiseException(JsonNull.INSTANCE)
        }
    }

    private fun raiseException(jsonElement: JsonElement): Nothing {
        throw PactBuildException("Unsupported json found in $jsonElement")
    }
}