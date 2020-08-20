package com.example.messenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityUserProfileBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.util.*

class UserProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityUserProfileBinding
    var myAccount: User? = null
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_profile)

        val toolbar = binding.userProfileToolbar
        val intent = intent

        loadCurrentUser()

        toolbar.title = "Profile"
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)

        toolbar.setNavigationOnClickListener {
            if (intent?.data != null) {
                val navIntent = Intent(this, EachPersonalChatActivity::class.java)
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

        binding.userProfileSaveBtn.setOnClickListener {
            updateUserInFirebaseDatabase()
        }

        binding.userProfileDiscardChangesBtn.setOnClickListener {
            Timber.d("myAccount is $myAccount")
            if (myAccount == null) return@setOnClickListener
            resetData(myAccount!!)
        }

    }

    fun loadData(user: User){

        resetData(user)

        val textWatcher = object : TextWatcher{
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForDataChange()
            }
        }

        binding.userprofileDisplayNameText.addTextChangedListener(textWatcher)
        binding.userprofileDisplayAboutText.addTextChangedListener(textWatcher)

        profilePictureClickListener()
    }

    private fun resetData(user: User) {
        Glide.with(this)
            .load(user.profilePictureUrl)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(binding.userprofileProfilePicture)

        binding.userprofileDisplayAboutText.setText(user.aboutDescription ?: "Hey there. I'm using Kotlin Messenger.")
        binding.userprofileDisplayNameText.setText(user.userName)
        binding.userprofileDisplayPhoneText.text = user.phoneNumber

        binding.userProfileDiscardChangesBtn.visibility = View.GONE
        binding.userProfileSaveBtn.visibility = View.GONE
    }

    private fun checkForDataChange(){
        val user = myAccount ?: return
        if (binding.userprofileDisplayNameText.text.toString() != user.userName || binding.userprofileDisplayAboutText.text.toString() !=  user.aboutDescription ?: "Hey there. I'm using Kotlin Messenger." || imageUrl ?: user.profilePictureUrl != user.profilePictureUrl){
            binding.userProfileDiscardChangesBtn.visibility = View.VISIBLE
            binding.userProfileSaveBtn.visibility = View.VISIBLE
        }
        else{
            binding.userProfileDiscardChangesBtn.visibility = View.GONE
            binding.userProfileSaveBtn.visibility = View.GONE
        }
    }

    private fun profilePictureClickListener(){
        binding.userprofileProfilePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            val imageUri = data.data ?: return

            Glide.with(this)
                .load(imageUri)
                .into(binding.userprofileProfilePicture)

            uploadImageToFireStoreStorage(imageUri)

        }
    }

    private fun uploadImageToFireStoreStorage(imageUri: Uri){
        val fileName = UUID.randomUUID().toString()
        val ref = firebaseStorage.getReference("/images/$fileName")

        ref.putFile(imageUri).addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener {
                Timber.i("downloadUrl is $it")
                imageUrl = it.toString()
                checkForDataChange()
            }
        }
    }



    private fun updateUserInFirebaseDatabase() {
        Toast.makeText(this, "Uploading data online", Toast.LENGTH_SHORT).show()
        val formerUser = myAccount
        formerUser?.userName = binding.userprofileDisplayNameText.text.toString()
        formerUser?.profilePictureUrl = imageUrl ?: myAccount?.profilePictureUrl.toString()
        formerUser?.aboutDescription = binding.userprofileDisplayAboutText.text.toString()

        val myRef = firebaseDatabase.getReference("users/${firebaseAuth.uid}")
        myRef.setValue(formerUser)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved", Toast.LENGTH_LONG).show()
                loadCurrentUser()
                checkForDataChange()
            }
            .addOnFailureListener {
                Timber.e(it)
            }
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
        changeUserActivityToOffline()
        firebaseAuth.signOut()
        val intent = Intent(this, RegisterActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}