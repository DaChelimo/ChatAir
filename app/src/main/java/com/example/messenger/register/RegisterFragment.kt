package com.example.messenger.register

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bigbangbutton.editcodeview.EditCodeView
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.databinding.FragmentRegisterBinding
import com.example.messenger.login.LoginFragment
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber

class RegisterFragment : Fragment() {

    lateinit var binding: FragmentRegisterBinding
    lateinit var viewModel: RegisterViewModel

    lateinit var userName: EditText
    lateinit var phoneNumber: EditText
    lateinit var registerBtn: Button

//    var imageUrl: String = "https://thumbs.dreamstime.com/b/default-avatar-profile-vector-user-profile-default-avatar-profile-vector-user-profile-profile-179376714.jpg"
    var user: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)
        userName = binding.usernameEdit
        registerBtn = binding.registerBtn
        phoneNumber = binding.emailEdit
        binding.selectPhotoBtn.alpha = 0F
        binding.selectPhotoText.visibility = View.GONE

        viewModel = ViewModelProvider(this, RegisterViewModelFactory(this)).get(RegisterViewModel::class.java)

        Glide.with(this)
            .load("https://thumbs.dreamstime.com/b/default-avatar-profile-vector-user-profile-default-avatar-profile-vector-user-profile-profile-179376714.jpg")
            .placeholder(R.drawable.ic_placeholder_person_24)
            .into(binding.circleImage)

        binding.signinText.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.selectPhotoBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        registerBtn.setOnClickListener {
            doRegister()
        }

        viewModel.shouldNavigate.observe(viewLifecycleOwner, {
            Timber.d("shouldNavigate is $it")
            if (it) {
                findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToLatestMessagesFragment())
            }
        })

        binding.enterCodeBtn.setOnClickListener {
            if (storedVerificationId == null) {
                showShortToast("You must first sign up to receive code.")
                return@setOnClickListener
            }

            val dialogView = layoutInflater.inflate(
                R.layout.enter_code_layout,
                null
            )

            val enterCodeBtn: Button = dialogView.findViewById(R.id.enter_code_submit_btn)
            val cancelBtn: Button = dialogView.findViewById(R.id.enter_code_cancel_btn)
            val inputCode: EditCodeView = dialogView.findViewById(R.id.enter_code_input)

            val alert = AlertDialog.Builder(this.requireContext())
                .setView(dialogView)
                .setCancelable(true)

            val customAlert = alert.create()
            customAlert.show()

            enterCodeBtn.setOnClickListener {
                if (inputCode.codeLength != 6) {
                    showShortToast("Code is invalid. Try again")
                    Timber.d("Code is too short")
                    inputCode.clearCode()
                } else {
                    showShortToast("Code is being processed.")
                    val credential =
                        PhoneAuthProvider.getCredential(storedVerificationId!!, inputCode.code)
                    viewModel.signInWithCredential(credential, userName.text.toString(), phoneNumber.text.toString().trim())
                    customAlert.dismiss()
                }
            }

            cancelBtn.setOnClickListener {
                customAlert.dismiss()
            }
        }

        return binding.root
    }

    private fun Fragment.showShortToast(text: String) {
        Toast.makeText(this.requireContext(), text, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            Timber.i("scheme is ${data.data?.scheme}")
            viewModel.imageUri = data.data
            Timber.i("imageUri is ${viewModel.imageUri}")
            Glide.with(this)
                .load(viewModel.imageUri)
                .into(binding.circleImage)
            binding.selectPhotoBtn.alpha = 0F
            binding.selectPhotoText.visibility = View.GONE
        }
    }

    private fun doRegister(){
        if (userName.text.isEmpty()){
            showShortToast( "Invalid input")
            return
        }

        showShortToast( "Processing input...")
        viewModel.verifyPhoneNumber(phoneNumber.text.toString().trim(), userName.text.toString())
    }


}