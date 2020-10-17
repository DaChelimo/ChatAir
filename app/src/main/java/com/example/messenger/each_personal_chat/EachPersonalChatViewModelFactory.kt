package com.example.messenger.each_personal_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EachPersonalChatViewModelFactory(private val fragment: EachPersonalChatFragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EachPersonalChatViewModel::class.java)) {
            return EachPersonalChatViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}