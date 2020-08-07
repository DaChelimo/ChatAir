package com.example.messenger

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityLatestMessagesBinding
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.messaging.FirebaseMessaging
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_item.view.*
import timber.log.Timber

class LatestMessagesActivity : AppCompatActivity() {

    lateinit var binding: ActivityLatestMessagesBinding
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true

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
            item as LatestMessageItem
            if (item.chatMessage.receiverAccount == null){
                Timber.d("item.chatMessage.friendUser is null")
                return@setOnItemClickListener
            }

            val intent = Intent(view.context, EachChatActivity::class.java)
            intent.putExtra(USER_KEY, if (item.chatMessage.senderAccount?.uid != firebaseAuth.uid) item.chatMessage.senderAccount else item.chatMessage.receiverAccount)
            Timber.i("item is ${item.chatMessage} and user is ${item.chatMessage}")
            startActivity(intent)

        }

        listenForLatestMessages()
    }

    val latestMessagesMap = HashMap<String, EachMessage>()

    fun refreshRecyclerViewMessages(){
        adapter.clear()
        Timber.i("latestMessagesMap size is ${latestMessagesMap.values.size}")
        latestMessagesMap.values.forEach {
            adapter.add(LatestMessageItem(it))
        }
    }


    private fun listenForLatestMessages(){
        latestMessagesMap.clear()
        val ref = firebaseDatabase.getReference("/latest-messages/${firebaseAuth.uid}")
        ref.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                listenForLatestMessages()
//                val chatMessage = snapshot.getValue(EachMessage::class.java) ?: return
//
//                if (chatMessage.fromId == firebaseAuth.uid && chatMessage.toId == firebaseAuth.uid){
//                    Timber.d("Same account in latest activity")
//                }
//                else {
//                    latestMessagesMap[chatMessage.id] = chatMessage
//                    refreshRecyclerViewMessages()
//                }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val chatMessage = snapshot.getValue(EachMessage::class.java) ?: return

                if (chatMessage.fromId == firebaseAuth.uid && chatMessage.toId == firebaseAuth.uid){
                    Timber.d("Same account in latest activity")
                }
                else {
                    latestMessagesMap[chatMessage.id] = chatMessage
                    refreshRecyclerViewMessages()
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }
        })
        refreshRecyclerViewMessages()
    }

    class LatestMessageItem(val chatMessage: EachMessage): Item<GroupieViewHolder>(){
        override fun getLayout(): Int = R.layout.latest_messages_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val layout = viewHolder.itemView

//            layout.latest_item_username.text = chatMessage.username
//            Glide.with(layout.context)
//                .load(chatMessage.profilePictureUrl)
//                .placeholder(R.drawable.ic_baseline_person_24)
//                .into(layout.latest_item_image)

//            layout.latest_item_username.text = chatMessage.friendUser?.userName
//            Glide.with(layout.context)
//                .load(chatMessage.friendUser?.profilePictureUrl)
//                .placeholder(R.drawable.ic_baseline_person_24)
//                .into(layout.latest_item_image)

            val otherAccount = if (chatMessage.senderAccount?.uid != firebaseAuth.uid) chatMessage.senderAccount else chatMessage.receiverAccount

            Timber.d("otherAccount.username is ${otherAccount?.userName}")

            layout.latest_item_username.text = otherAccount?.userName
            Glide.with(layout.context)
                .load(otherAccount?.profilePictureUrl)
                .placeholder(R.drawable.ic_baseline_person_24)
                .into(layout.latest_item_image)

            var formatString = if (chatMessage.textMessage.length > 33) chatMessage.textMessage.substring(0..32) else chatMessage.textMessage
            if(chatMessage.textMessage.length > 27){
                formatString += "..."
            }

            if (chatMessage.imageUrl == null) {
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
            R.id.sign_out -> {
                firebaseAuth.signOut()
                val intent = Intent(this, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            else -> false

        }
    }
}