package com.example.messenger.create_group

import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.User
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.create_group_chosen_member.view.*
import kotlinx.android.synthetic.main.each_user_layout.view.*
import timber.log.Timber


class UserListItem(val user: User): Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.each_user_layout

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val image = viewHolder.itemView.each_user_image
        val userName = viewHolder.itemView.each_user_name

        val view = viewHolder.itemView
        Glide.with(view)
            .load(user.profilePictureUrl)
            .into(image)
        userName.text = user.userName
        Timber.i("each uid: ${user.uid}")

    }
}

class ChosenMemberItem(val user: User): Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.create_group_chosen_member

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        val layout = viewHolder.itemView
        val image = layout.chosen_member_image
        val name = layout.chosen_member_name

        name.text = user.userName
        Glide.with(layout)
            .load(user.profilePictureUrl)
            .into(image)
    }
}