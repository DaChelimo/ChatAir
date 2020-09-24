package com.example.messenger.notification_pack

data class NotificationBody(val title: String, val message: String) {
    constructor(): this("", "")
}
data class Notification(val to: String, val data: NotificationBody) {
    constructor(): this("", NotificationBody("empty", "empty"))
}

class MyResponse()