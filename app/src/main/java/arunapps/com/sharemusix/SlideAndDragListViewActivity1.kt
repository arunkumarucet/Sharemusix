package arunapps.com.sharemusix

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.yydcdut.sdlv.Menu
import com.yydcdut.sdlv.MenuItem
import com.yydcdut.sdlv.SlideAndDragListView

/**
 * Created by yuyidong on 16/1/23.
 */
class SlideAndDragListViewActivity1 : AppCompatActivity(), AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener, AbsListView.OnScrollListener, SlideAndDragListView.OnDragDropListener, SlideAndDragListView.OnSlideListener, SlideAndDragListView.OnMenuItemClickListener, SlideAndDragListView.OnItemDeleteListener, SlideAndDragListView.OnItemScrollBackListener {

    private var mMenu: Menu? = null
    private var mList: ArrayList<String>? = null
    private var mArtist: ArrayList<String>? = null
    private var mListView: SlideAndDragListView? = null
    private var mDraggedEntity: String? = null
    private var mDraggedEntity1: String? = null

    private val mAdapter = object : BaseAdapter() {

        override fun getCount(): Int {
            return mList!!.size
        }

        override fun getItem(position: Int): String {
            return mList!![position]
        }

        override fun getItemId(position: Int): Long {
            return mList!![position].hashCode().toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val cvh: CustomViewHolder
            if (convertView == null) {
                cvh = CustomViewHolder()
                convertView = LayoutInflater.from(this@SlideAndDragListViewActivity1).inflate(R.layout.item_custom_btn, null)
                cvh.txtName = convertView!!.findViewById<View>(R.id.name) as TextView
                cvh.txtArtist = convertView.findViewById<View>(R.id.artist) as TextView
                cvh.image = convertView.findViewById<View>(R.id.imageView) as ImageView
                convertView.tag = cvh
            } else {
                cvh = convertView.tag as CustomViewHolder
            }
            cvh.txtName!!.text = this.getItem(position)
            cvh.txtArtist!!.text = mArtist!![position]
            val mypref = PreferenceManager.getDefaultSharedPreferences(this@SlideAndDragListViewActivity1)
            val pos = mypref.getInt("position", 0)
            if(pos==position) {
                cvh.image!!.visibility = View.VISIBLE
            }
            else {
                cvh.image!!.visibility = View.INVISIBLE
            }
            return convertView
        }

        internal inner class CustomViewHolder {
            var txtName: TextView? = null
            var txtArtist: TextView? = null
            var image: ImageView? = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slide_and_drag_list_view1)
        initData()
        initMenu()
        initUiAndListener()
    }

    fun initData() {
        mList = DatabaseHandler(applicationContext).getcurrentlist(1)
        mArtist = DatabaseHandler(applicationContext).getcurrentlist(2)
    }

    fun initMenu() {
        mMenu = Menu(true)
        mMenu!!.addItem(MenuItem.Builder().setWidth(resources.getDimension(R.dimen.slv_item_bg_btn_width).toInt() * 2)
                .setIcon(resources.getDrawable(R.drawable.ic_delete_dark))
                .build())
        mMenu!!.addItem(MenuItem.Builder().setWidth(resources.getDimension(R.dimen.slv_item_bg_btn_width_img).toInt())
                .setDirection(MenuItem.DIRECTION_RIGHT)
                .setIcon(resources.getDrawable(R.drawable.ic_delete_dark))
                .build())
    }

    fun initUiAndListener() {
        mListView = findViewById<View>(R.id.lv) as SlideAndDragListView
        mListView!!.setMenu(mMenu!!)
        mListView!!.adapter = mAdapter
        mListView!!.setOnScrollListener(this)
        mListView!!.setOnDragDropListener(this)
        mListView!!.onItemClickListener = this
        mListView!!.setOnSlideListener(this)
        mListView!!.setOnMenuItemClickListener(this)
        mListView!!.setOnItemDeleteListener(this)
        mListView!!.onItemLongClickListener = this
        mListView!!.setOnItemScrollBackListener(this)
    }

    override fun onDragViewStart(beginPosition: Int) {
        mDraggedEntity = mList!![beginPosition]
        mDraggedEntity1 = mArtist!![beginPosition]
        frompos = beginPosition
    }

    override fun onDragDropViewMoved(fromPosition: Int, toPosition: Int) {
        val Info = mList!!.removeAt(fromPosition)
        mList!!.add(toPosition, Info)
        mArtist!!.add(toPosition,mArtist!!.removeAt(fromPosition))
    }

    override fun onDragViewDown(finalPosition: Int) {
        mDraggedEntity=mList!![finalPosition]
        mDraggedEntity1=mArtist!![finalPosition]
        mList!!.set(finalPosition, mDraggedEntity!!)
        mArtist!!.set(finalPosition,mDraggedEntity1!!)
        if(frompos<finalPosition){
            DatabaseHandler(applicationContext).updatecurrentdrag(frompos,9999)
            for (i in frompos + 1..finalPosition)
            {
                DatabaseHandler(applicationContext).updatecurrentdrag(i,i-1)
            }
            DatabaseHandler(applicationContext).updatecurrentdrag(9999,finalPosition)
        }
        if(frompos>finalPosition){
            DatabaseHandler(applicationContext).updatecurrentdrag(frompos,9999)
            for (i in frompos - 1 downTo finalPosition)
            {
                DatabaseHandler(applicationContext).updatecurrentdrag(i,i+1)
            }
            DatabaseHandler(applicationContext).updatecurrentdrag(9999,finalPosition)
        }
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val old_position = mypref.getInt("position", 0)
        val prefsEditr = mypref.edit()
        if(old_position== frompos) {
            prefsEditr.putInt("position", finalPosition)
            prefsEditr.apply()
        }
    }

    override fun onSlideOpen(view: View, parentView: View, position: Int, direction: Int) {
        mList!!.removeAt(position - mListView!!.headerViewsCount)
        mArtist!!.removeAt(position - mListView!!.headerViewsCount)
        deletesingledb(position)
        mAdapter.notifyDataSetChanged()
    }
    fun deletesingledb(position: Int){
        DatabaseHandler(applicationContext).deletecurrentsingle(position)
        DatabaseHandler(applicationContext).updatecurrentdelete(position)
        val mypref = PreferenceManager.getDefaultSharedPreferences(this)
        val old_position = mypref.getInt("position", 0)
        val prefsEditr = mypref.edit()
        if(old_position>0) {
            prefsEditr.putInt("position", old_position - 1)
            prefsEditr.apply()
        }
    }
    override fun onSlideClose(view: View, parentView: View, position: Int, direction: Int) {
    }

    override fun onMenuItemClick(v: View, itemPosition: Int, buttonPosition: Int, direction: Int): Int {
        when (direction) {
            MenuItem.DIRECTION_LEFT -> when (buttonPosition) {
                0 -> return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP
            }
            MenuItem.DIRECTION_RIGHT -> when (buttonPosition) {
                0 -> return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP
            }
        }
        return Menu.ITEM_NOTHING
    }

    override fun onItemDeleteAnimationFinished(view: View, position: Int) {
        mList!!.removeAt(position - mListView!!.headerViewsCount)
        mArtist!!.removeAt(position - mListView!!.headerViewsCount)
        deletesingledb(position)
        mAdapter.notifyDataSetChanged()
    }

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
        when (scrollState) {
            AbsListView.OnScrollListener.SCROLL_STATE_IDLE -> {
            }
            AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL -> {
            }
            AbsListView.OnScrollListener.SCROLL_STATE_FLING -> {
            }
        }
    }

    override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {

    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return true
    }

    override fun onItemLongClick(parent: AdapterView<*>, view: View, position: Int, id: Long): Boolean {
        return true
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val mypref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val prefsEditr = mypref.edit()
        prefsEditr.putInt("position",position)
        prefsEditr.apply()
        val serviceIntent = Intent(applicationContext, PlayMusic::class.java)
        serviceIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
            startService(serviceIntent)
        stopService(serviceIntent)
        serviceIntent.action = Constants.ACTION.STARTFOREGROUND_ACTION
        startService(serviceIntent)
    }

    override fun onScrollBackAnimationFinished(view: View, position: Int) {
    }

    companion object {
        private val TAG = SlideAndDragListViewActivity1::class.java.simpleName
        var frompos: Int = 0
        var topos: Int = 0
    }
}