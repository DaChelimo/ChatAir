package com.example.messenger.both_profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.messenger.create_group.CreateGroupViewModel
import com.example.messenger.each_personal_chat.EachPersonalChatViewModel

class UserProfileViewModelFactory(private val fragment: UserProfileFragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserProfileViewModel::class.java)) {
            return UserProfileViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}