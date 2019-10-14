package com.janphkre.laminat.retrofit.annotations.body

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyRegexes(
    val values: Array<MatchRegex>
)