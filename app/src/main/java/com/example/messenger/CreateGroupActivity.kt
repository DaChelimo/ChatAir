package com.example.messenger

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.messenger.databinding.ActivityCreateGroupBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.create_group_chosen_member.view.*
import kotlinx.android.synthetic.main.each_user_layout.view.*
import timber.log.Timber

class CreateGroupActivity : AppCompatActivity() {

    lateinit var binding: ActivityCreateGroupBinding
    lateinit var userListAdapter: GroupAdapter<GroupieViewHolder>
    lateinit var chosenMembersAdapter: GroupAdapter<GroupieViewHolder>
    var chosenMembersList = HashMap<String, ChosenMemberItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "New Group"
        supportActionBar?.subtitle = "Add participants"

        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_group)
        val userListRecyclerView = binding.createGroupRecyclerview
        val chosenMembersRecyclerView = binding.chosenMembersRecyclerview
        chosenMembersRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        userListAdapter = GroupAdapter()
        chosenMembersAdapter = GroupAdapter()

        userListRecyclerView.adapter = userListAdapter
        chosenMembersRecyclerView.adapter = chosenMembersAdapter

        Timber.i("Init: Size of adapter is ${userListAdapter.groupCount}")

        fetchUsers()

        userListAdapter.setOnItemClickListener { item, _ ->
            val userListItem = item as UserListItem
            val chosenMember = userListItem.user
            if (chosenMembersList.containsKey(chosenMember.uid.toString())) {
                Toast.makeText(this, "User already added to group.", Toast.LENGTH_SHORT).show()
            }
            chosenMembersList[chosenMember.uid] = ChosenMemberItem(chosenMember)
            Timber.d("values are ${chosenMembersList.values}")
            chosenMembersAdapter.update(chosenMembersList.values)
            supportActionBar?.subtitle = "${chosenMembersList.values.size} selected"
        }

        binding.proceedBtn.setOnClickListener {
            Timber.d("itemCount is ${chosenMembersAdapter.itemCount}")
            if (chosenMembersAdapter.itemCount == 0){
                Toast.makeText(this, "At least one contact must be selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, CreateGroupDetailsActivity::class.java)
            val chosenMembersIntentExtra = ArrayList<User>()
            chosenMembersList.values.forEach {
                chosenMembersIntentExtra.add(it.user)
            }
            intent.putExtra(PARTICIPANTS_DATA, chosenMembersIntentExtra)
            startActivity(intent)
        }
    }

    private fun fetchUsers(){
        val allUsers = firebaseDatabase.getReference("/users")
        allUsers.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Timber.i("error: ${error.details}")
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val firebaseUser = it.getValue(User::class.java)
                    Timber.i("firebaseUser is $firebaseUser")
                    Timber.d("firebaseUser.uid is ${firebaseUser?.uid} and firebaseAuth.uid is ${firebaseAuth.uid}")
                    if (firebaseUser != null && firebaseUser.uid != firebaseAuth.uid ){
                        userListAdapter.add(UserListItem(firebaseUser))
                        Timber.i("Item added in list.")
                    }
                }
                Timber.i("End: Size of adapter is ${userListAdapter.groupCount}")
            }
        })
    }

    class UserListItem(val user: User): Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.each_user_layout

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val image = viewHolder.itemView.each_user_image
            val userName = viewHolder.itemView.each_user_name

            val view = viewHolder.itemView
            Glide.with(view)
                .load(user.profilePictureUrl)
                .into(image)
            userName.text = user.userName
            Timber.i("each uid: ${user.uid}")

        }
    }

    class ChosenMemberItem(val user: User): Item<GroupieViewHolder>() {
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