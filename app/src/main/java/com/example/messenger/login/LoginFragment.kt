package com.example.messenger.login

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bigbangbutton.editcodeview.EditCodeView
import com.example.messenger.R
import com.example.messenger.databinding.FragmentLoginBinding
import com.example.messenger.storedVerificationId
import com.google.firebase.auth.PhoneAuthProvider
import timber.log.Timber

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        viewModel = ViewModelProvider(this, LoginViewModelFactory(this)).get(LoginViewModel::class.java)

        val phoneNumber = binding.loginUserEmail

        binding.loginButton.setOnClickListener {
            if (phoneNumber.text.isEmpty()){
                viewModel.showShortToast("Input fields cannot be empty")
                return@setOnClickListener
            }

            viewModel.showShortToast("Processing input...")
            viewModel.verifyPhoneNumber(phoneNumber.text.toString())
        }

        binding.loginEnterCodeBtn.setOnClickListener {
            if(storedVerificationId == null){
                viewModel.showShortToast("You must first sign up to receive code.")
                return@setOnClickListener
            }

            showCustomDialog()
        }

        viewModel.shouldNavigate.observe(viewLifecycleOwner, {
            if (it) Timber.d("Navigate to LatestMessages Fragment")//TODO: navigate to Latest
        })

        return binding.root
    }

    private fun showCustomDialog() {
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

        setDialogClickListeners(enterCodeBtn, inputCode, customAlert, cancelBtn)
    }

    private fun setDialogClickListeners(
        enterCodeBtn: Button,
        inputCode: EditCodeView,
        customAlert: AlertDialog,
        cancelBtn: Button
    ) {
        enterCodeBtn.setOnClickListener {
            if (inputCode.codeLength != 6) {
                viewModel.showShortToast("Code is invalid. Try again")
                Timber.d("Code is too short")
                inputCode.clearCode()
            } else {
                viewModel.showShortToast("Code is being processed.")
                val credential =
                    PhoneAuthProvider.getCredential(storedVerificationId!!, inputCode.code)
                viewModel.signInWithCredential(credential)
                customAlert.dismiss()
            }
        }

        cancelBtn.setOnClickListener {
            customAlert.dismiss()
        }
    }


}