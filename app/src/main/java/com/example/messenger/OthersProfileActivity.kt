package com.example.messenger

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityOthersProfileBinding
import com.example.messenger.each_personal_chat.EachPersonalChatFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class OthersProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityOthersProfileBinding
    var friendUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_others_profile)

        Timber.d("intent is $intent")
        if (intent != null){
            friendUser = intent.getParcelableExtra(FRIEND_USER_PROFILE)
            loadFriendUser()
            Timber.d("intent data is not null and data is $friendUser.")
        }

//        this.supportActionBar?.title = friendUser?.userName
        loadToolbarData()

    }

    private fun loadToolbarData(){

        binding.otherProfileToolbarTitle.text = friendUser?.userName

        binding.otherProfileToolbarBackBtn.setOnClickListener {
            it.background = ResourcesCompat.getDrawable(resources, R.drawable.other_profile_back_onclick, null)
            Timber.d("Above navigation")
            val intent = Intent(this, EachPersonalChatFragment::class.java)
            intent.putExtra(USER_KEY, friendUser)
            Timber.d("Navigation called")
            startActivity(intent)
            finishAffinity()
        }
    }

    override fun onBackPressed() {
        val intent = Intent(this, EachPersonalChatFragment::class.java)
        intent.putExtra(USER_KEY, friendUser)
        Timber.d("Navigation called")
        startActivity(intent)
        finishAffinity()
    }

    fun loadData(user: User){
        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(binding.othersProfileProfilePicture)

        binding.othersProfileDisplayAboutText .text = user.aboutDescription ?: "Hey there. I'm using Kotlin Messenger."
        binding.othersProfileDisplayNameText.text = user.userName
        binding.othersProfileDisplayPhoneText.text = user.phoneNumber

    }

    private fun loadFriendUser(){
        val ref = firebaseDatabase.getReference("/users/${friendUser?.uid}")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("snapshot data is ${snapshot.getValue(User::class.java)}")
                val user = snapshot.getValue(User::class.java) ?: return

                loadData(user)
            }
        })
    }
}