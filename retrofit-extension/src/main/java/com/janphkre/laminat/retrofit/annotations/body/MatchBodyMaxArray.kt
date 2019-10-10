package com.janphkre.laminat.retrofit.annotations.body

@Retention(AnnotationRetention.RUNTIME)
annotation class MatchBodyMaxArray(
    val path: String = "$",
    val maxCount: Int
)