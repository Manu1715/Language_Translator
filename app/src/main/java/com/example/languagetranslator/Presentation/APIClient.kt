package com.example.languagetranslator.Presentation

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Using Google Translate free API endpoint - more accurate for Hindi translations
    private const val BASE_URL = "https://translate.googleapis.com/"
    
    val translatorApi: TranslatorApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslatorApi::class.java)
    }
}
