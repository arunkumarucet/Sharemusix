package arunapps.com.sharemusix

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.ArrayList

class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_MUSIX = "CREATE TABLE $TABLE_LIST($NAME TEXT,$ARTIST TEXT,$ALBUM TEXT,$PATH TEXT PRIMARY KEY,$COMPOSER TEXT)"
        val CREATE_ALBUM_TABLE= "CREATE TABLE $ALBUMTABLE($ALBUMNAME TEXT PRIMARY KEY,$ALBUMPATH TEXT,$ALBUMCOMPOSER TEXT)"
        val CREATE_ARTIST_TABLE="CREATE TABLE $ARTISTTABLE($ARTISTNAME TEXT PRIMARY KEY,$ARTISTPATH TEXT)"
        val CREATE_PLAYLISTMAIN_TABLE="CREATE TABLE $PLAYMAINTABLE($PLAYNAME TEXT PRIMARY KEY)"
        val CREATE_PLAYLIST_TABLE="CREATE TABLE $PLAYLISTTABLE($PLAYLISTNAME TEXT,$PLAY_NAME TEXT,$PLAY_ARTIST TEXT,$PlAY_ALBUM TEXT,$PLAYLISTPATH TEXT)"
        val CREATE_CURRENT_TABLE="CREATE TABLE $CURRENTTABLE($CURRENTID INTEGER,$CURRENTNAME TEXT,$CURRENTARTIST TEXT,$CURRENTALBUM TEXT,$CURRENTPATH TEXT)"
        db.execSQL(CREATE_MUSIX)
        db.execSQL(CREATE_ALBUM_TABLE)
        db.execSQL(CREATE_ARTIST_TABLE)
        db.execSQL(CREATE_PLAYLISTMAIN_TABLE)
        db.execSQL(CREATE_PLAYLIST_TABLE)
        db.execSQL(CREATE_CURRENT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP + TABLE_LIST)
        db.execSQL(DROP + ALBUMTABLE)
        db.execSQL(DROP + ARTISTTABLE)
        db.execSQL(DROP + PLAYMAINTABLE)
        db.execSQL(DROP + PLAYLISTTABLE)
        db.execSQL(DROP + CURRENTTABLE)
        onCreate(db)
    }

    /**    public Integer getcontactcount() {
     * SQLiteDatabase db = this.getWritableDatabase();
     * String count = "SELECT count(*) FROM " + TABLE_CONTACTS;
     * Cursor mcursor = db.rawQuery(count, null);
     * mcursor.moveToFirst();
     * int icount = mcursor.getInt(0);
     * return icount;
     * }
     */
    fun insertlist(name: String, artist: String, album: String, path: String,composer: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(NAME, name)
        values.put(ARTIST, artist)
        values.put(ALBUM, album)
        values.put(PATH, path)
        values.put(COMPOSER,composer)
        db.insert(TABLE_LIST, null, values)
        db.close()
    }

    fun insertalbum(name:String,path:String,composer:String){
        val db=this.writableDatabase
        val values=ContentValues()
        values.put(ALBUMNAME,name)
        values.put(ALBUMPATH,path)
        values.put(ALBUMCOMPOSER,composer)
        db.insert(ALBUMTABLE,null,values)
        db.close()
    }
    fun insertartist(name:String,path:String){
        val db=this.writableDatabase
        val values=ContentValues()
        values.put(ARTISTNAME,name)
        values.put(ARTISTPATH,path)
        db.insert(ARTISTTABLE,null,values)
        db.close()
    }
    fun insertplaylistmain(name:String){
        val db=this.writableDatabase
        val values=ContentValues()
        values.put(PLAYNAME,name)
        db.insert(PLAYMAINTABLE,null,values)
        db.close()
    }
    fun insertplaylist(playname:String,name:String,artist:String,album:String,path:String){
        val db=this.writableDatabase
        val values=ContentValues()
        values.put(PLAYLISTNAME,playname)
        values.put(PLAY_NAME,name)
        values.put(PLAY_ARTIST,artist)
        values.put(PlAY_ALBUM,album)
        values.put(PLAYLISTPATH,path)
        db.insert(PLAYLISTTABLE,null,values)
        db.close()
    }
    fun insertcurrent_normal(id:Int,name:String,artist:String,album:String,path:String){
        val db=this.writableDatabase
        val values=ContentValues()
        values.put(CURRENTID,id)
        values.put(CURRENTNAME,name)
        values.put(CURRENTARTIST,artist)
        values.put(CURRENTALBUM,album)
        values.put(CURRENTPATH,path)
        db.insert(CURRENTTABLE,null,values)
        db.close()
    }
    fun updatecurrent(id:Int){
        val db=this.writableDatabase
        val updateQuery = "UPDATE $CURRENTTABLE SET $CURRENTID=$CURRENTID+1 WHERE $CURRENTID>$id"
        val c = db.rawQuery(updateQuery,null)
        c.moveToFirst()
        c.close()
        db.close()
    }
    fun deletecurrent(){
        val db=this.writableDatabase
        db.delete(CURRENTTABLE,null,null)
        db.close()
    }
    fun deletecurrentsingle(id:Int){
        val db = this.writableDatabase
        db.delete(CURRENTTABLE, CURRENTID + "=" + id, null)
        db.close()
    }
    fun deletecurrentplaylist(playlist: String,path: String){
        val db = this.writableDatabase
        db.delete(PLAYLISTTABLE, "$PLAYLISTNAME='$playlist' AND $PLAYLISTPATH='$path'" , null)
        db.close()
    }
    fun deletetplaylist(playlist: String){
        val db = this.writableDatabase
        db.delete(PLAYLISTTABLE, "$PLAYLISTNAME='$playlist'" , null)
        db.close()
    }
    fun deletetplaylistmain(playlist: String){
        val db = this.writableDatabase
        db.delete(PLAYMAINTABLE, "$PLAYNAME='$playlist'" , null)
        db.close()
    }
    fun updatecurrentdelete(id:Int){
        val db=this.writableDatabase
        val updateQuery = "UPDATE $CURRENTTABLE SET $CURRENTID=$CURRENTID-1 WHERE $CURRENTID>$id"
        val c = db.rawQuery(updateQuery,null)
        c.moveToFirst()
        c.close()
        db.close()
    }
    fun updatecurrentdrag(id: Int,assign: Int ){
        val db=this.writableDatabase
        val updateQuery = "UPDATE $CURRENTTABLE SET $CURRENTID=$assign WHERE $CURRENTID=$id"
        val c1 = db.rawQuery(updateQuery,null)
        c1.moveToFirst()
        c1.close()
        db.close()
    }
    fun getAlllist(a: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val selectQuery = "SELECT * FROM $TABLE_LIST ORDER BY $NAME COLLATE NOCASE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getAllAlbum(a: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val selectQuery = "SELECT * FROM $ALBUMTABLE ORDER BY $ALBUMNAME COLLATE NOCASE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getAllArtist(a: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val selectQuery = "SELECT * FROM $ARTISTTABLE ORDER BY $ARTISTNAME COLLATE NOCASE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getAlbumList(a: Int,b: String): ArrayList<String> {
        val list = ArrayList<String>()
        val str=b.replace("'","''")
        val selectQuery = "SELECT * FROM $TABLE_LIST WHERE $ALBUM='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getArtistList(a: Int,b: String): ArrayList<String> {
        val list = ArrayList<String>()
        val str=b.replace("'","''")
        val selectQuery = "SELECT * FROM $TABLE_LIST WHERE $COMPOSER='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getPlaylistMain(a: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val selectQuery = "SELECT * FROM $PLAYMAINTABLE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getPlayList(a:Int,b: String): ArrayList<String> {
        val list = ArrayList<String>()
        val str=b.replace("'","''")
        val selectQuery = "SELECT * FROM $PLAYLISTTABLE WHERE $PLAYLISTNAME='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getcurrentlist(a: Int): ArrayList<String> {
        val list = ArrayList<String>()
        val selectQuery = "SELECT * FROM $CURRENTTABLE ORDER BY $CURRENTID COLLATE NOCASE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(a))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
    fun getcurrentcount(): Int{
        val selectQuery = "SELECT * FROM $CURRENTTABLE"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery,null)
        val count = cursor.count
        cursor.close()
        db.close()
        return count
    }
    fun getcurrentdata(id:String,a: Int): String{
        val str=id.replace("'","''")
        val selectQuery = "SELECT * FROM $CURRENTTABLE WHERE $CURRENTPATH='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        var `val` = ""
        if (cursor.moveToFirst()) {
            `val` = cursor.getString(a)
        }
        cursor.close()
        db.close()
        return `val`
    }
    fun getdata(id: String, a: Int): String {
        val str=id.replace("'","''")
        val selectQuery = "SELECT * FROM $TABLE_LIST WHERE $PATH='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        var `val` = ""
        if (cursor.moveToFirst()) {
            `val` = cursor.getString(a)
        }
        cursor.close()
        db.close()
        return `val`
    }
    fun getsongwifi(id: String, a: Int): String {
        val str=id.replace("'","''")
        val selectQuery = "SELECT * FROM $TABLE_LIST WHERE $NAME='$str'"
        val db = this.readableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        var `val` = ""
        if (cursor.moveToFirst()) {
            `val` = cursor.getString(a)
        }
        cursor.close()
        db.close()
        return `val`
    }

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "musixshare"
        //Main Table
        private val TABLE_LIST = "list"
        private val PATH = "path"
        private val ARTIST = "artist"
        private val ALBUM = "album"
        private val NAME = "name"
        private val COMPOSER = "composer"
        private val DROP = "DROP TABLE IF EXISTS "
        //AlbumList Table
        private val ALBUMTABLE="albumtable"
        private val ALBUMNAME="albumname"
        private val ALBUMPATH="albumpath"
        private val ALBUMCOMPOSER = "albumcomposer"
        //ArtistList Table
        private val ARTISTTABLE="artisttable"
        private val ARTISTNAME="artistname"
        private val ARTISTPATH="artistpath"
        //Playlist Main Table
        private val PLAYMAINTABLE="playmaintable"
        private val PLAYNAME="playname"
        //Playlist Table
        private val PLAYLISTTABLE="playlist"
        private val PLAYLISTNAME="playlistname"
        private val PLAYLISTPATH="playlistpath"
        private val PLAY_NAME="play_name"
        private val PLAY_ARTIST="play_artist"
        private val PlAY_ALBUM = "play_album"
        //Current Table
        private val CURRENTTABLE="currenttable"
        private val CURRENTID="id"
        private val CURRENTNAME="currentname"
        private val CURRENTARTIST="currentartist"
        private val CURRENTALBUM="currentalbum"
        private val CURRENTPATH="currentpath"
    }
}