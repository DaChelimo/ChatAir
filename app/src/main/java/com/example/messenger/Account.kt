package com.example.messenger

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.runBlocking
import timber.log.Timber

object Account {
    fun getAccount(uid: String? = firebaseAuth.uid): User? {
        var user: User? = null
        return runBlocking {
            val ref = firebaseDatabase.getReference("users/$uid")

            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    user = snapshot.getValue(User::class.java)
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.d(error.toException())
                }
            })

            return@runBlocking user
        }
    }
}