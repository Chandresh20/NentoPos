package com.tjcg.nentopos

import android.app.*
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.io.IOException
import java.net.URL

const val ALL_ORDER_UPDATE = "ALL_ORDER_UPDATE"
const val VARIATION_UPDATE = "VARIATION_UPDATE"
const val CATEGORY_UPDATE = "CATEGORY_UPDATE"
const val PRODUCT_UPDATE = "PRODUCT_UPDATE"

const val ACTION_KEY = "action"
const val ORDER_ID_KEY = "order_id"
const val ACTION_UPDATE = "UPDATE"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FirebaseTokenRefereshed", token)
        Constants.firebaseToken = token
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        var dataMap = ""
        for (d in data) {
            dataMap += "${d.key}: ${d.value}\n"
        }
        message.data.get("action")
        Log.d("FirebaseMessage", dataMap)
        sendNotification(message.notification, data)
        val forOutlet = data["outlet_id"]
        if (forOutlet?.toInt() == Constants.selectedOutletId) {
            when (data["message"]) {
                ALL_ORDER_UPDATE -> {
                    Log.d("ALLORDERUPDATE", "${message.data[ACTION_KEY]} && $ACTION_UPDATE")
                    if (message.data[ACTION_KEY] == ACTION_UPDATE) {
                        val orderId = message.data[ORDER_ID_KEY]
                        Log.d("NotificationUpdate", "for order: $orderId")
                        MainActivity.orderRepository.getSingleOrderOnline(
                            this, Constants.selectedOutletId, orderId ?: "0")
                    } else {
                        MainActivity.orderRepository.getAllOrdersOnline(
                            this, Constants.selectedOutletId, 0,true)
                    }
                }
                VARIATION_UPDATE -> {

                }
                CATEGORY_UPDATE -> {
                    MainActivity.progressDialogRepository.showAlertDialog(
                        "New Category updates are ready for Menu, please click on sync button " +
                                "and reload all products")
                }
                PRODUCT_UPDATE  -> {
                    MainActivity.mainRepository.getNewProducts(
                        this, Constants.selectedOutletId, "uniqueId", MainActivity.deviceID)
                }
            }
        }
    }

    private fun sendNotification(
        notification: RemoteMessage.Notification?,
        data: Map<String, String>
    ):Boolean
    {
        val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT)
        val notificationBuilder = NotificationCompat.Builder(this, "channel_id")
            .setContentTitle(notification!!.title)
            .setContentText(notification.body)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setContentIntent(pendingIntent)
            .setContentInfo(notification.title)
            .setLargeIcon(icon)
            .setColor(Color.RED)
            .setLights(Color.RED, 1000, 300)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.mipmap.ic_launcher)
        try {
            val pictureURL = data["picture_url"]
            if (pictureURL != null && "" != pictureURL) {
                val url = URL(pictureURL)
                val bigPicture = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                notificationBuilder.setStyle(
                    NotificationCompat.BigPictureStyle().bigPicture(bigPicture).setSummaryText(
                        notification.body
                    )
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Notification Channel is required for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channel_id", "channel_name", NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "channel description"
            channel.setShowBadge(true)
            channel.canShowBadge()
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            notificationManager.createNotificationChannel(channel)
        }
        notificationManager.notify(0, notificationBuilder.build())
        return true
    }
}