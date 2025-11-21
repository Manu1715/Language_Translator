package com.example.languagetranslator.Presentation

import org.json.JSONArray

class TranslatorRepository {
    private val api = ApiClient.translatorApi

    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        return try {
            val normalizedText = text.trim().replace(Regex("\\s+"), " ")
            
            if (normalizedText.isBlank()) {
                return Result.failure(Exception("Input text is empty"))
            }
            
            // Map language codes to Google Translate format
            val sourceCode = mapLanguageCode(sourceLang)
            val targetCode = mapLanguageCode(targetLang)
            
            // Google Translate works better with whole sentences/phrases for context
            // For very long texts, we'll translate in chunks but preserve context
            val maxChunkSize = 4500 // Google Translate has ~5000 character limit
            
            val translated = if (normalizedText.length > maxChunkSize) {
                // For very long texts, split by sentences but maintain context
                val sentences = splitIntoSentences(normalizedText)
                val chunks = mutableListOf<String>()
                var currentChunk = StringBuilder()
                
                sentences.forEach { sentence ->
                    if (currentChunk.length + sentence.length > maxChunkSize && currentChunk.isNotEmpty()) {
                        chunks.add(currentChunk.toString().trim())
                        currentChunk.clear()
                    }
                    if (currentChunk.isNotEmpty()) currentChunk.append(" ")
                    currentChunk.append(sentence)
                }
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                }
                
                chunks.mapNotNull { chunk ->
                    if (chunk.isBlank()) null else {
                        try {
                            translateText(chunk, sourceCode, targetCode)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }.joinToString(separator = " ").trim()
            } else {
                // Translate as whole text for better context and accuracy (especially for Hindi)
                translateText(normalizedText, sourceCode, targetCode)
            }
            
            if (translated.isBlank()) {
                Result.failure(Exception("Translation returned empty result"))
            } else {
                Result.success(translated)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Translation failed: ${e.message}", e))
        }
    }

    /**
     * Translates text using Google Translate API
     * The API returns a JSON array where the translation is in [0][0][0]
     */
    private suspend fun translateText(text: String, sourceLang: String, targetLang: String): String {
        val responseBody = api.translate("gtx", sourceLang, targetLang, "t", text)
        
        // Use 'use' block for automatic resource management
        val responseStr = responseBody.use { body ->
            try {
                body.string()
            } catch (e: Exception) {
                throw Exception("Failed to read response: ${e.message}", e)
            }
        }
        
        if (responseStr.isBlank()) {
            throw Exception("Empty response from API")
        }
        
        // The response is a JSON array: [[["translated text",...],...],...]
        // We need to extract the translated text from the nested structure
        return extractTranslationFromResponse(responseStr)
    }

    /**
     * Extracts translation from Google Translate API JSON response
     * Response format: [[["translated text","original text",null,null,0]],null,"en"]
     */
    private fun extractTranslationFromResponse(responseStr: String): String {
        return try {
            // Clean the response string (remove any leading/trailing whitespace)
            val cleanResponse = responseStr.trim()
            if (cleanResponse.isEmpty()) {
                throw Exception("Empty response from API")
            }
            
            val jsonArray = JSONArray(cleanResponse)
            if (jsonArray.length() == 0) {
                throw Exception("Empty JSON array in response")
            }
            
            // Get the first element which should be an array of translation segments
            val firstElement = jsonArray.get(0)
            if (firstElement !is JSONArray) {
                throw Exception("Unexpected response format: first element is not an array. Got: ${firstElement::class.simpleName}")
            }
            
            val firstArray = firstElement
            val sb = StringBuilder()
            
            for (i in 0 until firstArray.length()) {
                try {
                    val item = firstArray.get(i)
                    if (item is JSONArray && item.length() > 0) {
                        // The translated text is at index 0
                        val translatedText = item.getString(0)
                        if (translatedText.isNotBlank()) {
                            sb.append(translatedText)
                        }
                    }
                } catch (e: Exception) {
                    // Skip this item if it can't be parsed, continue with next
                    continue
                }
            }
            
            val result = sb.toString().trim()
            if (result.isBlank()) {
                throw Exception("No translation found in response. Raw response: $cleanResponse")
            }
            result
        } catch (e: org.json.JSONException) {
            throw Exception("JSON parsing error: ${e.message}. Response: ${responseStr.take(200)}", e)
        } catch (e: Exception) {
            throw Exception("Failed to parse translation response: ${e.message}. Response: ${responseStr.take(200)}", e)
        }
    }

    /**
     * Maps language codes to Google Translate format
     * Google Translate uses ISO 639-1 codes
     */
    private fun mapLanguageCode(langCode: String): String {
        return when (langCode.lowercase()) {
            "hi" -> "hi"  // Hindi
            "en" -> "en"  // English
            "es" -> "es"  // Spanish
            "pa" -> "pa"  // Punjabi
            else -> langCode.lowercase()
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        // Split by sentence boundaries while preserving context
        return text.trim()
            .split(Regex("(?<=[.!?])\\s+"))
            .filter { it.isNotBlank() }
            .map { it.trim() }
    }
}
