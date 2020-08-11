package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityUserProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class UserProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityUserProfileBinding
    var myAccount: User? = null
    var friendUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)

        val toolbar = binding.userProfileToolbar
        val intent = intent
        if (intent?.data != null){
            friendUser = intent.getParcelableExtra(FRIEND_USER_PROFILE)
            loadFriendUser()
            Timber.d("intent data is not null and data is $friendUser.")
        } else{
            Timber.d("intent data is null.")
            loadCurrentUser()
        }

        toolbar.title = "Profile"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)

        toolbar.setNavigationOnClickListener {
            if (intent?.data != null) {
                val navIntent = Intent(this, EachChatActivity::class.java)
                startActivity(navIntent)
                finish()
            }
            else{
                val navIntent = Intent(this, LatestMessagesActivity::class.java)
                startActivity(navIntent)
                finish()
            }
        }

        setSupportActionBar(toolbar)

    }

    fun loadData(user: User){
        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(binding.userprofileProfilePicture)

        binding.userprofileDisplayAboutText.setText(user.aboutDescription ?: "Hey there. I'm using Kotlin Messenger.")
        binding.userprofileDisplayNameText.setText(user.userName)
        binding.userprofileDisplayPhoneText.text = user.phoneNumber

    }

    private fun loadFriendUser(){
        val ref = firebaseDatabase.getReference("/users/${friendUser?.uid}")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return

                loadData(user)
            }
        })
    }

    private fun loadCurrentUser(){
        val ref = firebaseDatabase.getReference("/users/${firebaseAuth.uid}")
        ref.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.details)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java) ?: return

                myAccount = user
                loadData(user)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_profile_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out){

            val alertDialog = AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out.")
                .setPositiveButton("Yes") { _, _ -> signOutUser() }
                .setNegativeButton("No") { _, _ -> }
                .create()

            alertDialog.show()

            return true
        }
        return false
    }

    private fun signOutUser(){
        firebaseAuth.signOut()
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}