package com.example.messenger.both_profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.databinding.ActivityUserProfileBinding
import timber.log.Timber

class UserProfileFragment : Fragment() {

    lateinit var binding: ActivityUserProfileBinding
    var myAccount: User? = null
    private var imageUrl: String? = null

    private val viewModel = lazy {
        ViewModelProvider(viewModelStore, UserProfileViewModelFactory(this)).get(UserProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_user_profile, container, false)

        binding.backBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.value.currentAccount.observe(viewLifecycleOwner, {
            loadData()
        })

        binding.userProfileSaveBtn.setOnClickListener {
            viewModel.value.updateUserInFirebaseDatabase()
        }

        binding.userProfileDiscardChangesBtn.setOnClickListener {
            Timber.d("myAccount is $myAccount")
            if (myAccount == null) return@setOnClickListener
            resetData()
        }


        viewModel.value.modifiedAccount.observe(viewLifecycleOwner, {
            Timber.d("it == viewModel.value.currentAccount.value is ${it == viewModel.value.currentAccount.value}")
            if (it != viewModel.value.currentAccount.value) {
                binding.userProfileDiscardChangesBtn.visibility = View.VISIBLE
                binding.userProfileSaveBtn.visibility = View.VISIBLE
            }
            else {
                binding.userProfileDiscardChangesBtn.visibility = View.GONE
                binding.userProfileSaveBtn.visibility = View.GONE
            }
        })

        return binding.root
    }


    private fun loadData() {
        resetData()

        binding.userprofileDisplayNameText.addTextChangedListener(viewModel.value.userNameTextWatcher)
        binding.userprofileDisplayAboutText.addTextChangedListener(viewModel.value.aboutTextWatcher)

        profilePictureClickListener()
    }

    private fun resetData() {
        val user = viewModel.value.currentAccount.value ?: return
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

            viewModel.value.uploadImageToFireStoreStorage(imageUri)

        }
    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.user_profile_menu, menu)

        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sign_out){

            val alertDialog = AlertDialog.Builder(requireContext())
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
//        val intent = Intent(this, RegisterFragment::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(intent)
    }
}