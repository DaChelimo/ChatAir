package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityLatestMessagesBinding
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_item.view.*
import timber.log.Timber
//import com.sinch.android.rtc.calling

class LatestMessagesActivity : AppCompatActivity() {

    lateinit var binding: ActivityLatestMessagesBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        changeUserActivityToOnline()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_latest_messages)
        val latestMessageRecyclerView = binding.latestRecyclerView

        listenForLatestMessages()

        latestMessageRecyclerView.adapter = adapter

//        if (firebaseInstanceId.token != null) sendUserToToDatabase(firebaseInstanceId.token!!)

        firebaseMessaging.subscribeToTopic("/topics/${firebaseAuth.uid}")

        firebaseInstanceId.instanceId
            .addOnSuccessListener {task ->
                Timber.d("Success. Token is ${task.token}")
                sendUserToToDatabase(task.token)
            }
            .addOnFailureListener {
                Timber.e(it)
            }

        adapter.setOnItemClickListener { item, view ->
            if (item is LatestMessageItem) {
                if (item.chatPersonalMessage.receiverAccount == null) {
                    Timber.d("item.chatMessage.friendUser is null")
                    return@setOnItemClickListener
                }

                val intent = Intent(view.context, EachPersonalChatActivity::class.java)
                intent.putExtra(
                    USER_KEY,
                    if (item.chatPersonalMessage.senderAccount?.uid != firebaseAuth.uid) item.chatPersonalMessage.senderAccount else item.chatPersonalMessage.receiverAccount
                )
                Timber.i("item is ${item.chatPersonalMessage} and user is ${item.chatPersonalMessage}")
                startActivity(intent)

            }
            else{
                item as GroupMessageItem

                Timber.d("group data is ${item.basicGroupData}")
                val intent = Intent(this, EachGroupChatActivity::class.java)
                intent.putExtra(GROUP_KEY, item.basicGroupData)
                startActivity(intent)
            }
        }
    }

    val latestMessagesMap = HashMap<String, Item<GroupieViewHolder>>()

    fun refreshRecyclerViewMessages(){
        Timber.i("latestMessagesMap size is ${latestMessagesMap.values.size}")
        adapter.update(latestMessagesMap.values)
    }

    private fun getUserGroups(){
        val userGroupsRef = firebaseDatabase.getReference("user-groups/${firebaseAuth.uid}")

        userGroupsRef.addChildEventListener(object : ChildEventListener{
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
//        userGroupsRef.addValueEventListener(object : ValueEventListener{
//            override fun onCancelled(error: DatabaseError) {
//                Timber.e(error.message)
//            }
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                snapshot.children.forEach {
//                    val uid = it.getValue(String::class.java).toString()
//                    Timber.d("Retrieved uid: $uid")
//                    getGroupFromGroupUid(uid)
//                }
//            }
//        })
    }

    private fun getGroupFromGroupUid(uid: String) {
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

    private fun getGroupDataAndAddToAdapter(groupBasicDataRef: DatabaseReference, latestMessage: EachGroupMessage?){
        groupBasicDataRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.message)
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val basicGroupData = snapshot.getValue(BasicGroupData::class.java) ?: return
                latestMessagesMap[basicGroupData.groupUid] = GroupMessageItem(basicGroupData, latestMessage)
                refreshRecyclerViewMessages()
            }
        })
    }

    private fun listenForLatestMessages(){
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

                latestMessagesMap[updatedChatMessage.id] = LatestMessageItem(updatedChatMessage)
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
                    latestMessagesMap[chatMessage.id] = LatestMessageItem(chatMessage)
                    refreshRecyclerViewMessages()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {     }
        })
    }

    class GroupMessageItem(val basicGroupData: BasicGroupData, val latestMessage: EachGroupMessage?): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.latest_messages_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView

            layout.latest_item_username.text = basicGroupData.groupName
            if (basicGroupData.groupIcon != null) {
                Glide.with(layout.context)
                    .load(basicGroupData.groupIcon)
                    .placeholder(R.drawable.ic_baseline_person_24)
                    .into(layout.latest_item_image)
            }
            else{
                layout.latest_item_image.setImageResource(R.drawable.group_default_image)
            }

            Timber.d("latest message is $latestMessage")

            if (latestMessage == null) {
                layout.latest_item_last_message.visibility = View.GONE
            }
            else{
                layout.latest_item_last_message.visibility = View.VISIBLE
                layout.latest_item_last_message.text = "${latestMessage.senderAccount?.userName}: ${latestMessage.textMessage}"
                layout.latest_item_time_sent.text = formatTimeForAdapter(latestMessage.timeStamp)
            }
        }
    }

    class LatestMessageItem(val chatPersonalMessage: EachPersonalMessage): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.latest_messages_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView

            val otherAccount = if (chatPersonalMessage.senderAccount?.uid != firebaseAuth.uid) chatPersonalMessage.senderAccount else chatPersonalMessage.receiverAccount

            Timber.d("otherAccount.username is ${otherAccount?.userName}")

            layout.latest_item_username.text = otherAccount?.userName

            if (chatPersonalMessage.wasRead) layout.latest_item_time_sent.setTextColor(viewHolder.itemView.resources.getColor(R.color.textGrey))
            else layout.latest_item_time_sent.setTextColor(viewHolder.itemView.resources.getColor(R.color.dateBlue))

            val toId = otherAccount?.uid
            val totalUnreadMessages = getAllUnreadMessages(toId)

            if (totalUnreadMessages > 0){
                layout.latest_item_unread_text.text = totalUnreadMessages.toString()
                layout.latest_item_unread_text.visibility = View.VISIBLE
                layout.latest_item_unread_background.visibility = View.VISIBLE
            } else{
                layout.latest_item_unread_text.visibility = View.GONE
                layout.latest_item_unread_background.visibility = View.GONE
            }

            layout.latest_item_time_sent.text = formatTimeForAdapter(chatPersonalMessage.timeStamp)

            Glide.with(layout.context)
                .load(otherAccount?.profilePictureUrl)
                .placeholder(R.drawable.ic_baseline_person_24)
                .into(layout.latest_item_image)

            var formatString = if (chatPersonalMessage.textMessage.length > 33) chatPersonalMessage.textMessage.substring(0..32) else chatPersonalMessage.textMessage
            if(chatPersonalMessage.textMessage.length > 27){
                formatString += "..."
            }

            if (chatPersonalMessage.imageUrl == null) {
                layout.latest_item_last_message.text = formatString
            }else {
                layout.latest_item_last_message.text = "Image"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.latest_messages_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.add_new_user -> {
                val intent = Intent(this, NewUsersActivity::class.java)
                startActivity(intent)

                true
            }
            R.id.my_account -> {
                val intent = Intent(this, UserProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.create_group -> {
                val intent = Intent(this, CreateGroupActivity::class.java)
                startActivity(intent)
                true
            }
            else -> false

        }
    }

}