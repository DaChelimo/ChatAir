package com.example.messenger.create_group_details

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide
import com.example.messenger.*
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

class CreateGroupDetailsViewModel(val fragment: CreateGroupDetailsFragment): ViewModel() {
    var participantsIntentArray = ArrayList<User>()
    val adapter = GroupAdapter<GroupieViewHolder>()
    var groupUri: Uri? = null
    var myAccount: User? = null

    private var _participantsSize = MutableLiveData<Int>()
    val participantsSize: LiveData<Int>
        get() = _participantsSize
    private var _shouldNavigate = MutableLiveData<BasicGroupData?>()
    val shouldNavigate: LiveData<BasicGroupData?>
        get() = _shouldNavigate

    fun getMyAccount() {
        firebaseDatabase.getReference("users/${firebaseAuth.uid}").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                myAccount = snapshot.getValue(User::class.java)
            }
        })
    }

    fun createGroup(groupName: String) {
        val mainRef = firebaseDatabase.getReference("/groups").push()
        val uid = mainRef.key.toString()
        Timber.d("group uid is $uid")
        val groupBasicDataRef = firebaseDatabase.getReference("/groups/$uid/basic_data")
//        val groupMessagesRef = firebaseDatabase.getReference("/groups/$uid/messages")

        if (groupUri == null) {
            val basicGroupData = BasicGroupData(
                groupIcon = null,
                groupName = groupName,
                groupMembers = participantsIntentArray,
                groupUid = uid,
                groupFormedTime = System.currentTimeMillis(),
                groupCreatedBy = myAccount?.userName.toString()
            )
            groupBasicDataRef.setValue(basicGroupData)
                .addOnSuccessListener {
                    _shouldNavigate.value = basicGroupData
                }
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
                            val basicGroupData = BasicGroupData(
                                groupIcon = it.toString(),
                                groupName = groupName,
                                groupMembers = participantsIntentArray,
                                groupUid = uid,
                                groupFormedTime = System.currentTimeMillis(),
                                groupCreatedBy = myAccount?.userName.toString()
                            )

                            groupBasicDataRef.setValue(basicGroupData)
                                .addOnSuccessListener {
                                    _shouldNavigate.value = basicGroupData
                                }

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

        fragment.requireContext().showLongToast("Success. Group created.")

        groupBasicDataRef.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
//                val basicGroupData = snapshot.getValue(BasicGroupData::class.java) ?: return
//                Timber.d("group name is ${basicGroupData.groupName}")
//
//                val intent = Intent(this@CreateGroupDetailsFragment, EachGroupChatFragment::class.java)
//                intent.putExtra(GROUP_KEY, basicGroupData)
//                startActivity(intent)
            }
        })
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