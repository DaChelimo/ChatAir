package com.example.messenger.create_group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.messenger.R
import com.example.messenger.User
import com.example.messenger.databinding.FragmentCreateGroupBinding
import com.example.messenger.showShortToast
import timber.log.Timber

class CreateGroupFragment : Fragment() {

    lateinit var binding: FragmentCreateGroupBinding

    private val thisContext = lazy {
        this.requireContext()
    }.value

    @Suppress("RecursivePropertyAccessor")
    val viewModel = lazy {
        ViewModelProvider(this, CreateGroupViewModelFactory(thisContext)).get(CreateGroupViewModel::class.java)
    }.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_group, container, false)
        val userListRecyclerView = binding.createGroupRecyclerview
        val chosenMembersRecyclerView = binding.chosenMembersRecyclerview
        chosenMembersRecyclerView.layoutManager = LinearLayoutManager(this.requireContext(), LinearLayoutManager.HORIZONTAL, false)

        userListRecyclerView.adapter = viewModel.userListAdapter
        chosenMembersRecyclerView.adapter = viewModel.chosenMembersAdapter

        viewModel.totalGroupMembersCount.observe(viewLifecycleOwner, {
            binding.newGroupParticipantsNum.text = "$it selected"
        })



        binding.proceedBtn.setOnClickListener {
            if (viewModel.chosenMembersAdapter.itemCount == 0) {
                requireContext().showShortToast("At least one contact must be selected")
                return@setOnClickListener
            }

            val userList = ArrayList<User>()
            (viewModel.chosenMembersList.values as ArrayList<ChosenMemberItem>).forEach {
                userList.add(it.user)
            }
            Timber.d("list.size is ${userList.size}")
            findNavController().navigate(CreateGroupFragmentDirections.actionCreateGroupFragmentToCreateGroupDetailsFragment(userList.toTypedArray()))
        }

        return binding.root
    }



}