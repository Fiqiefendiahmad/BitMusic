package com.music.bitchord.android.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.bitchord.android.R

/**
 * Foreground media session service for now-playing notification and lock screen
 * controls. The actual [ExoPlayer] is owned by [PlaybackHolder] so in-app UI
 * and system controls share state.
 */
class PlaybackService : MediaSessionService() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        PlaybackHolder.ensure(applicationContext)
        ensureChannel()
        runCatching { addSession(PlaybackHolder.session) }
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.app_name)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_notification)
            }
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        PlaybackHolder.session

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Satisfy startForegroundService() contract promptly. Include transport
        // actions so next/prev/play are visible even before Media3 refreshes.
        ensureChannel()
        val session = runCatching { PlaybackHolder.session }.getOrNull()
        val player = session?.player
        val meta = player?.mediaMetadata
        val title = meta?.title?.toString()?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
        val text = meta?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Now playing"
        val playing = player?.isPlaying == true
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    packageManager.getLaunchIntentForPackage(packageName) ?: Intent(),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS),
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    if (playing) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY,
                ),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT),
            )
        session?.let {
            androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(it.sessionCompatToken)
                .setShowActionsInCompactView(0, 1, 2)
                .also { style -> builder.setStyle(style) }
        }
        ServiceCompat.startForeground(
            this,
            NOW_PLAYING_NOTIFICATION_ID,
            builder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
        return START_STICKY
    }

    override fun onDestroy() {
        // Keep shared player/session lifecycle managed by PlaybackHolder/app.
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        listOf("bitmusic_playback_v8", "bitmusic_playback_v9").forEach { id ->
            runCatching { nm.deleteNotificationChannel(id) }
        }
        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Now playing",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Playback controls and track information"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    companion object {
        // v10: Alerting channel (IMPORTANCE_DEFAULT, sound=null) ensures status-bar icon on One UI
        const val NOTIFICATION_CHANNEL_ID = "bitmusic_playback_v10"
        private const val NOW_PLAYING_NOTIFICATION_ID = 1000
    }
}
