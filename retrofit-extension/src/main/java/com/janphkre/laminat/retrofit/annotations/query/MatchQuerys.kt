package com.janphkre.laminat.retrofit.annotations.query

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchQuerys(
    val values: Array<MatchQuery>
)