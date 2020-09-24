package com.example.messenger.create_group_details

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.messenger.*
import com.example.messenger.databinding.FragmentCreateGroupDetailsBinding
import timber.log.Timber

class CreateGroupDetailsFragment : Fragment() {

    lateinit var binding: FragmentCreateGroupDetailsBinding

    private val viewModel = lazy {
        ViewModelProvider(this, CreateGroupDetailsViewModelFactory(this)).get(CreateGroupDetailsViewModel::class.java)
    }.value

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_create_group_details, container, false)
        val participantsRecyclerView = binding.createGroupDetailsParticipantsRecyclerview
        val groupName = binding.createGroupDetailsName
        val image = binding.cameraImage
        val imageBackground = binding.cameraBackgroundImage

        binding.newGroupParticipantsNum.text = "Add subject"

        Timber.d("arguments is $arguments")
        if (arguments != null) {
            viewModel.participantsIntentArray.addAll(CreateGroupDetailsFragmentArgs.fromBundle(requireArguments()).usersList)
            viewModel.participantsIntentArray.forEach {
                val chosenMemberItem = CreateGroupDetailsViewModel.ChosenMemberItem(it)
                viewModel.adapter.add(chosenMemberItem)
            }
        }


        viewModel.participantsSize.observe(viewLifecycleOwner, {
            Timber.d("intent size is $it")
            binding.createGroupDetailsAdditionalDetails.text =
                "Participants: $it"
        })
        participantsRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 4, GridLayoutManager.VERTICAL, false)
        participantsRecyclerView.adapter = viewModel.adapter

        image.setOnClickListener { sendIntentForGroupLogo() }
        imageBackground.setOnClickListener { sendIntentForGroupLogo() }

        binding.acceptBtn.setOnClickListener {
            if (groupName.text.toString().isEmpty()) {
                requireContext().showShortToast("Group must have a name")
                return@setOnClickListener
            }

            viewModel.createGroup(binding.createGroupDetailsName.text.toString())
        }

        viewModel.shouldNavigate.observe(viewLifecycleOwner, {
            Timber.d("it is $it")
            if (it != null) {
                CreateGroupDetailsFragmentDirections.actionCreateGroupDetailsFragmentToEachGroupChatFragment(it)
            }
        })

        return binding.root
    }



    private fun sendIntentForGroupLogo(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1234)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == Activity.RESULT_OK && data != null){
            viewModel.groupUri = data.data
            binding.cameraImage.alpha = 0F
            binding.cameraBackgroundImage.alpha = 0F
            binding.groupImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(viewModel.groupUri)
                .into(binding.groupImage)
        }
    }


}


//            val myRefAccount = firebaseDatabase.getReference("users/${firebaseAuth.uid}")
//            myRefAccount.addValueEventListener(object : ValueEventListener {
//                override fun onCancelled(error: DatabaseError) {
//                    Timber.e(error.message)
//                }
//
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val myAccount = snapshot.getValue(User::class.java)
//                    if (myAccount == null) {
//                        Timber.i("My account is null")
//                        return
//                    }
//                    group.members.forEach {
////                        val eachUserRef = firebaseDatabase.getReference("")
//                        val groupsIncludedIn = firebaseDatabase.getReference("user-groups/${it.uid}/$uid")
//                        groupsIncludedIn.setValue(uid)
////                            firebaseDatabase.getReference("user-messages/${it.uid}/$uid").push()
////                        val dummyMessage = EachMessage(
////                            eachUserRef.key!!,
////                            firebaseAuth.uid!!,
////                            it.uid,
////                            null,
////                            "This is a sample message. You can begin chatting.",
////                            System.currentTimeMillis() / 1000,
////                            myAccount.userName,
////                            myAccount.profilePictureUrl,
////                            it,
////                            myAccount
////                        )
////                        eachUserRef.setValue(dummyMessage)
//                    }
//                }
//            })