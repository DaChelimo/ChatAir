package com.example.messenger.latest_messages

import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_item.view.*
import timber.log.Timber

class GroupMessageItem(val basicGroupData: BasicGroupData, val latestMessage: EachGroupMessage?): Item<GroupieViewHolder>(){
    override fun getLayout(): Int = R.layout.latest_messages_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView

        layout.latest_item_username.text = basicGroupData.groupName
        if (basicGroupData.groupIcon != null) {
            Glide.with(layout.context)
                .load(basicGroupData.groupIcon)
                .placeholder(R.drawable.ic_baseline_person_24)
                .into(layout.latest_item_image)
        }
        else{
            layout.latest_item_image.setImageResource(R.drawable.group_default_image)
        }

        Timber.d("latest message is $latestMessage")

        if (latestMessage == null) {
            layout.latest_item_last_message.visibility = View.GONE
        }
        else{
            layout.latest_item_last_message.visibility = View.VISIBLE
            layout.latest_item_last_message.text = "${latestMessage.senderAccount?.userName}: ${latestMessage.textMessage}"
            layout.latest_item_time_sent.text = formatTimeForAdapter(latestMessage.timeStamp)
        }
    }
}

class LatestMessageItem(val chatPersonalMessage: EachPersonalMessage): Item<GroupieViewHolder>(){
    override fun getLayout(): Int = R.layout.latest_messages_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView

        val otherAccount = if (chatPersonalMessage.senderAccount?.uid != firebaseAuth.uid) chatPersonalMessage.senderAccount else chatPersonalMessage.receiverAccount

        Timber.d("otherAccount.username is ${otherAccount?.userName}")

        layout.latest_item_username.text = otherAccount?.userName

        if (chatPersonalMessage.wasRead) {
            layout.latest_item_time_sent.setTextColor(
                ResourcesCompat.getColor(
                    viewHolder.itemView.resources,
                    R.color.textGrey,
                    null
                )
            )
        }
        else {
            layout.latest_item_time_sent.setTextColor(viewHolder.itemView.resources.getColor(R.color.dateBlue))
        }

        val toId = otherAccount?.uid
        val totalUnreadMessages = getAllUnreadMessages(toId)

        if (totalUnreadMessages > 0){
            layout.latest_item_unread_text.text = totalUnreadMessages.toString()
            layout.latest_item_unread_text.visibility = View.VISIBLE
            layout.latest_item_unread_background.visibility = View.VISIBLE
        } else{
            layout.latest_item_unread_text.visibility = View.GONE
            layout.latest_item_unread_background.visibility = View.GONE
        }

        layout.latest_item_time_sent.text = formatTimeForAdapter(chatPersonalMessage.timeStamp)

        Glide.with(layout.context)
            .load(otherAccount?.profilePictureUrl)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(layout.latest_item_image)

        var formatString = if (chatPersonalMessage.textMessage.length > 33) chatPersonalMessage.textMessage.substring(0..32) else chatPersonalMessage.textMessage
        if(chatPersonalMessage.textMessage.length > 27){
            formatString += "..."
        }

        if (chatPersonalMessage.imageUrl == null) {
            layout.latest_item_last_message.text = formatString
        }else {
            layout.latest_item_last_message.text = "Image"
        }
    }
}