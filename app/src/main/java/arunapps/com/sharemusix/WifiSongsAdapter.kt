package arunapps.com.sharemusix

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.AsyncTask
import android.widget.*
import java.util.ArrayList
import android.view.*
import android.content.DialogInterface
import android.support.v7.app.AlertDialog
import android.widget.ArrayAdapter
import android.content.Intent
import android.net.wifi.p2p.WifiP2pInfo
import android.preference.PreferenceManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.NetworkInterface.getNetworkInterfaces
import java.net.SocketException
import kotlin.experimental.and


class WifiSongsAdapter(private val context: Context, internal var fname: ArrayList<String>,val progressBar: ProgressBar) : BaseAdapter() {

    init {
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getCount(): Int {
        return fname.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var vi = convertView
        if (convertView == null)
            vi = inflater!!.inflate(R.layout.layout_custom_wifi_songlist, parent, false)
        val textView = vi!!.findViewById<View>(R.id.name) as TextView
        textView.text = fname[position]
        vi.setOnClickListener{
            progressBar.visibility=View.VISIBLE
            val serviceIntent = Intent(context, FileTransferService::class.java)
            serviceIntent.action = FileTransferService.ACTION_SEND_NAME
            serviceIntent.putExtra(FileTransferService.EXTRAS_NAME, fname[position])
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988)
            context.startService(serviceIntent)
            //Toast.makeText(context,"Getting data...",Toast.LENGTH_SHORT).show()
            if(DeviceDetailFragment.info.isGroupOwner && DeviceDetailFragment.info.groupFormed){
                //Toast.makeText(context,"ok",Toast.LENGTH_SHORT).show()
                DeviceDetailFragment.SongServerAsyncTask(context).execute()
            }
        }
        return vi
    }

    companion object {
        private var inflater: LayoutInflater? = null
    }

}