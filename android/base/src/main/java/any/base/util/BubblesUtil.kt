package any.base.util

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object BubblesUtil {
    const val EXTRA_SKIP_PASSWORD = "com.dokar.any.extra.SKIP_PASSWORD"

    private const val BUBBLES_NOTIFICATION_ID = 1000

    private const val BUBBLES_NOTIFICATION_CHANNEL = "BUBBLES"

    private const val BUBBLES_SHORTCUT_CATEGORY = "com.dokar.any.category.BUBBLES"

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun isBubblesAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    fun showBubblesNotification(
        context: Context,
        targetActivity: Class<out Activity>,
        notificationTitle: String,
        notificationText: String,
        notificationIcon: Icon,
        bubblePersonName: String,
        bubbleIcon: Icon,
    ) {
        if (!isBubblesAvailable()) {
            return
        }
        // Create bubble intent
        val target = Intent(context, targetActivity).apply {
            putExtra(EXTRA_SKIP_PASSWORD, true)
        }
        val bubbleIntent = PendingIntent.getActivity(
            context,
            0, /* requestCode */
            target, /* intent */
            PendingIntent.FLAG_UPDATE_CURRENT, /* flags */
        )

        val person = Person.Builder()
            .setName(bubblePersonName)
            .setImportant(true)
            .build()

        // Create sharing shortcut
        val shortcutId = "SHORTCUT_FOR_SHARING"
        val shortcut = ShortcutInfo.Builder(context, shortcutId)
            .setCategories(setOf(BUBBLES_SHORTCUT_CATEGORY))
            .setIntent(Intent(Intent.ACTION_DEFAULT))
            .setLongLived(true)
            .setShortLabel(person.name ?: "")
            .build()

        // Create bubble metadata
        val bubbleData = Notification.BubbleMetadata.Builder(bubbleIntent, bubbleIcon)
            .setAutoExpandBubble(true)
            .setDesiredHeight(Int.MAX_VALUE)
            .build()

        val channel = NotificationChannel(
            BUBBLES_NOTIFICATION_CHANNEL,
            "Bubbles Intro",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(100L, 100L, 50L)
        }
        val style = Notification.MessagingStyle(person)
            .addMessage(
                Notification.MessagingStyle.Message(
                    notificationText,
                    System.currentTimeMillis(),
                    person,
                )
            )

        // Create notification, referencing the sharing shortcut
        val builder = Notification.Builder(context, BUBBLES_NOTIFICATION_CHANNEL)
            .setContentTitle(notificationTitle)
            .setSmallIcon(notificationIcon)
            .setBubbleMetadata(bubbleData)
            .setShortcutId(shortcutId)
            .addPerson(person)
            .setStyle(style)

        // Set shortcut
        context.getSystemService(ShortcutManager::class.java).dynamicShortcuts = listOf(shortcut)

        // Show bubble notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(BUBBLES_NOTIFICATION_ID, builder.build())
    }
}