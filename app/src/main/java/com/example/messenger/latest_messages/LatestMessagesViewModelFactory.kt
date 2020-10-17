package com.example.messenger.latest_messages

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class LatestMessagesViewModelFactory(private val fragment: Fragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LatestMessagesViewModel::class.java)) {
            return LatestMessagesViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}