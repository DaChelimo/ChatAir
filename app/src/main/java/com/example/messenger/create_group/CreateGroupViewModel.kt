package com.example.messenger.create_group

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.messenger.User
import com.example.messenger.firebaseAuth
import com.example.messenger.firebaseDatabase
import com.example.messenger.showShortToast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import timber.log.Timber

class CreateGroupViewModel(private val context: Context) : ViewModel() {
    val userListAdapter: GroupAdapter<GroupieViewHolder> = GroupAdapter()
    val chosenMembersAdapter: GroupAdapter<GroupieViewHolder> = GroupAdapter()

    val chosenMembersList = HashMap<String, ChosenMemberItem>()

    private var _totalGroupMembersCount = MutableLiveData<Int>()
    val totalGroupMembersCount: LiveData<Int>
        get() = _totalGroupMembersCount

    init {
        initClickListeners()
        fetchUsers()
    }

    private fun initClickListeners() {
        userListAdapter.setOnItemClickListener { item, _ ->
            val userListItem = item as UserListItem
            val chosenMember = userListItem.user
            if (chosenMembersList.containsKey(chosenMember.uid)) {
                context.showShortToast("User already added to group.")
                return@setOnItemClickListener
            }

            val chosenMemberItem = ChosenMemberItem(chosenMember)
            chosenMembersList[chosenMember.uid] = chosenMemberItem
            Timber.d("values.size are ${chosenMembersList.values.size}")
            chosenMembersAdapter.add(chosenMemberItem)
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
}