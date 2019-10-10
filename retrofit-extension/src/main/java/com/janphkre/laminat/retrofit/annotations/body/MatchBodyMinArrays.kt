package com.janphkre.laminat.retrofit.annotations.body

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyMinArrays(
    val values: Array<MatchBodyMinArray>
)