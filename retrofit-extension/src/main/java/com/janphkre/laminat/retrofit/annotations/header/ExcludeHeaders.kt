package com.janphkre.laminat.retrofit.annotations.header

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExcludeHeaders (
    val values: Array<String> = []
)