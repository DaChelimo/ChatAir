package com.example.messenger.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RegisterViewModelFactory (private val fragment: RegisterFragment, private val phoneNumber: String, private val username: String) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            return RegisterViewModel(fragment, phoneNumber, username) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}