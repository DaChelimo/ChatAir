package com.example.messenger.each_personal_chat

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.databinding.FragmentEachPersonalChatBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.friend_image_chat.view.*
import kotlinx.android.synthetic.main.friend_text_chat.view.*
import kotlinx.android.synthetic.main.my_image_chat.view.*
import kotlinx.android.synthetic.main.my_text_chat.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class EachPersonalChatFragment : Fragment() {

    lateinit var binding: FragmentEachPersonalChatBinding
    private lateinit var viewModel: EachPersonalChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_each_personal_chat, container, false)
        viewModel = ViewModelProvider(this, EachPersonalChatViewModelFactory(this)).get(EachPersonalChatViewModel::class.java)
        viewModel.canAllowLongClick = true
        viewModel.friendUser.value = EachPersonalChatFragmentArgs.fromBundle(requireArguments()).user

        setToolbarData()
        viewModel.changeLatestMessageStatusToRead()
        viewModel.getProfilePicture()

        viewModel.friendUser.observe(viewLifecycleOwner, {
            setToolbarData()
        })

        val chatRecyclerView = binding.chatRecyclerview
//        Timber.d("intentMessage is $intentMessage.")
//        if (intentMessage == null) Timber.d("IntentMessage is null") else binding.chatEdit.setText(
//            intentMessage
//        )

        binding.chooseImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, viewModel.IMAGE_REQUEST_CODE)
        }

        binding.sendChatBtn.setOnClickListener {
            if (binding.chatEdit.text.isEmpty()) {
                viewModel.showShortToast("Text cannot be empty")
                return@setOnClickListener
            }
            viewModel.sendMessage(viewModel.chooseImageUrl, binding.chatEdit.text.toString())
        }

        viewModel.listenForMessages()
        binding.chatEdit.text.clear()

        chatRecyclerView.adapter = viewModel.adapter

        viewModel.adapter.setOnItemLongClickListener { item, view ->

            if(viewModel.canAllowLongClick) {
                viewModel.longPressMessage = item
                viewModel.longPressView.add(view)
                Timber.d("longPressView size is ${viewModel.longPressView.size}")

                binding.longPressToolbar.visibility = View.VISIBLE
                binding.toolbarConstraint.visibility = View.GONE

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
                    val longPressToolbar = binding.longPressToolbar
//                    setSupportActionBar(longPressToolbar)

                    view.setBackgroundColor(resources.getColor(R.color.highlightColor))
                }

                viewModel.canAllowLongClick = false

                true
            }
            else{
                Timber.i("viewModel.canAllowLongClick is $viewModel.canAllowLongClick")

                false
            }
        }

        viewModel.scrollToPosition.observe(viewLifecycleOwner, {
            val lastItem = it - 1
            Timber.i("adapter.itemCount - 1 is $lastItem and adapter.itemCount is $it")
            binding.chatRecyclerview.layoutManager?.scrollToPosition(lastItem)

            val imm  = this.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.sendChatBtn.windowToken, 0)
        })

        binding.longPressCancelBtn.setOnClickListener {
            binding.longPressToolbar.visibility = View.GONE
            binding.toolbarConstraint.visibility = View.VISIBLE

            setToolbarData()
            viewModel.returnItemToDefault()
        }


        binding.longPressCopyBtn.setOnClickListener {
            viewModel.copyToClipboard()
            setToolbarData()
            viewModel.returnItemToDefault()
        }

        binding.longPressDeleteBtn.setOnClickListener {
            viewModel.deleteMessage()

            binding.longPressToolbar.visibility = View.GONE
            binding.toolbarConstraint.visibility = View.VISIBLE

            setToolbarData()

        }

        binding.longPressShareBtn.setOnClickListener {
            viewModel.shareMessage()
            viewModel.returnItemToDefault()
            setToolbarData()
        }

        binding.personalVoiceCallBtn.setOnClickListener {
//            val intent = Intent(this, PersonalVoiceCallActivity::class.java)
//            intent.putExtra(VOICE_CALL_USER, viewModel.friendUser.value)
//            startActivity(intent)
            findNavController().navigate(EachPersonalChatFragmentDirections.actionEachPersonalChatFragmentToPersonalVideoCallFragment())
        }

        binding.personalVideoCallBtn.setOnClickListener {
//            val intent = Intent(this, PersonalVideoCallActivity::class.java)
////            intent.putExtra(VOICE_CALL_USER, viewModel.friendUser.value)
//            startActivity(intent)
            findNavController().navigate(EachPersonalChatFragmentDirections.actionEachPersonalChatFragmentToPersonalVideoCallFragment())
        }

        return binding.root
    }

    private fun setToolbarData() {
//        val toolbar = binding.toolbarConstraint
        binding.toolbarName.text = viewModel.friendUser.value?.userName
        Glide.with(this)
            .load(viewModel.friendUser.value?.profilePictureUrl)
            .into(binding.toolbarImage)

        binding.personalBackButtonBtn.setOnClickListener {
            findNavController().popBackStack(R.id.latestMessagesFragment, false)
        }

        binding.toolbarLastSeen.text = "Offline"
        val lastSeenRef = firebaseDatabase.getReference("/users-activity/${viewModel.friendUser.value?.uid}")

        lastSeenRef.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val userActivity = snapshot.getValue(UserActivity::class.java) ?: return
                if (!userActivity.isOnline){
                    binding.toolbarLastSeen.text = convertTimeToLastSeenTime(userActivity.lastSeen)
                }
                else{
                    binding.toolbarLastSeen.text = "Online"
                }
            }
        })

        binding.toolbarConstraint.setOnClickListener {
//            val intent = Intent(this, OthersProfileActivity::class.java)
//            intent.putExtra(FRIEND_USER_PROFILE, viewModel.friendUser.value)
//            Timber.i("friend user is $viewModel.friendUser.value")
//            startActivity(intent)
        }
//        setSupportActionBar(toolbar)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == viewModel.IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            if (data.data == null) return
            val uri = data.data
            EachPersonalChatFragmentDirections.actionEachPersonalChatFragmentToPreviewImageFragment(viewModel.friendUser.value ?: return, uri.toString(), true)
        }
    }


}
