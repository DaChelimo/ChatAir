package com.example.messenger.create_group

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.messenger.each_personal_chat.EachPersonalChatViewModel

class CreateGroupViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EachPersonalChatViewModel::class.java)) {
            return CreateGroupViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}