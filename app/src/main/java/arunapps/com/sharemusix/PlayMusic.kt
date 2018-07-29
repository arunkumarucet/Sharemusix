package arunapps.com.sharemusix

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import android.R.id.edit
import android.content.SharedPreferences
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.R.attr.description
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Color
import android.media.AudioAttributes


class PlayMusic : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener ,AudioManager.OnAudioFocusChangeListener{
    var status: Notification? = null
    var broadcastManager: LocalBroadcastManager? = null
    // Using RemoteViews to bind custom layouts into Notification
    var views: RemoteViews? = null
    var bigViews: RemoteViews? = null
    var idBinder = IDBinder()
    var seek = 0
    var mediaPlayer: MediaPlayer? = null
    var context: Context = this
    var handler: Handler? = null
    var file: String? = null
    var notifyID: Int = 1
    var CHANNEL_ID: String = "Song Details"
    var name: CharSequence = "Song Notification"
    var mNotificationManager: NotificationManager? = null
    var current_position: Int? = null
    var importance = NotificationManager.IMPORTANCE_HIGH
    var isfakestop: Boolean = true
    var mAudioManager: AudioManager? = null
    private val sendUpdates = Runnable {
        while (mediaPlayer != null) {
            sendElapsedTime()
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }
    fun PlayMusic(){
    }
    fun isPlaying(): Boolean {
        return mediaPlayer!!.isPlaying()
    }


    inner class IDBinder : Binder() {
        fun getService(): PlayMusic{
            return this@PlayMusic
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return idBinder
    }

    override fun onCreate() {
        startForeground(1, Notification())
        broadcastManager = LocalBroadcastManager.getInstance(applicationContext)
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val position = mypref.getInt("position", 0)
        try {
            file = DatabaseHandler(context).getcurrentlist(4)[position]
        }catch (e:Exception){
            e.printStackTrace()
        }
        stop()
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer!!.setDataSource(file)
            if (Build.VERSION.SDK_INT >= 21) {
                val aa:AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                mediaPlayer!!.setAudioAttributes(aa)
            } else {
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mAudioManager!!.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            mediaPlayer!!.setOnPreparedListener(this)
            mediaPlayer!!.setOnCompletionListener(this)
            mediaPlayer!!.prepareAsync()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onAudioFocusChange(focusChange: Int) {
        if(focusChange<=0) {
            //LOSS -> PAUSE
            try {
                if (mediaPlayer!!.isPlaying) {
                    seek = mediaPlayer!!.currentPosition
                    mediaPlayer!!.pause()
                    PLAYING_STATE = false
                    views!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_play)
                    bigViews!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_play)
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        } else {
            try {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.seekTo(seek)
                    mediaPlayer!!.start()
                    PLAYING_STATE = true
                    views!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_pause)
                    bigViews!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_pause)
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }
    override fun onDestroy() {
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            if(isfakestop) {
                val intent = Intent(this@PlayMusic, PlayMusic::class.java)
                intent.putExtra("check", "restart")
                intent.putExtra("seektime", mediaPlayer!!.currentPosition)
                mediaPlayer!!.stop()
                this.startForegroundService(intent)
            }else{
                isfakestop=true
            }
        }else{
            mediaPlayer!!.stop()
        }
    }

    override fun onUnbind(intent: Intent): Boolean {
        stop()
        return super.onUnbind(intent)
    }

    override fun onStart(intent: Intent, startid: Int) {

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == Constants.ACTION.STARTFOREGROUND_ACTION) {
            showNotification()
        } else if (intent.action == Constants.ACTION.PREV_ACTION) {
            try {
                val mypref = PreferenceManager.getDefaultSharedPreferences(this)
                val old_position = mypref.getInt("position", 0)
                val prefsEditr = mypref.edit()
                if (old_position > 0) {
                    prefsEditr.putInt("position", old_position - 1)
                    prefsEditr.apply()
                    onCreate()
                    showNotification()
                } else {
                    prefsEditr.putInt("position", 0)
                    prefsEditr.apply()
                }
            }catch(e:Exception){
                e.printStackTrace()
            }
            /**
            Toast.makeText(context,mypref.getInt("position",0).toString(),Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Clicked Previous", Toast.LENGTH_SHORT).show()
            **/
        } else if (intent.action == Constants.ACTION.PLAY_ACTION) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    seek = mediaPlayer!!.currentPosition
                    mediaPlayer!!.pause()
                    PLAYING_STATE=false
                    views!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_play)
                    bigViews!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_play)
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                        mNotificationManager!!.notify(notifyID,status)
                    }else{
                        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,status)
                    }
                } else if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.seekTo(seek)
                    mediaPlayer!!.start()
                    PLAYING_STATE=true
                    views!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_pause)
                    bigViews!!.setImageViewResource(R.id.status_bar_play,
                            android.R.drawable.ic_media_pause)
                    if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
                        mNotificationManager!!.notify(notifyID,status)
                    }else{
                        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,status)
                    }
                } else {
                    val serviceIntent = Intent(context, PlayMusic::class.java)
                    serviceIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        context.startService(serviceIntent)
                    context.stopService(serviceIntent)
                    serviceIntent.action = Constants.ACTION.STARTFOREGROUND_ACTION
                    context.startService(serviceIntent)
                    PLAYING_STATE = true
                }
            }catch(e:Exception){
                e.printStackTrace()
            }
        } else if (intent.action == Constants.ACTION.NEXT_ACTION) {
            try {
                val mypref = PreferenceManager.getDefaultSharedPreferences(this)
                val old_position = mypref.getInt("position", 0)
                val prefsEditr = mypref.edit()
                if (old_position >= 0 && old_position < DatabaseHandler(context).getcurrentcount() - 1) {
                    prefsEditr.putInt("position", old_position + 1)
                    prefsEditr.apply()
                    onCreate()
                    showNotification()
                } else {
                    prefsEditr.putInt("position", 0)
                    prefsEditr.apply()
                }
            }catch(e:Exception){
                e.printStackTrace()
            }
            /**
            Toast.makeText(context,mypref.getInt("position",0).toString(),Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Clicked Next", Toast.LENGTH_SHORT).show()
            **/
        } else if (intent.action == Constants.ACTION.STOPFOREGROUND_ACTION) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mediaPlayer!!.reset()
                    mediaPlayer!!.stop()
                    mediaPlayer!!.release()
                    mediaPlayer = null
                    try {
                        mNotificationManager!!.cancel(notifyID)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    isfakestop = false
//                Toast.makeText(this,"stopped",Toast.LENGTH_LONG).show()
                    stopSelf()
                } else {
//                Toast.makeText(this, "Service Stoped", Toast.LENGTH_SHORT).show()
                    stopForeground(true)
                    stopSelf()
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }else if(intent.action == Constants.ACTION.PLAY_WITH_SEEK){
            val seekbarpos = intent.extras.getInt("seekpos")
            mediaPlayer!!.seekTo(seekbarpos)
        }
        try {
            if (intent.extras.getString("check") == "restart")
                current_position = intent.extras.getInt("seektime")
        }catch (e: Exception){
            e.printStackTrace()
        }
        return Service.START_STICKY
    }
    fun mediaplaying() : Boolean{
        if(mediaPlayer!!.isPlaying)
            return true
        else
            return false
    }
    fun showNotification() {
        views = RemoteViews(packageName,
                R.layout.status_bar)
        bigViews = RemoteViews(packageName,
                R.layout.status_bar_expanded)

        // showing default album image
        views!!.setViewVisibility(R.id.status_bar_icon, View.VISIBLE)
        views!!.setViewVisibility(R.id.status_bar_album_art, View.GONE)
        bigViews!!.setImageViewBitmap(R.id.status_bar_album_art,
                Constants.getDefaultAlbumArt(this))

        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Constants.ACTION.MAIN_ACTION
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

        val previousIntent = Intent(this, PlayMusic::class.java)
        previousIntent.action = Constants.ACTION.PREV_ACTION
        val ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0)

        val playIntent = Intent(this, PlayMusic::class.java)
        playIntent.action = Constants.ACTION.PLAY_ACTION
        val pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0)

        val nextIntent = Intent(this, PlayMusic::class.java)
        nextIntent.action = Constants.ACTION.NEXT_ACTION
        val pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0)

        val closeIntent = Intent(this, PlayMusic::class.java)
        closeIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
        val pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, 0)

        views!!.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent)
        bigViews!!.setOnClickPendingIntent(R.id.status_bar_play, pplayIntent)


        views!!.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent)
        bigViews!!.setOnClickPendingIntent(R.id.status_bar_next, pnextIntent)

        views!!.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent)
        bigViews!!.setOnClickPendingIntent(R.id.status_bar_prev, ppreviousIntent)

        views!!.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent)
        bigViews!!.setOnClickPendingIntent(R.id.status_bar_collapse, pcloseIntent)

        views!!.setImageViewResource(R.id.status_bar_play,
                android.R.drawable.ic_media_pause)
        bigViews!!.setImageViewResource(R.id.status_bar_play,
                android.R.drawable.ic_media_pause)

        val metaRetriver: MediaMetadataRetriever
        metaRetriver = MediaMetadataRetriever()
        try {
            metaRetriver.setDataSource(file)
            val art = metaRetriver.embeddedPicture
            val songImage = BitmapFactory.decodeByteArray(art, 0, art.size)
            bigViews!!.setImageViewBitmap(R.id.status_bar_album_art, songImage)
            views!!.setImageViewBitmap(R.id.status_bar_icon, songImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val db = DatabaseHandler(context)
        views!!.setTextViewText(R.id.status_bar_track_name, db.getcurrentdata(file!!, 1))
        bigViews!!.setTextViewText(R.id.status_bar_track_name, db.getcurrentdata(file!!, 1))

        views!!.setTextViewText(R.id.status_bar_artist_name, db.getcurrentdata(file!!, 2))
        bigViews!!.setTextViewText(R.id.status_bar_artist_name, db.getcurrentdata(file!!, 2))

        bigViews!!.setTextViewText(R.id.status_bar_album_name, db.getcurrentdata(file!!, 3))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mChannel: NotificationChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description="Description"
            mChannel.setSound(null,null)
            status = Notification.Builder(this).setChannelId(CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher).setColor(Color.parseColor("#0a00b6")).build()
            status!!.contentView = views
            status!!.bigContentView = bigViews
            status!!.flags = Notification.FLAG_ONGOING_EVENT
            status!!.contentIntent = pendingIntent
            mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager!!.createNotificationChannel(mChannel)
            mNotificationManager!!.notify(notifyID , status)
        }else{
            status = Notification.Builder(this).build()
            status!!.contentView = views
            status!!.bigContentView = bigViews
            status!!.flags = Notification.FLAG_ONGOING_EVENT
            status!!.icon = R.mipmap.ic_launcher
            status!!.contentIntent = pendingIntent
            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, status)
        }

    }

    override fun onPrepared(mp: MediaPlayer) {
        if(current_position!=null)
            mp.seekTo(current_position!!)
        mp.start()
        PLAYING_STATE=true
        current_position=0
        // Création et lancement du Thread de mise à jour de l'UI
        val updateThread = Thread(sendUpdates)
        updateThread.start()
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val threedenabled = mypref.getBoolean("isthreedenabled",false)
        if(threedenabled) {
            if (PLAYING_STATE) {
                var left: Float = 0.9f
                var right: Float = 0.1f
                handlerone(left, right)
            }
        }
    }
    fun handlerone(l:Float,r:Float){
        val delay: Long =100
        val handler = Handler()
        var left=l
        var right=r
        handler.postDelayed(object : Runnable {
            override fun run() {
                if(left<=0.9f && left>0.1f) {
                    if(mediaPlayer!=null) {
                        mediaPlayer!!.setVolume(left, right)
                        left = left - 0.1f
                        right = right + 0.1f
                        handler.postDelayed(this, delay)
                    }
                }else{
                    handlertwo(0.1f,0.9f)
                    handler.removeCallbacks(this)
                }
            }
        }, delay)
    }
    fun handlertwo(l:Float,r:Float){
        val handler = Handler()
        val delay: Long =100
        var left=l
        var right=r
        handler.postDelayed(object : Runnable {
            override fun run() {
                if(right<=0.9f && right>0.1f) {
                    if(mediaPlayer!=null) {
                        mediaPlayer!!.setVolume(left, right)
                        left = left + 0.1f
                        right = right - 0.1f
                        handler.postDelayed(this, delay)
                    }
                }else{
                    handlerone(0.9f,0.1f)
                    handler.removeCallbacks(this)
                }
            }
        }, delay)
    }
    fun pause() {
        if (mediaPlayer != null)
            mediaPlayer!!.pause()
    }

    fun play() {
        if (mediaPlayer != null)
            mediaPlayer!!.start()
    }

    fun stop() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    fun seekTo(msec: Int) {
        mediaPlayer!!.seekTo(msec)
    }

    override fun onCompletion(mp: MediaPlayer) {
        // Utilisation du BroadcastReceiver local pour indiquer à l'activité que la lecture est terminée
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val old_position = mypref.getInt("position", 0)
        val prefsEditr = mypref.edit()
        PLAYING_STATE=false
        if(old_position<DatabaseHandler(context).getcurrentcount()-1) {
            prefsEditr.putInt("position", old_position + 1)
            prefsEditr.apply()
            onCreate()
            showNotification()
        }else{
            val intent = Intent(MPS_COMPLETED)
            broadcastManager!!.sendBroadcast(intent)
        }
    }

    private fun sendElapsedTime() {
        // Utilisation du BroadcastReceiver local pour envoyer la durée passée
        val intent = Intent(MPS_RESULT)
        try {
            if (mediaPlayer != null)
                intent.putExtra(MPS_MESSAGE, mediaPlayer!!.currentPosition)
            broadcastManager!!.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        val MPS_MESSAGE = "com.arunapps.sharemusix.PlayMusic.MESSAGE"
        val MPS_RESULT = "com.arunapps.sharemusix.PlayMusic.RESULT"
        val MPS_COMPLETED = "com.arunapps.sharemusix.PlayMusic.COMPLETED"
        val ACTION_TEXT = PlayMusic::class.java.name + "seekbar"
        var runnable: Runnable? = null
        var PLAYING_STATE: Boolean = false
    }

}