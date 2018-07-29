package arunapps.com.sharemusix

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

object Constants {
    interface ACTION {
        companion object {
            val MAIN_ACTION = "arunapps.com.sharemusix.action.main"
            val INIT_ACTION = "arunapps.com.sharemusix.action.init"
            val PREV_ACTION = "arunapps.com.sharemusix.action.prev"
            val PLAY_ACTION = "arunapps.com.sharemusix.action.play"
            val NEXT_ACTION = "arunapps.com.sharemusix.action.next"
            val STARTFOREGROUND_ACTION = "arunapps.com.sharemusix.action.startforeground"
            val STOPFOREGROUND_ACTION = "arunapps.com.sharemusix.action.stopforeground"
            val PLAY_WITH_SEEK = "arunapps.com.sharemusix.action.playseek"
        }

    }

    interface NOTIFICATION_ID {
        companion object {
            val FOREGROUND_SERVICE = 101
        }
    }

    fun getDefaultAlbumArt(context: Context): Bitmap? {
        var bm: Bitmap? = null
        val options = BitmapFactory.Options()
        try {
            bm = BitmapFactory.decodeResource(context.resources,
                    R.drawable.ic_launcher_background, options)
        } catch (ee: Error) {
        } catch (e: Exception) {
        }

        return bm
    }

}