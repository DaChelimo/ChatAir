package com.example.messenger.new_users

import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.example.messenger.R
import com.example.messenger.User
import com.example.messenger.firebaseAuth
import com.example.messenger.firebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.each_user_layout.view.*
import timber.log.Timber

class NewUsersViewModel: ViewModel() {
    val adapter = GroupAdapter<GroupieViewHolder>()

    fun fetchUsers(){
        val allUsers = firebaseDatabase.getReference("/users")
        allUsers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.i("error: ${error.details}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val firebaseUser = it.getValue(User::class.java)
                    Timber.i("firebaseUser is $firebaseUser")
                    Timber.d("firebaseUser.uid is ${firebaseUser?.uid} and firebaseAuth.uid is ${firebaseAuth.uid}")
                    if (firebaseUser != null && firebaseUser.uid != firebaseAuth.uid ){
                        adapter.add(UserItem(firebaseUser))
                        Timber.i("Item added in list.")
                    }
                }
                adapter.notifyDataSetChanged()
                Timber.i("End: Size of adapter is ${adapter.itemCount}")
            }
        })
    }

    class UserItem(val user: User): Item<GroupieViewHolder>() {
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
}