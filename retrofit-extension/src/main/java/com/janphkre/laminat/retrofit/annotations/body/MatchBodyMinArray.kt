package com.janphkre.laminat.retrofit.annotations.body

@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyMinArray(
    val path: String = "$",
    val minCount: Int
)