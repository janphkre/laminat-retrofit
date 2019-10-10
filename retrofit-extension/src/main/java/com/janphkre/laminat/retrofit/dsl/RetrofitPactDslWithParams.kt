package com.janphkre.laminat.retrofit.dsl

import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath
import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath
import au.com.dius.pact.consumer.dsl.PactDslResponse
import au.com.dius.pact.external.PactBuildException
import com.janphkre.laminat.retrofit.annotations.*
import com.janphkre.laminat.retrofit.body.BodyMatchElement
import com.janphkre.laminat.retrofit.body.RetrofitPactDslBodyCreator
import okhttp3.RetrofitPactRequestWithParams
import java.lang.reflect.Method

class RetrofitPactDslWithParams(
    private val pactDslRequestWithoutPath: PactDslRequestWithoutPath,
    private val retrofitMethod: Method,
    private val retrofitRequest: RetrofitPactRequestWithParams
) {

    private val anyMatchRegex = ".*"
    private val annotations = Annotations(retrofitMethod)

    fun toPactDsl(): PactDslRequestWithPath {
        val intermediatePact = pactDslRequestWithoutPath.method(retrofitRequest.method)
            .let { pactDsl ->
                if (annotations.pathRegex == null) {
                    pactDsl.path(retrofitRequest.relativePath)
                } else {
                    pactDsl.matchPath(annotations.pathRegex, retrofitRequest.relativePath)
                }
            }
            .apply {
                retrofitRequest.headers.forEach { header ->
                    if (annotations.excludeHeaders[header.first] == null) {
                        return@forEach
                    }
                    val regex = annotations.headerRegexes[header.first]
                    if (regex == null) {
                        headers(header.first, header.second)
                    } else {
                        matchHeader(header.first, regex, header.second)
                    }
                }
                retrofitRequest.query.forEach { parameter ->
                    val regex = annotations.queryRegexes[parameter.first] ?: anyMatchRegex
                    matchQuery(parameter.first, regex, parameter.second)
                }
            }
        //TODO not accounting for MultiPart or FormUrlEncoded at the moment?
        if (retrofitRequest.body == null) {
            return intermediatePact
        }
        val dslBody = RetrofitPactDslBodyCreator(
            retrofitMethod,
            retrofitRequest.body,
            BodyMatchElement.from(annotations.bodyRegexes, annotations.bodyArrays)
        ).create()
        return intermediatePact.apply {
            if (dslBody != null) {
                body(dslBody)
            }
        }
    }

    fun body(dslPart: DslPart): PactDslRequestWithPath {
        return toPactDsl().body(dslPart)
    }

    /**
     * Converts the retrofit pact dsl back to a pact dsl.
     * matchPath is unsupported at the moment.
     */
    fun willRespondWith(): PactDslResponse {
        return toPactDsl().willRespondWith()
    }

    private fun raiseException(message: String, cause: Exception? = null): Nothing {
        throw PactBuildException("$message on $retrofitMethod", cause)
    }
}