package com.example.messenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityCreateGroupDetailsBinding
import com.example.messenger.each_group_chat.EachGroupChatFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.create_group_chosen_member.view.*
import timber.log.Timber
import java.util.*

class CreateGroupDetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityCreateGroupDetailsBinding
    var participantsIntentArray = ArrayList<User>()
    val adapter = GroupAdapter<GroupieViewHolder>()
    var groupUri: Uri? = null

    companion object{
        var myAccount: User? = null

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_group_details)
        val participantsRecyclerView = binding.createGroupDetailsParticipantsRecyclerview
        val groupName = binding.createGroupDetailsName
        val image = binding.cameraImage
        val imageBackground = binding.cameraBackgroundImage

        firebaseDatabase.getReference("users/${firebaseAuth.uid}").addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                myAccount = snapshot.getValue(User::class.java)
            }
        })

//        supportActionBar?.title = "New Group"
//        supportActionBar?.subtitle = "Add subject"

        if (intent != null) {
//            participantsIntentArray = intent.getParcelableArrayListExtra(PARTICIPANTS_DATA)
            participantsIntentArray =
                intent.getParcelableArrayListExtra(PARTICIPANTS_DATA) ?: arrayListOf()
            participantsIntentArray.forEach { eachUser ->
                val adapterItem = ChosenMemberItem(eachUser)
                adapter.add(adapterItem)
            }
        }
        Timber.d("intent size is ${participantsIntentArray.size}")
        binding.createGroupDetailsAdditionalDetails.text =
            "Participants: ${participantsIntentArray.size}"
        participantsRecyclerView.layoutManager =
            GridLayoutManager(this, 4, GridLayoutManager.VERTICAL, false)
        participantsRecyclerView.adapter = adapter

        image.setOnClickListener { sendIntentForGroupLogo() }
        imageBackground.setOnClickListener { sendIntentForGroupLogo() }

        binding.acceptBtn.setOnClickListener {
            if (groupName.text.toString().isEmpty()) {
                Toast.makeText(this, "Group must have a name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createGroup()
        }
    }

    private fun createGroup() {
        val mainRef = firebaseDatabase.getReference("/groups").push()
        val uid = mainRef.key.toString()
        Timber.d("group uid is $uid")
        val groupBasicDataRef = firebaseDatabase.getReference("/groups/$uid/basic_data")
//        val groupMessagesRef = firebaseDatabase.getReference("/groups/$uid/messages")

        if (groupUri == null) {
            groupBasicDataRef.setValue(
                BasicGroupData(
                    groupIcon = null,
                    groupName = binding.createGroupDetailsName.text.toString(),
                    groupMembers = participantsIntentArray,
                    groupUid = uid,
                    groupFormedTime = System.currentTimeMillis(),
                    groupCreatedBy = myAccount?.userName.toString()
                )
            )
            val myGroupsIncludedIn = firebaseDatabase.getReference("user-groups/${firebaseAuth.uid}/$uid")
            myGroupsIncludedIn.setValue(uid).addOnSuccessListener {
                Timber.d("Success. myUid is $uid")
            }

            participantsIntentArray.forEach {
                val groupsIncludedIn = firebaseDatabase.getReference("user-groups/${it.uid}/$uid")
                groupsIncludedIn.setValue(uid).addOnSuccessListener {
                    Timber.d("Success. uid is $uid")
                }
            }

        } else {
            val storageRef =
                FirebaseStorage.getInstance().getReference("/images/${UUID.randomUUID()}")
            storageRef.putFile(groupUri!!)
                .addOnFailureListener {
                    Timber.e(it)
                }
                .addOnSuccessListener {

                    storageRef.downloadUrl
                        .addOnFailureListener {
                            Timber.e(it)
                        }
                        .addOnSuccessListener {

                            groupBasicDataRef.setValue(
                                BasicGroupData(
                                    groupIcon = it.toString(),
                                    groupName = binding.createGroupDetailsName.text.toString(),
                                    groupMembers = participantsIntentArray,
                                    groupUid = uid,
                                    groupFormedTime = System.currentTimeMillis(),
                                    groupCreatedBy = myAccount?.userName.toString()
                                )
                            )

                            val myGroupsIncludedIn =
                                firebaseDatabase.getReference("user-groups/${firebaseAuth.uid}/$uid")

                            myGroupsIncludedIn.setValue(uid).addOnSuccessListener {
                                Timber.d("Success. myUid is $uid")
                            }

                            participantsIntentArray.forEach {
                                val groupsIncludedIn = firebaseDatabase.getReference("user-groups/${it.uid}/$uid")
                                groupsIncludedIn.setValue(uid).addOnSuccessListener {
                                    Timber.d("Success. uid is $uid")
                                }
                            }
                        }
                }
        }

        Toast.makeText(applicationContext, "Success. Group created.", Toast.LENGTH_LONG).show()

        groupBasicDataRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val basicGroupData = snapshot.getValue(BasicGroupData::class.java) ?: return
                Timber.d("group name is ${basicGroupData.groupName}")

                val intent = Intent(this@CreateGroupDetailsActivity, EachGroupChatFragment::class.java)
                intent.putExtra(GROUP_KEY, basicGroupData)
                startActivity(intent)
            }
        })
    }

    private fun sendIntentForGroupLogo(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 1234)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1234 && resultCode == Activity.RESULT_OK && data != null){
            groupUri = data.data
            binding.cameraImage.alpha = 0F
            binding.cameraBackgroundImage.alpha = 0F
            binding.groupImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(groupUri)
                .into(binding.groupImage)
        }
    }

    class ChosenMemberItem(val user: User): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.create_group_chosen_member

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView
            val image = layout.chosen_member_image
            val name = layout.chosen_member_name

            name.text = user.userName
            Glide.with(layout)
                .load(user.profilePictureUrl)
                .into(image)
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