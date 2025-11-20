package com.example.languagetranslator.Presentation

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit interface for Google Translate API
interface TranslatorApi {
    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String,
        @Query("sl") sourceLang: String,
        @Query("tl") targetLang: String,
        @Query("dt") dt: String,
        @Query("q") text: String
    ): ResponseBody
}
