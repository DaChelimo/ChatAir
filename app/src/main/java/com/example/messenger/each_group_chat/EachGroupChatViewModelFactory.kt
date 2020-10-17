package com.example.messenger.each_group_chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EachGroupChatViewModelFactory(private val fragment: EachGroupChatFragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EachGroupChatViewModel::class.java)) {
            return EachGroupChatViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}