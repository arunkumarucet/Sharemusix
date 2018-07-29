package arunapps.com.sharemusix

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.io.File
import java.nio.file.Files.delete



class StartUpPage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up_page)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            DatabaseAsyncTask(applicationContext).execute()
            val dir = File(Environment.getExternalStorageDirectory().toString() + "/" + applicationContext.packageName)
            if (dir.isDirectory) {
                val children = dir.list()
                for (i in children.indices) {
                    File(dir, children[i]).delete()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.CHANGE_NETWORK_STATE, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION), 1)
        }
        Handler().postDelayed(object:Runnable {
            public override fun run() {
                startActivity(Intent(this@StartUpPage, MainActivity::class.java))
                finish()
            }
        }, 3000)
    }
    class DatabaseAsyncTask constructor(val context: Context) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void): String? {
            val cr = context.contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
            val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            val cur = cr.query(uri, null, selection, null, sortOrder)
            val db = DatabaseHandler(context)
            val sonc: Int
            if (cur != null) {
                sonc = cur.count
                if (sonc > 0) {
                    while (cur.moveToNext()) {
                        val album = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                        val artist = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                        val path = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA))
                        val name = path.substring(path.lastIndexOf("/") + 1)
                        var composer: String
                        try {
                            composer = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.COMPOSER))
                        }catch (e:Exception){
                            composer="Unknown Artist"
                            e.printStackTrace()
                        }
                        db.insertlist(name, artist, album, path,composer)
                        db.insertalbum(album,path,composer)
                        db.insertartist(composer,path)
                    }
                }
                cur.close()
            }
            return null
        }
    }
}
