package com.example.tournote.Functionality.Segments.ChatRoom

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.tournote.GlobalClass
import com.example.tournote.Groups.Activity.GroupSelectorActivity
import com.example.tournote.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.Random
import kotlin.jvm.java

class MyFirebaseMessagingService : FirebaseMessagingService() {

    var grpName = "Anonymous"

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FirebaseMessaging", "From: ${message.from}")
        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        val groupId = message.data["group_id"]
        Log.d("grpadapater", "Title: $title")
        Log.d("grpadapater", "Body: $body")

        /*for (grp in GlobalClass.GroupDetails_Everything){
            if (grp.groupID == groupId){
                Log.d("grpadapater", "Group ID: ${grp.groupID}")
                Log.d("grpadapater", "Group Name: ${grp.name}")
                grpName = grp.name ?: "Anonymous"
                break
            }
        }*/

        showNotification(title, body,grpName)
    }


    fun getCustomViews(title: String? , body: String?, groupId: String?): RemoteViews{
        Log.d("message", "Title: $title")
        val remoteView = RemoteViews("com.example.tournote",R.layout.custom_notificaton)
        remoteView.setTextViewText(R.id.desc,body)
        remoteView.setTextViewText(R.id.title,title)
        remoteView.setTextViewText(R.id.grpName,groupId)

        return remoteView
    }

    private fun showNotification(title: String?, body: String?, groupId: String?) {
        Log.d("message", "Title show: $title")
    val intent = Intent(this, GroupSelectorActivity::class.java)
        val channelId = "notification_channel"
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        var builder: NotificationCompat.Builder = NotificationCompat.Builder(
            applicationContext,
            channelId
        )

            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        builder = builder.setContent(
            getCustomViews(title,body,groupId)
        )

        val notificationManager : NotificationManager? = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificatoinChannel = NotificationChannel(channelId,"Notify_app",
                NotificationManager.IMPORTANCE_HIGH)
            notificationManager!!.createNotificationChannel(notificatoinChannel)
        }

        notificationManager!!.notify(0,builder.build())

    }

}