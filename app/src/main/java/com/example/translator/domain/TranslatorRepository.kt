package com.example.translator.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.languagetranslator.Presentation.TranslatorRepository

data class Language(
    val code: String,
    val name: String
)

class TranslatorViewModel(
    private val repository: TranslatorRepository = TranslatorRepository()
) : ViewModel() {

    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            Language("en", "English"),
            Language("es", "Spanish"),
            Language("hi", "Hindi"),
            Language("pa", "Punjabi")
        )
    }

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _sourceLanguage = MutableStateFlow(SUPPORTED_LANGUAGES[0]) // English
    val sourceLanguage: StateFlow<Language> = _sourceLanguage

    private val _targetLanguage = MutableStateFlow(SUPPORTED_LANGUAGES[1]) // Spanish
    val targetLanguage: StateFlow<Language> = _targetLanguage

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setErrorMessage(message: String?) {
        _error.value = message
    }

    fun setSourceLanguage(language: Language) {
        _sourceLanguage.value = language
    }

    fun setTargetLanguage(language: Language) {
        _targetLanguage.value = language
    }

    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
        val nextInput = if (_translatedText.value.isNotBlank()) _translatedText.value else _inputText.value
        _inputText.value = nextInput
        _translatedText.value = ""
        _error.value = null
        if (nextInput.isNotBlank()) {
            translate()
        }
    }

    fun translate() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _error.value = "Please enter text"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.translate(
                text,
                _sourceLanguage.value.code,
                _targetLanguage.value.code
            )
            _isLoading.value = false

            result.onSuccess {
                _translatedText.value = it
                _error.value = null
            }.onFailure {
                _translatedText.value = ""
                _error.value = it.localizedMessage ?: "Error occurred"
            }
        }
    }
}