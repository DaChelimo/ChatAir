package com.example.messenger.each_group_chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.R
import com.example.messenger.databinding.FragmentEachGroupChatBinding
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import timber.log.Timber
import java.util.*
import kotlin.collections.LinkedHashMap

class EachGroupChatFragment : Fragment() {

    lateinit var binding: FragmentEachGroupChatBinding
    private lateinit var viewModel: EachGroupChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_each_group_chat, container, false)
        viewModel = ViewModelProvider(this, EachGroupChatViewModelFactory(this)).get(EachGroupChatViewModel::class.java)

//        viewModel.basicGroupData = intent.getParcelableExtra(GROUP_KEY)
//        if (viewModel.basicGroupData != null) {
////            supportActionBar?.title = viewModel.basicGroupData!!.groupName
//        }

        setToolbarData()
        viewModel.getProfilePicture()

        val chatRecyclerView = binding.groupChatRecyclerview
//        val intentMessage = intent.getStringExtra(INTENT_URI)
//        Timber.d("intentMessage is $intentMessage.")
//        if (intentMessage == null) Timber.d("IntentMessage is null") else binding.groupChatEdit.setText(
//            intentMessage
//        )

        binding.groupChooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, viewModel.IMAGE_REQUEST_CODE)
        }

        binding.groupSendChatBtn.setOnClickListener {
            if (binding.groupChatEdit.text.isEmpty()) {
                viewModel.showShortToast("Text cannot be empty")
                return@setOnClickListener
            }
            viewModel.sendMessage(binding.groupChatEdit.text.toString())
        }

        viewModel.listenForMessages()

        chatRecyclerView.adapter = viewModel.adapter

        viewModel.adapter.setOnItemLongClickListener { item, view ->

            if (viewModel.canAllowLongClick) {
                viewModel.longPressMessage = item
                viewModel.longPressView = view

                binding.groupLongPressConstraint.visibility = View.VISIBLE
                binding.groupToolbarConstraint.visibility = View.GONE

                val isApplicable: Boolean = when (item) {
                    is FriendImageChatItem -> {
                        false
                    }
                    is MyImageChatItem -> {
                        false
                    }
                    else -> {
                        true
                    }
                }

                Timber.d("isApplicable is $isApplicable")

                if (isApplicable) {
                    val longPressToolbar = binding.groupLongPressConstraint
//                    setSupportActionBar(longPressToolbar)

                    view.setBackgroundColor(resources.getColor(R.color.highlightColor))
                }

                viewModel.canAllowLongClick = false

                true
            } else {
                Timber.i("canAllowLongClick is ${viewModel.canAllowLongClick}")

                false
            }
        }

        viewModel.scrollToPosition.observe(viewLifecycleOwner, {
            val lastItem = it - 1
            Timber.i("adapter.itemCount - 1 is $lastItem and viewModel.adapter.itemCount is ${viewModel.adapter.itemCount}")
            binding.groupChatRecyclerview.layoutManager?.scrollToPosition(lastItem)

            binding.groupChatEdit.text.clear()
            val imm = this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.groupSendChatBtn.windowToken, 0)
        })

        binding.groupLongPressCancelBtn.setOnClickListener {
            binding.groupLongPressConstraint.visibility = View.GONE
            binding.groupToolbarConstraint.visibility = View.VISIBLE

            setToolbarData()
            viewModel.returnItemToDefault()
        }


        binding.groupLongPressCopyBtn.setOnClickListener {
            viewModel.copyToClipboard()
            setToolbarData()
            viewModel.returnItemToDefault()
        }

        binding.groupLongPressDeleteBtn.setOnClickListener {
//            deleteMessage()

            binding.groupLongPressConstraint.visibility = View.GONE
            binding.groupToolbarConstraint.visibility = View.VISIBLE // groupToolbarConstraint

            setToolbarData()
            viewModel.returnItemToDefault()

        }

        binding.groupLongPressShareBtn.setOnClickListener {
            viewModel.shareMessage()
            viewModel.returnItemToDefault()
            setToolbarData()
        }

        return binding.root
    }

//    override fun onBackPressed() {
//        if (viewModel.basicGroupData != null){
//            val intent = Intent(this, LatestMessagesFragment::class.java)
//            startActivity(intent)
//            finishAffinity()
//        }
//        else{
//            super.onBackPressed()
//        }
//    }

    private fun setToolbarData() {
        val toolbar = binding.groupToolbarConstraint
        binding.groupToolbarName.text = viewModel.basicGroupData?.groupName
        Glide.with(this)
            .load(viewModel.basicGroupData?.groupIcon)
            .into(binding.groupToolbarImage)

//        binding.groupBackButton.setOnClickListener {
//            val intent = Intent(this, LatestMessagesFragment::class.java)
//            startActivity(intent)
//            finishAffinity()
//        }

        binding.groupToolbarConstraint.setOnClickListener {
//            val intent = Intent(this, OthersProfileActivity::class.java)
//            intent.putExtra(FRIEND_USER_PROFILE, viewModel.basicGroupData)
//            Timber.i("friend user is ${viewModel.basicGroupData}")
//            startActivity(intent)
        }
//        setSupportActionBar(toolbar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == viewModel.IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            if (data.data == null) return
            val uri = data.data
            val ref = firebaseStorage.getReference("/images/chat-images/${UUID.randomUUID()}")
            ref.putFile(uri!!).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    Timber.d("download url is $it")
                    viewModel.chooseImageUrl = it.toString()
                    binding.groupChatEdit.setText(viewModel.chooseImageUrl.toString())
                }
            }.addOnFailureListener {
                Timber.e(it)
            }
        }
    }



//    var numberRepeated = 1


}