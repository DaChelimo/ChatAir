package com.example.messenger.each_group_chat

import com.bumptech.glide.Glide
import com.example.messenger.EachGroupMessage
import com.example.messenger.R
import com.example.messenger.convertTimeStampToAdapterTime
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.friend_image_chat.view.*
import kotlinx.android.synthetic.main.friend_text_chat.view.*
import kotlinx.android.synthetic.main.my_image_chat.view.*
import kotlinx.android.synthetic.main.my_text_chat.view.*
import timber.log.Timber

class MyTextChatItem(val text: String, var newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.my_text_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        layout.my_text_chat_text.text = text
        layout.my_text_chat_time_stamp.text = convertTimeStampToAdapterTime(newGroupMessage.timeStamp)

        val myImage = layout.my_text_chat_image
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
        if (EachGroupChatFragment.myAccount?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(EachGroupChatFragment.myAccount?.profilePictureUrl)
                .into(myImage)
        }
        Timber.d("my position is $position")
    }
}

class MyImageChatItem(private val myImageUrl: String, val newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.my_image_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        val image = layout.my_image_chat_image

        layout.my_image_chat_time_stamp.text = convertTimeStampToAdapterTime(newGroupMessage.timeStamp)

        Glide.with(layout.context)
            .load(myImageUrl)
            .into(image)

        val myImage = layout.my_image_chat_profile_picture
//            Timber.d("currentUserImageUrl is $currentUserImageUrl")
        if (EachGroupChatFragment.myAccount?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(EachGroupChatFragment.myAccount?.profilePictureUrl)
                .into(myImage)
        }
        Timber.d("my position is $position")
    }
}

class FriendTextChatItem(val newGroupMessage: EachGroupMessage) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.friend_text_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val friendImage = viewHolder.itemView.friends_image
        viewHolder.itemView.friends_text.text = newGroupMessage.textMessage

        if (newGroupMessage.senderAccount?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(newGroupMessage.senderAccount.profilePictureUrl)
                .into(friendImage)
        }

        Timber.d("friend position is $position")
    }
}

class FriendImageChatItem(val newGroupMessage: EachGroupMessage) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.friend_image_chat

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        val image = layout.image_friends_image

        Glide.with(layout.context)
            .load(newGroupMessage.imageUrl)
            .into(image)

        val friendImage = layout.image_friends_profile_picture
        if (newGroupMessage.senderAccount?.profilePictureUrl != null) {
            Glide.with(viewHolder.itemView.context)
                .load(EachGroupChatFragment.basicGroupData?.groupIcon)
                .into(friendImage)
        }
        Timber.d("my position is $position")
    }
}