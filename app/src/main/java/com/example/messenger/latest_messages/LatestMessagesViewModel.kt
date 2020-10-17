package com.example.messenger.latest_messages

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.example.messenger.*
import com.google.firebase.database.*
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import timber.log.Timber

class LatestMessagesViewModel (private val fragment: Fragment) : ViewModel() {

    val adapter = GroupAdapter<GroupieViewHolder>()
    val latestMessagesMap = HashMap<String, Item<GroupieViewHolder>>()

    fun subscribeToTopic() {
        firebaseMessaging.subscribeToTopic("/topics/${firebaseAuth.uid}")

        firebaseInstanceId.instanceId
            .addOnSuccessListener {task ->
                Timber.d("Success. Token is ${task.token}")
                sendUserToToDatabase(task.token)
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }

    fun refreshRecyclerViewMessages(){
        Timber.i("latestMessagesMap size is ${latestMessagesMap.values.size}")
        adapter.update(latestMessagesMap.values)
    }

    fun getUserGroups(){
        val userGroupsRef = firebaseDatabase.getReference("user-groups/${firebaseAuth.uid}")

        userGroupsRef.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val uid = snapshot.getValue(String::class.java).toString()
                Timber.d("Retrieved uid: $uid")
                getGroupFromGroupUid(uid)
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val uid = snapshot.getValue(String::class.java).toString()
                Timber.d("Retrieved uid: $uid")
                getGroupFromGroupUid(uid)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
        })

    }

    fun getGroupFromGroupUid(uid: String) {
        val groupBasicDataRef = firebaseDatabase.getReference("/groups/$uid/basic_data")
        val groupLatestMessages = firebaseDatabase.getReference("/groups/$uid/latest-message")

        var latestMessage: EachGroupMessage? = null

        groupLatestMessages.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                val eachGroupMessage = snapshot.getValue(EachGroupMessage::class.java)
                Timber.d("eachGroupMessage is $eachGroupMessage")
                latestMessage = eachGroupMessage
                getGroupDataAndAddToAdapter(groupBasicDataRef, latestMessage)
                return
            }
        })

        getGroupDataAndAddToAdapter(groupBasicDataRef, latestMessage)

    }

    fun getGroupDataAndAddToAdapter(groupBasicDataRef: DatabaseReference, latestMessage: EachGroupMessage?){
        groupBasicDataRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val basicGroupData = snapshot.getValue(BasicGroupData::class.java) ?: return
                latestMessagesMap[basicGroupData.groupUid] =
                    GroupMessageItem(basicGroupData, latestMessage)
                refreshRecyclerViewMessages()
            }
        })
    }

    fun listenForLatestMessages(){
        getUserGroups()
        val ref = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}")

        ref.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                listenForLatestMessages()
                val updatedChatMessage = snapshot.getValue(EachPersonalMessage::class.java) ?: return
                if (updatedChatMessage.id.isEmpty()) return

                if (updatedChatMessage.fromId == firebaseAuth.uid && updatedChatMessage.toId == firebaseAuth.uid){
                    Timber.d("Same account in latest activity")
                    return
                }

                latestMessagesMap[updatedChatMessage.id] =
                    LatestMessageItem(updatedChatMessage)
                refreshRecyclerViewMessages()
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val chatMessage = snapshot.getValue(EachPersonalMessage::class.java) ?: return
                if (chatMessage.id.isEmpty()) return

                Timber.d("Latest message item is ${chatMessage}")
                if (chatMessage.fromId == firebaseAuth.uid && chatMessage.toId == firebaseAuth.uid){
                    Timber.d("Same account in latest activity")
                }
                else {
                    latestMessagesMap[chatMessage.id] =
                        LatestMessageItem(chatMessage)
                    refreshRecyclerViewMessages()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {     }
        })
    }


}