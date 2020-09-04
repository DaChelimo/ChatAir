package com.example.messenger

import android.content.Context
import android.net.ConnectivityManager
import android.os.Parcelable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import org.joda.time.LocalDate
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

val firebaseAuth = FirebaseAuth.getInstance()
val firebaseStorage = FirebaseStorage.getInstance() // for images
val firebaseDatabase = FirebaseDatabase.getInstance() // for data
val firebaseInstanceId = FirebaseInstanceId.getInstance()
val firebaseMessaging = FirebaseMessaging.getInstance()
val firebasePhoneAuth = PhoneAuthProvider.getInstance()

var storedVerificationId: String? = null
var resendingToken: PhoneAuthProvider.ForceResendingToken? = null

const val TOO_SHORT_MESSAGE = "The format of the phone number provided is incorrect. Please enter the phone number in a format that can be parsed into E.164 format. E.164 phone numbers are written in the format [+][country code][subscriber number including area code]. [ TOO_SHORT ]"
const val INVALID_FORMAT_MESSAGE = "The format of the phone number provided is incorrect. Please enter the phone number in a format that can be parsed into E.164 format. E.164 phone numbers are written in the format [+][country code][subscriber number including area code]. [ Invalid format. ]"

const val VOICE_CALL_USER = "VOICE_CALL_USER"
const val APPLICATION_KEY = "9a4edf60-85ec-4f50-9b25-d1e2f5ba27f9"
const val APPLICATION_SECRET = "2CkEH85ZGUioHyQok7Nzpw=="

@Parcelize
data class User(var userName: String, val uid: String, var profilePictureUrl: String, var aboutDescription: String?, val phoneNumber: String): Parcelable{
    constructor(): this("",  "", "", null, "")
}

@Parcelize
data class UserActivity(val uid: String, var isOnline: Boolean, var lastSeen: Long): Parcelable {
    constructor(): this("", false, -1)
}

fun changeUserActivityToOffline(){
    val cm = mainContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    val isConnected = activeNetwork?.isConnectedOrConnecting == true

    Timber.d("isConnected is $isConnected")

    if (firebaseAuth.uid != null){
        val ref = firebaseDatabase.getReference("users-activity/${firebaseAuth.uid}")
        val userActivity = UserActivity(firebaseAuth.uid.toString(), false, System.currentTimeMillis())

        ref.setValue(userActivity)
            .addOnSuccessListener {
                Timber.d("Success changing activity to offline.")
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }
    else{
        Timber.i("firebaseUid is null")
    }
}

fun changeUserActivityToOnline(){
    val cm = mainContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = cm.activeNetworkInfo
    val isConnected = activeNetwork?.isConnectedOrConnecting == true

    Timber.d("isConnected is $isConnected")

    if (firebaseAuth.uid != null){
        val ref = firebaseDatabase.getReference("users-activity/${firebaseAuth.uid}")
        val userActivity = UserActivity(firebaseAuth.uid.toString(), true, System.currentTimeMillis())

        ref.setValue(userActivity)
            .addOnSuccessListener {
                Timber.d("Success changing activity to online.")
            }
            .addOnFailureListener {
                Timber.e(it)
            }
    }
    else{
        Timber.i("firebaseUid is null")
    }
}


// TODO: DO NOT TOUCH!!!!

fun formatCallTime(inputSeconds: Long): String{
    val hours = inputSeconds / 3600
    val minutes = (inputSeconds % 3600) /60
    val seconds = inputSeconds % 60
    val zero = 0L

    val formattedTime = when {
        hours != zero -> "$hours:$minutes:$seconds"
        else -> "$minutes:$seconds"
    }

    Timber.d("formatted time is $formattedTime")

    return formattedTime
}


fun convertTimeToLastSeenTime(timeInMillis: Long): String {
        val cleanTime = SimpleDateFormat("hh:mm", Locale.getDefault()).format(timeInMillis)

        val isYesterday = LocalDate.now().minusDays(1).compareTo(LocalDate(timeInMillis)) == 0
        val isToday = LocalDate.now().compareTo(LocalDate(timeInMillis)) == 0
        val isThisWeek = LocalDate.now().weekyear.compareTo(LocalDate(timeInMillis).weekyear) == 0

        Timber.d("day of week is ${LocalDate(timeInMillis).dayOfWeek().asShortText} and string is ${LocalDate(timeInMillis).dayOfWeek().asString} and and text is ${LocalDate(timeInMillis).dayOfWeek().asText}")

        return when {
            isToday -> {
                "last seen today at $cleanTime"
            }
            isYesterday -> {
                "last seen yesterday at $cleanTime"
            }
            isThisWeek -> {
                "last seen on ${LocalDate(timeInMillis).dayOfWeek().asShortText} at $cleanTime"
            }
            else -> {
                "last seen ${SimpleDateFormat("dd/mm/yyyy", Locale.getDefault()).format(timeInMillis)} at $cleanTime"
            }
        }
}

data class TokenClass(val userName: String, val uid: String, val token: String){
    constructor(): this("", "", "")
}

@Parcelize
data class BasicGroupData(val groupIcon: String?, val groupName: String, val groupMembers: ArrayList<User>, val groupUid: String, val groupFormedTime: Long, val groupCreatedBy: String): Parcelable{
    constructor(): this(null, "", arrayListOf(), "", -1, "")
}

data class GroupLatestMessage(val basicGroupData: BasicGroupData, val message: EachGroupMessage)

data class NotificationBody(val title: String, val message: String)
data class Notification(val to: String, val data: NotificationBody)

data class FCMData(val to: String, val actualDataMap: DataMap)

fun sendUserToToDatabase(token: String){
    Timber.d("Token: $token")

    val databaseRef = firebaseDatabase.getReference("/user/${firebaseAuth.uid}")
    databaseRef.addValueEventListener(object : ValueEventListener{
        override fun onCancelled(error: DatabaseError) {
            Timber.e(error.details)
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val user = snapshot.getValue(User::class.java) ?: return

            val ref = firebaseDatabase.getReference("/tokens/${firebaseAuth.uid}}")
            if (firebaseAuth.uid == null) return
            ref.setValue(TokenClass(user.userName, firebaseAuth.uid!!, token))
        }
    })
}

const val USER_KEY = "USER_KEY"
const val INTENT_URI = "INTENT_URI"
const val FRIEND_USER_PROFILE = "FRIEND_USER_PROFILE"
const val PARTICIPANTS_DATA = "PARTICIPANTS_DATA"
const val GROUP_KEY = "GROUP_KEY"

enum class TIME_FORMATS{SHOW_MINUTE, SHOW_DAY, SHOW_DATE}

fun formatTimeForAdapter(timeInMillis: Long): String {
    val locale = Locale.getDefault()
    val currentDay = SimpleDateFormat("EEEE", locale).format(System.currentTimeMillis())
    val givenDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(timeInMillis)

    val chosenTimeFormat: TIME_FORMATS = when (currentDay) {
        givenDay -> TIME_FORMATS.SHOW_MINUTE
        else -> TIME_FORMATS.SHOW_DATE
    }

    val formattedDate = if (chosenTimeFormat == TIME_FORMATS.SHOW_MINUTE){
        SimpleDateFormat("hh:mm", locale).format(timeInMillis)
    }
    else{
        SimpleDateFormat("dd/MM/yyyy", locale).format(timeInMillis)
    }

    Timber.d("Formatted date is $formattedDate")
    return formattedDate
}

fun getAllUnreadMessages(toId: String?): Int {
    val fromId = firebaseAuth.uid
    val ref = firebaseDatabase.getReference("/user-messages/$fromId/$toId")

    var totalUnread = 0
    val messageMap : HashMap<String?, EachPersonalMessage?> = HashMap()

    ref.addChildEventListener(object : ChildEventListener {
        override fun onCancelled(error: DatabaseError) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val eachPersonalMessage = snapshot.getValue(EachPersonalMessage::class.java)

            messageMap[eachPersonalMessage?.id] = eachPersonalMessage
            Timber.d("eachPersonalMessage?.wasRead is ${eachPersonalMessage?.wasRead}")
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val eachPersonalMessage = snapshot.getValue(EachPersonalMessage::class.java)

            messageMap[eachPersonalMessage?.id] = eachPersonalMessage
            Timber.d("eachPersonalMessage?.wasRead is ${eachPersonalMessage?.wasRead}")
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {}
    })

    messageMap.values.forEach {
        if (it != null){
            if (!it.wasRead) totalUnread++ else Timber.d("Was read is ${it.wasRead}")
        }
    }

    return totalUnread
}

fun convertTimeStampToAdapterTime(timeInMillis: Long): String {
    val locale = Locale.getDefault()
    val convertedTime = SimpleDateFormat("hh:mm", locale).format(timeInMillis)
    Timber.d("converted time is $convertedTime")

    return convertedTime
}

data class DataMap(var sendersName: String, var message: String)

@Parcelize
data class EachPersonalMessage(var id: String, val fromId: String, val toId: String, val imageUrl: String?, var textMessage: String, val timeStamp: Long, val username: String, val profilePictureUrl: String, val receiverAccount: User?, val senderAccount: User?, var wasRead: Boolean): Parcelable{
    constructor(): this("", "", "", null, "", -1, "", "", null, null, false)
}

@Parcelize
data class EachGroupMessage(var id: String, val fromId: String, val imageUrl: String?, var textMessage: String, val timeStamp: Long, val username: String, val profilePictureUrl: String, val receiverAccounts: ArrayList<User>, val senderAccount: User?, val wasRead: Boolean): Parcelable{
    constructor(): this("", "", null, "", -1, "", "", arrayListOf<User>(), null, false)
}

var mainContext: Context? = null