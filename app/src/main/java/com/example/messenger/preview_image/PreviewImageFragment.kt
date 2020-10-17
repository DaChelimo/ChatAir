package com.example.messenger.preview_image

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.User
import com.example.messenger.databinding.PreviewImageFragmentBinding

class PreviewImageFragment : Fragment() {

    private lateinit var viewModel: PreviewImageViewModel
    private lateinit var binding: PreviewImageFragmentBinding
    lateinit var friendUser: User
    lateinit var imageUrl: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.preview_image_fragment, container, false)
        val mainArguments = PreviewImageFragmentArgs.fromBundle(requireArguments())
        friendUser = mainArguments.friendUser
        imageUrl = mainArguments.imageUriString.toString().toUri()
        val shouldEdit = mainArguments.shouldEdit

        val imagePreview = binding.mainImage
        val textMessage = binding.textMessage

        if (shouldEdit) {
            binding.sendBtn.visibility = View.GONE
            binding.textMessage.visibility = View.GONE
        }

        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .into(imagePreview)

        binding.sendBtn.setOnClickListener {
            if (textMessage.text.toString().isEmpty()) viewModel.sendMessage(null, textMessage.text.toString())
            else viewModel.getStorageImageUrl(imageUrl)
        }

        viewModel.storageImageUrl.observe(viewLifecycleOwner, {
            viewModel.sendMessage(it, textMessage.text.toString() )
        })

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this, PreviewImageViewModelFactory(this)).get(PreviewImageViewModel::class.java)
    }

}