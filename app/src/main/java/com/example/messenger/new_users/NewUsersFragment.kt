package com.example.messenger.new_users

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.databinding.FragmentNewUsersBinding
import com.example.messenger.each_personal_chat.EachPersonalChatFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.each_user_layout.view.*
import timber.log.Timber

class NewUsersFragment : Fragment() {

    lateinit var binding: FragmentNewUsersBinding
    lateinit var viewModel: NewUsersViewModel
    var intentUri: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //        Timber.i("intent type is ${intent.type}")
//        if (Intent.ACTION_SEND == intent.action && intent.type != null){
//            intentUri = intent.getStringExtra(INTENT_URI)
//            Timber.d("intentUri is $intentUri")
//        }
//        else{
////            supportActionBar?.title = "Select User"
//        }

        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_new_users, container, false)
        viewModel = ViewModelProvider(this).get(NewUsersViewModel::class.java)
        val userRecyclerView = binding.usersRecyclerview


        userRecyclerView.adapter = viewModel.adapter

        Timber.i("Init: Size of adapter is ${viewModel.adapter.groupCount}")

        viewModel.fetchUsers()
        viewModel.adapter.setOnItemClickListener { item, _ ->

            val user = item as NewUsersViewModel.UserItem

//            val intent = Intent(view.context, EachPersonalChatFragment::class.java)
//            intent.putExtra(USER_KEY, user.user)
//            intent.putExtra(INTENT_URI, intentUri)
//            Timber.i("item is $item.")
//            startActivity(intent)
        }

        return binding.root
    }

//    override fun onBackPressed() {
//        if (intentUri != null) finish() else super.onBackPressed()
//    }




}