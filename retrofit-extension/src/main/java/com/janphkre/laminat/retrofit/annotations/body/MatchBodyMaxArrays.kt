package com.janphkre.laminat.retrofit.annotations.body

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyMaxArrays(
    val values: Array<MatchBodyMaxArray>
)