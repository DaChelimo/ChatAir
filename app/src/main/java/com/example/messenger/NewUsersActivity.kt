package com.example.messenger

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityNewUsersBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.each_user_layout.view.*
import timber.log.Timber

class NewUsersActivity : AppCompatActivity() {

    lateinit var binding: ActivityNewUsersBinding
    lateinit var adapter: GroupAdapter<GroupieViewHolder>
    var intentUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i("intent type is ${intent.type}")
        if (Intent.ACTION_SEND == intent.action && intent.type != null){
            intentUri = intent.getStringExtra(INTENT_URI)
            Timber.d("intentUri is $intentUri")
            supportActionBar?.title = "Send To"
        }
        else{
            supportActionBar?.title = "Select User"
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_new_users)
        val userRecyclerView = binding.usersRecyclerview

        adapter = GroupAdapter()
        userRecyclerView.adapter = adapter

        Timber.i("Init: Size of adapter is ${adapter.groupCount}")

        fetchUsers()
        adapter.setOnItemClickListener { item, view ->

            val user = item as UserItem

            val intent = Intent(view.context, EachChatActivity::class.java)
            intent.putExtra(USER_KEY, user.user)
            intent.putExtra(INTENT_URI, intentUri)
            Timber.i("item is $item.")
            startActivity(intent)
        }

    }

    override fun onBackPressed() {
        if (intentUri != null) finish() else super.onBackPressed()
    }

    private fun fetchUsers(){
        val allUsers = firebaseDatabase.getReference("/users")
        allUsers.addListenerForSingleValueEvent(object : ValueEventListener{
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
                Timber.i("End: Size of adapter is ${adapter.groupCount}")
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