package com.example.messenger.create_group_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.messenger.each_personal_chat.EachPersonalChatViewModel

class CreateGroupDetailsViewModelFactory(private val fragment: CreateGroupDetailsFragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EachPersonalChatViewModel::class.java)) {
            return CreateGroupDetailsViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}