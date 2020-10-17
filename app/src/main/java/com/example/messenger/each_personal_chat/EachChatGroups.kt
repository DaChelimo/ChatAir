package com.example.messenger.each_personal_chat

import android.view.View
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.friend_image_chat.view.*
import kotlinx.android.synthetic.main.friend_text_chat.view.*
import kotlinx.android.synthetic.main.my_image_chat.view.*
import kotlinx.android.synthetic.main.my_text_chat.view.*
import timber.log.Timber

class MyTextChatItem(val text: String, var newPersonalMessage: EachPersonalMessage) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.my_text_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        layout.my_text_chat_text.text = newPersonalMessage.textMessage
        layout.my_text_chat_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

        val myImage = layout.my_text_chat_image
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
        myImage.visibility = View.GONE
//        if (EachPersonalChatFragment.myAccount?.profilePictureUrl != null) {
//            Glide.with(viewHolder.itemView.context)
//                .load(EachPersonalChatFragment.myAccount?.profilePictureUrl)
//                .into(myImage)
//        }
        Timber.d("my position is $position")
    }
}

class MyImageChatItem(
    private val myImageUrl: String,
    val newPersonalMessage: EachPersonalMessage,
    val fragment: EachPersonalChatFragment
) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.my_image_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        val image = layout.my_image_chat_image

        layout.my_image_chat_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)
        if (newPersonalMessage.textMessage.isNotEmpty()) {
            layout.my_image_text.text = newPersonalMessage.textMessage
        } else layout.my_image_text.visibility = View.GONE

        Glide.with(layout.context)
            .load(myImageUrl)
            .into(image)

        val myImage = layout.my_image_chat_profile_picture

        myImage.setOnClickListener {
            val fromId = newPersonalMessage.fromId
            val toId = newPersonalMessage.toId
            val myUid = firebaseAuth.uid
            val otherUid = if (myUid == fromId) toId else fromId
            fragment.findNavController().navigate(EachPersonalChatFragmentDirections.actionEachPersonalChatFragmentToPreviewImageFragment(Account.getAccount(otherUid) ?: return@setOnClickListener, newPersonalMessage.imageUrl.toString()))
        }
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
        val myAccount = Account.getAccount()
        if (myAccount?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(myAccount.profilePictureUrl)
                .into(myImage)
        }
        Timber.d("my position is $position")
    }
}

class FriendTextChatItem(val text: String, val newPersonalMessage: EachPersonalMessage) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.friend_text_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val friendImage = viewHolder.itemView.friends_image
        val layout = viewHolder.itemView
        viewHolder.itemView.friends_text.text = text
        layout.friends_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)

        val toId = newPersonalMessage.toId
        val fromId = newPersonalMessage.fromId
        val myUid = firebaseAuth.uid
        val friendUser = Account.getAccount(if (toId == myUid) fromId else toId)
        if (friendUser?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(friendUser.profilePictureUrl)
                .into(friendImage)
        }

        Timber.d("friend position is $position")
    }
}

class FriendImageChatItem(
    private val friendImageUrl: String,
    val newPersonalMessage: EachPersonalMessage,
    val fragment: EachPersonalChatFragment
) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.friend_image_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        val image = layout.image_friends_image

        Glide.with(layout.context)
            .load(friendImageUrl)
            .into(image)

        layout.my_image_chat_time_stamp.text = convertTimeStampToAdapterTime(newPersonalMessage.timeStamp)
        if (newPersonalMessage.textMessage.isNotEmpty()) {
            layout.friend_image_text.text = newPersonalMessage.textMessage
        } else layout.friend_image_text.visibility = View.GONE


        val friendImage = layout.image_friends_profile_picture

        friendImage.setOnClickListener {
            val fromId = newPersonalMessage.fromId
            val toId = newPersonalMessage.toId
            val myUid = firebaseAuth.uid
            val otherUid = if (myUid == fromId) toId else fromId
            fragment.findNavController().navigate(EachPersonalChatFragmentDirections.actionEachPersonalChatFragmentToPreviewImageFragment(Account.getAccount(otherUid) ?: return@setOnClickListener, newPersonalMessage.imageUrl.toString()))
        }

        val toId = newPersonalMessage.toId
        val fromId = newPersonalMessage.fromId
        val myUid = firebaseAuth.uid
        val friendUser = Account.getAccount(if (toId == myUid) fromId else toId)

        if (friendUser?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(friendUser.profilePictureUrl)
                .into(friendImage)
        }
        Timber.d("my position is $position")
    }
}