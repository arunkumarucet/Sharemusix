/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package arunapps.com.sharemusix;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import arunapps.com.sharemusix.DeviceListFragment.DeviceActionListener;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements ConnectionInfoListener {

    protected static final int CHOOSE_FILE_RESULT_CODE = 20;
    private View mContentView = null;
    private WifiP2pDevice device;
    public static WifiP2pInfo info;
    ProgressDialog progressDialog = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mContentView = inflater.inflate(R.layout.device_detail, null);

        mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        ((DeviceActionListener) getActivity()).disconnect();
                    }
                });

        mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        // Allow user to pick an image from Gallery or other
                        // registered apps
                        //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        //intent.setType("image/*");
                        //startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                        ArrayList<String> data = new ArrayList<>();
                        DatabaseHandler db = new DatabaseHandler(getActivity().getApplicationContext());
                        data = db.getAlllist(0);
                        String real_data=getDottedDecimalIP(getLocalIPAddress())+"\n";
                        for(int i=0; i<data.size();i++)
                            real_data=real_data+data.get(i)+"\n";
                        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                        serviceIntent.setAction(FileTransferService.ACTION_SEND_DATA);
                        serviceIntent.putExtra(FileTransferService.EXTRAS_DATA,real_data);
                        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                                info.groupOwnerAddress.getHostAddress());
                        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                        getActivity().startService(serviceIntent);

                    }
                });

        return mContentView;
    }
    public void connect_method(WifiP2pDevice device){
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(mContentView.getContext());
        if(pref.getBoolean("isStream",true))
            config.groupOwnerIntent = 0;
        else
            config.groupOwnerIntent = 15;
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel",
                "Connecting to :" + device.deviceAddress, true, true
//                        new DialogInterface.OnCancelListener() {
//
//                            @Override
//                            public void onCancel(DialogInterface dialog) {
//                                ((DeviceActionListener) getActivity()).cancelDisconnect();
//                            }
//                        }
        );
        ((DeviceActionListener) getActivity()).connect(config);
    }
  /**  @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // User has picked an image. Transfer it to group owner i.e peer using
        // FileTransferService.
        Uri uri = data.getData();
        Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
        Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                info.groupOwnerAddress.getHostAddress());
        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
        getActivity().startService(serviceIntent);
    }
**/
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        this.info = info;
        this.getView().setVisibility(View.VISIBLE);

        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            //Toast.makeText(getActivity().getApplicationContext(), info.toString(), Toast.LENGTH_SHORT).show();
            new FileServerAsyncTask(getActivity()).execute();//for receiving all songs=server listenes
        } else if (info.groupFormed) {
            // The other device acts as the client. In this case, we enable the
            // get file button.
            mContentView.findViewById(R.id.btn_start_client).setVisibility(View.VISIBLE);
            //Toast.makeText(getActivity().getApplicationContext(), getDottedDecimalIP(getLocalIPAddress()), Toast.LENGTH_SHORT).show();
            new FileServerAsyncTask1(getActivity()).execute();//for receiving file name-client listens
        }

    }

    /**
     * Updates the UI with device data
     * 
     * @param device the device to be displayed
     */
    public void showDetails(WifiP2pDevice device) {
        this.device = device;
        this.getView().setVisibility(View.VISIBLE);
        TextView view = (TextView) mContentView.findViewById(R.id.device_address);
        view.setText(device.deviceAddress);
        view = (TextView) mContentView.findViewById(R.id.device_info);
        view.setText(device.toString());

    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    public void resetViews() {
        mContentView.findViewById(R.id.btn_start_client).setVisibility(View.GONE);
        this.getView().setVisibility(View.GONE);
    }

    /**
     * A simple server socket that accepts connection and writes some data on
     * the stream.
     */
    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(Void... params) {
/**            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
                Socket client = serverSocket.accept();
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");
                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
**/
            ServerSocket serverSocket = null;
            Socket client = null;
            DataInputStream inputstream = null;
            try {
                serverSocket = new ServerSocket(8988);
                client = serverSocket.accept();
                inputstream = new DataInputStream(client.getInputStream());
                String str = inputstream.readUTF();
                serverSocket.close();
                return str;
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }finally{
                if(inputstream != null){
                    try{
                        inputstream.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
                if(client != null){
                    try{
                        client.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
                if(serverSocket != null){
                    try{
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
/**            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                if(Build.VERSION.SDK_INT>=24){
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName()+".arunapps.com.sharemusix.provider", new File(result)),"image/*");
                }else
                    intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }
**/
            if (result != null) {
                if(info.isGroupOwner && info.groupFormed) {
                    String lines[] = result.split("\\n");
                    SharedPreferences mypref = PreferenceManager.getDefaultSharedPreferences(context);
                    SharedPreferences.Editor prefsEditr = mypref.edit();
                    prefsEditr.putString("clientip", lines[0]);
                    prefsEditr.apply();
                    new SongServerAsyncTask(context).execute();//for receiving song file-server listenes
                    //Toast.makeText(context, lines[0], Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(context, WifiSongList.class);
                    i.putExtra("songlist", result);
                    context.startActivity(i);
                    cancel(true);
                }
            }
        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, e.toString());
            return false;
        }
        return true;
    }
    public static class FileServerAsyncTask1 extends AsyncTask<Void, Void, String> {

        private Context context;

        /**
         * @param context
         */
        public FileServerAsyncTask1(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(Void... params) {
/**            try {
 ServerSocket serverSocket = new ServerSocket(8988);
 Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
 Socket client = serverSocket.accept();
 Log.d(WiFiDirectActivity.TAG, "Server: connection done");
 final File f = new File(Environment.getExternalStorageDirectory() + "/"
 + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
 + ".jpg");

 File dirs = new File(f.getParent());
 if (!dirs.exists())
 dirs.mkdirs();
 f.createNewFile();

 Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());
 InputStream inputstream = client.getInputStream();
 copyFile(inputstream, new FileOutputStream(f));
 serverSocket.close();
 return f.getAbsolutePath();
 } catch (IOException e) {
 Log.e(WiFiDirectActivity.TAG, e.getMessage());
 return null;
 }
 **/
            ServerSocket serverSocket = null;
            Socket client = null;
            DataInputStream inputstream = null;
            try {
                serverSocket = new ServerSocket(8988);
                client = serverSocket.accept();
                inputstream = new DataInputStream(client.getInputStream());
                String str = inputstream.readUTF();
                serverSocket.close();
                return str;
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }finally{
                if(inputstream != null){
                    try{
                        inputstream.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
                if(client != null){
                    try{
                        client.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
                if(serverSocket != null){
                    try{
                        serverSocket.close();
                    } catch (IOException e) {
                        Log.e(WiFiDirectActivity.TAG, e.getMessage());
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
/**            if (result != null) {
 statusText.setText("File copied - " + result);
 Intent intent = new Intent();
 intent.setAction(Intent.ACTION_VIEW);
 if(Build.VERSION.SDK_INT>=24){
 intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
 intent.setDataAndType(FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName()+".arunapps.com.sharemusix.provider", new File(result)),"image/*");
 }else
 intent.setDataAndType(Uri.parse("file://" + result), "image/*");
 context.startActivity(intent);
 }
 **/
            if (result != null) {
                if(!info.isGroupOwner && info.groupFormed) {
                    DatabaseHandler db = new DatabaseHandler(context);
                    Intent serviceIntent = new Intent(context, FileTransferService.class);
                    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                    serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, db.getsongwifi(result, 3));
                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                            info.groupOwnerAddress.getHostAddress());
                    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8988);
                    context.startService(serviceIntent);
                    new FileServerAsyncTask1(context).execute();
                }
            }
        }

    }
    public static class SongServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;

        /**
         * @param context
         */
        public SongServerAsyncTask(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8988);
                Socket client = serverSocket.accept();
                final File f = new File(Environment.getExternalStorageDirectory() + "/" + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis() + ".mp3");
                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                InputStream inputstream = client.getInputStream();
                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            }catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                SharedPreferences mypref = PreferenceManager.getDefaultSharedPreferences(context);
                boolean isListView = mypref.getBoolean("isListView",false);
                if(info.isGroupOwner && info.groupFormed && isListView) {
                    MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
                    try{
                        metaRetriver.setDataSource(result);
                        Intent intent = new Intent(context,WifiCurrentListener.class);
                        intent.putExtra("result",result);
                        context.startActivity(intent);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
/**                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".arunapps.com.sharemusix.provider", new File(result)), "audio/*");
                    } else
                        intent.setDataAndType(Uri.parse("file://" + result), "audio/*");
                    context.startActivity(intent);
 **/
                    new SongServerAsyncTask(context).execute();//for receiving song file-server listenes
                }
            }
        }

    }

    private byte[] getLocalIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
                            return inetAddress.getAddress();
                        }
                        //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
                    }
                }
            }
        } catch (SocketException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        } catch (NullPointerException ex) {
            //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
        }
        return null;
    }

    private String getDottedDecimalIP(byte[] ipAddr) {
        //convert to dotted decimal notation:
        String ipAddrStr = "";
        for (int i=0; i<ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i]&0xFF;
        }
        return ipAddrStr;
    }

}
