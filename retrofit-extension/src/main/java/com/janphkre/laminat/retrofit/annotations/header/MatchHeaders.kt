package com.janphkre.laminat.retrofit.annotations.header

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MatchHeaders(
    val values: Array<MatchHeader>
)