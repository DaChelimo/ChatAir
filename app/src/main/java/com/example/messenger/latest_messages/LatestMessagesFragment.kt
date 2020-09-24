package com.example.messenger.latest_messages

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.messenger.*
import com.example.messenger.databinding.FragmentLatestMessagesBinding
import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

class LatestMessagesFragment : Fragment() {

    lateinit var binding: FragmentLatestMessagesBinding
    private lateinit var viewModel: LatestMessagesViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        changeUserActivityToOnline()

        if (firebaseAuth.currentUser == null) {
            findNavController().navigate(R.id.action_latestMessagesFragment_to_registerFragment)
        }

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_latest_messages, container, false)
        viewModel = ViewModelProvider(this, LatestMessagesViewModelFactory(this)).get(LatestMessagesViewModel::class.java)
        val latestMessageRecyclerView = binding.latestRecyclerView

        viewModel.listenForLatestMessages()
        latestMessageRecyclerView.adapter = viewModel.adapter
        viewModel.subscribeToTopic()

        viewModel.adapter.setOnItemClickListener { item, view ->
            if (item is LatestMessageItem) {
                if (item.chatPersonalMessage.receiverAccount == null) {
                    Timber.d("item.chatMessage.friendUser is null")
                    return@setOnItemClickListener
                }

                val account =  if (item.chatPersonalMessage.senderAccount?.uid != firebaseAuth.uid) item.chatPersonalMessage.senderAccount else item.chatPersonalMessage.receiverAccount
                Timber.d("account is $account")
                findNavController().navigate(LatestMessagesFragmentDirections.actionLatestMessagesFragmentToEachPersonalChatFragment(account ?: return@setOnItemClickListener))

            }
            else{
                item as GroupMessageItem

                Timber.d("group data is ${item.basicGroupData}")
                findNavController().navigate(LatestMessagesFragmentDirections.actionLatestMessagesFragmentToEachGroupChatFragment(item.basicGroupData))
            }
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if (firebaseAuth.currentUser == null) {
            findNavController().navigate(R.id.action_latestMessagesFragment_to_registerFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.latest_messages_menu, menu)
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when(item.itemId){
//            R.id.add_new_user -> {
//                val intent = Intent(this, NewUsersActivity::class.java)
//                startActivity(intent)
//
//                true
//            }
//            R.id.my_account -> {
//                val intent = Intent(this, UserProfileActivity::class.java)
//                startActivity(intent)
//                true
//            }
//            R.id.create_group -> {
//                val intent = Intent(this, CreateGroupActivity::class.java)
//                startActivity(intent)
//                true
//            }
//            else -> false
//
//        }
//    }

}