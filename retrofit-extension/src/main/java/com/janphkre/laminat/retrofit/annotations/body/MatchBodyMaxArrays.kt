package com.janphkre.laminat.retrofit.annotations.body

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyMaxArrays(
    val values: Array<MatchBodyMaxArray>
)