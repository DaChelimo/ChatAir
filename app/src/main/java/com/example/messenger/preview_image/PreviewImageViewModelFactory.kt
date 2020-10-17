package com.example.messenger.preview_image

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class PreviewImageViewModelFactory (private val fragment: PreviewImageFragment) : ViewModelProvider.Factory {
    @Suppress("unchecked_cast")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PreviewImageViewModel::class.java)) {
            return PreviewImageViewModel(fragment) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}