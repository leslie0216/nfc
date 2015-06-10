package ca.usask.chl848.nfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity implements NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
    private String m_userName;
    private String m_userId;

    NfcAdapter m_NfcAdapter;
    private static final int MESSAGE_SENT = 1;
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private MainView m_mainView;

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (m_messageList.size() == 0) {
                m_mainView.sendPhoneInfo();
            }
            m_mainView.invalidate();

            sendMessage();
            timerHandler.postDelayed(this, 500);
        }
    };

    /** BT begin
     */
    private ClientThread m_clientThread = null;
    private ConnectedThread m_connectedThread = null;

    private BluetoothAdapter m_bluetoothAdapter = null;
    private BluetoothDevice m_device = null;

    private BluetoothSocket m_socket = null;

    private UUID m_UUID = UUID.fromString("8bb345b0-712a-400a-8f47-6a4bda472638");

    private InputStream m_inStream;
    private OutputStream m_outStream;

    private static int REQUEST_ENABLE_BLUETOOTH = 1;

    private ArrayList m_messageList = new ArrayList();

    private boolean isConnected;
    /** BT end
     */

    /**
     * experiment begin
     */
    private Button m_startBtn;
    private Button m_continueBtn;
    /**
     * experiment end
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();
        m_userName = bundle.getString("user");
        m_userId = bundle.getString("id");

        setTitle(m_userId + " : " + m_userName + " - Condition : " + getResources().getString(R.string.app_name));

        m_NfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (m_NfcAdapter == null) {
            showToast("NFC is not available on this device.");
        } else {
            m_NfcAdapter.setOnNdefPushCompleteCallback(this, this);
            m_NfcAdapter.setNdefPushMessageCallback(this, this);
        }

        //handleIntent(getIntent());

        /**
         * BT begin
         */
        setupBluetooth();
        /**
         * BT end
         */
        m_startBtn = new Button(this);
        m_startBtn.setText("Start");
        m_startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_mainView != null && m_mainView.getBallCount() == 0) {
                    if (!m_mainView.isFinished()) {
                        m_mainView.startBlock();
                    } else {
                        finish();
                        System.exit(0);
                    }
                }
            }
        });

        RelativeLayout relativeLayout = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        relativeLayout.addView(m_startBtn, layoutParams);

        setStartButtonEnabled(false);

        m_mainView = new MainView(this);

        this.addContentView(m_mainView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        this.addContentView(relativeLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        /**
         * experiment begin
         */
        m_continueBtn = new Button(this);
        m_continueBtn.setText("Continue");
        m_continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (m_mainView != null && m_mainView.getBallCount() == 0) {
                    if (!m_mainView.isFinished()) {
                        m_mainView.nextBlock();
                    } else {
                        showDoneButton();
                    }
                }
            }
        });

        RelativeLayout relativeLayout_con = new RelativeLayout(this);

        RelativeLayout.LayoutParams layoutParams_con = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams_con.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        layoutParams_con.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        relativeLayout_con.addView(m_continueBtn, layoutParams_con);

        setContinueButtonEnabled(false);

        this.addContentView(relativeLayout_con, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        /**
         * experiment end
         */

        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If NFC is not available, we won't be needing this menu
        if (m_NfcAdapter == null) {
            return super.onCreateOptionsMenu(menu);
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(Settings.ACTION_NFCSHARING_SETTINGS);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = m_mainView.encodeMessage();

        NdefRecord record=new NdefRecord(NdefRecord.TNF_WELL_KNOWN,NdefRecord.RTD_TEXT,new byte[0],text.getBytes());

        return new NdefMessage(record);
    }

    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        m_msgCompleteHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }

    /** This handler receives a message from onNdefPushComplete */
    private final Handler m_msgCompleteHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SENT:
                    Toast.makeText(getApplicationContext(), "Ball sent!", Toast.LENGTH_SHORT).show();
                    m_mainView.removeBalls();
                    m_mainView.endTrail();
                    m_mainView.invalidate();
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        setupThread();
        super.onResume();
        setupForegroundDispatch(this, m_NfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, m_NfcAdapter);

        super.onPause();
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@linkBaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            //String textEncoding = ((payload[0] & 128) == 0) ? String.valueOf("UTF-8") : String.valueOf("UTF-16");

            // Get the Language Code
            //int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            //return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            return new String(payload);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                m_mainView.logAndSendReceiveMessageToServer(result);

               //m_mainView.receivedBall(ballId, ballColor);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("received ball");
                        m_mainView.invalidate();
                    }
                });
            }
        }
    }

    /**
     * experiment begin
     */
    public void setStartButtonEnabled(boolean enabled) {
        m_startBtn.setEnabled(enabled);
    }

    public void setContinueButtonEnabled(boolean enabled) {
        m_continueBtn.setEnabled(enabled);
    }

    public void showDoneButton() {
        setContinueButtonEnabled(false);
        m_startBtn.setText("Done");
        m_startBtn.setEnabled(true);
    }
    /**
     * experiment end
     */

    /**
     * BT begin
     */
    @Override
    protected void onRestart() {
        setupThread();
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        stopThreads();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            //return false;
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void exit() {
        new AlertDialog.Builder(MainActivity.this).setTitle("Warning").setMessage("Do you want to exit?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_mainView.closeLogger();
                finish();
                System.exit(0);
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        }).show();
    }

    private void stopThreads() {
        if(m_bluetoothAdapter!=null&&m_bluetoothAdapter.isDiscovering()){
            m_bluetoothAdapter.cancelDiscovery();
        }

        if (m_clientThread != null) {
            m_clientThread.cancel();
            m_clientThread = null;
        }
        if (m_connectedThread != null) {
            m_connectedThread.cancel();
            m_connectedThread = null;
        }
    }

    public void showMessageOnMainView(String msg) {
        if(m_mainView != null)
            m_mainView.setMessage(msg);
    }

    public void sendPhoneInfo() {
        if (m_mainView != null) {
            m_mainView.sendPhoneInfo();
        }
    }

    private void setupBluetooth(){
        showMessageOnMainView("Not Connected");
        isConnected = false;

        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(m_bluetoothAdapter != null){  //Device support Bluetooth
            if(!m_bluetoothAdapter.isEnabled()){
                Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
            }
            else {
                setupThread();
            }
        }
        else{   //Device does not support Bluetooth

            Toast.makeText(this,"Bluetooth not supported on device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                setupThread();
            }
            else {
                showToast("Bluetooth is not enable on your device");
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void setupThread(){
        findDevices();
        if (m_clientThread == null) {
            m_clientThread = new ClientThread();
            m_clientThread.start();
        }
    }

    public void findDevices() {
        Set<BluetoothDevice> pairedDevices = m_bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                if (device.getName().contains("btserver"))
                {
                    m_device = device;
                    break;
                }
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }

    public String getUserName() {
        return m_userName;
    }

    public String getUserId() {
        return m_userId;
    }

    private class ConnectedThread extends Thread {
        public ConnectedThread() {
            try {
                m_inStream = m_socket.getInputStream();
                m_outStream = m_socket.getOutputStream();
                showMessageOnMainView("Connected");
                isConnected = true;
                sendPhoneInfo();
            } catch (IOException e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    // Read from the InputStream
                    if( m_inStream != null && (bytes = m_inStream.read(buffer)) > 0 )
                    {
                        byte[] buf_data = new byte[bytes];
                        for(int i=0; i<bytes; i++)
                        {
                            buf_data[i] = buffer[i];
                        }
                        String msg = new String(buf_data);
                        receiveBTMessage(msg);
                    }
                } catch (IOException e) {
                    cancel();
                    showMessageOnMainView("Not Connected");
                    isConnected = false;
                    m_mainView.clearRemotePhoneInfo();
                    break;
                }
            }
        }

        public void write(String msg) {
            try {
                if (m_outStream != null) {
                    m_outStream.write(msg.getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.flush();
                    m_outStream.close();
                    m_outStream = null;
                }
                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ClientThread extends Thread {
        public ClientThread() {
            initSocket();
        }

        private void initSocket() {
            try {
                if (m_device != null) {
                    m_socket = m_device.createInsecureRfcommSocketToServiceRecord(m_UUID);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                if (m_socket != null) {
                    m_bluetoothAdapter.cancelDiscovery();

                    try {
                        showMessageOnMainView("Connecting...");
                        m_socket.connect();
                    } catch (IOException e) {
                        try {
                            m_socket.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                        initSocket();
                        continue;
                    }

                    //Do work to manage the connection (in a separate thread)
                    m_connectedThread = new ConnectedThread();
                    m_connectedThread.start();
                    break;
                } else {
                    initSocket();
                }
            }
        }

        public void cancel() {
            try {
                if (m_inStream != null) {
                    m_inStream.close();
                    m_inStream = null;
                }
                if (m_outStream != null) {
                    m_outStream.flush();
                    m_outStream.close();
                    m_outStream = null;
                }

                if (m_socket != null) {
                    m_socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendBTMessage(String msg) {
        if (m_connectedThread != null) {
            m_connectedThread.write(msg);
        }
    }

    public void addMessage(String msg) {
        m_messageList.add(msg);
    }

    public void sendMessage(){
        if (m_messageList.size() != 0) {
            String msg = (String)m_messageList.get(0);
            m_messageList.remove(0);
            sendBTMessage(msg);
        }
    }

    private void receiveBTMessage(String msg){
        try {
            JSONArray jsonArray = new JSONArray(msg);

            int len = jsonArray.length();

            ArrayList<String> names = new ArrayList<>();

            for (int i=0; i<len; ++i) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                final String senderName = jsonObject.getString("name");
                int senderColor = jsonObject.getInt("color");
                float senderX = (float) jsonObject.getDouble("x");
                float senderY = (float) jsonObject.getDouble("y");
                float senderZ = (float) jsonObject.getDouble("z");

                if (m_mainView != null) {
                    m_mainView.updateRemotePhone(senderName, senderColor);
                }

                boolean isSendingBall = jsonObject.getBoolean("isSendingBall");
                if (isSendingBall && m_mainView != null) {
                    String receiverName = jsonObject.getString("receiverName");
                    if (receiverName.equalsIgnoreCase(m_userName)) {
                        String ballId = jsonObject.getString("ballId");
                        int ballColor = jsonObject.getInt("ballColor");
                        m_mainView.receivedBall(ballId, ballColor);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("received ball from : " + senderName);
                            }
                        });
                    }
                }

                names.add(senderName);
            }

            ArrayList<MainView.RemotePhoneInfo> remotePhoneInfos = m_mainView.getRemotePhones();
            ArrayList<MainView.RemotePhoneInfo> lostPhoneInfos = new ArrayList<>();
            for (MainView.RemotePhoneInfo phoneInfo : remotePhoneInfos) {
                if (!names.contains(phoneInfo.m_name)) {
                    lostPhoneInfos.add(phoneInfo);
                }
            }

            if (!lostPhoneInfos.isEmpty()) {
                m_mainView.removePhones(lostPhoneInfos);
            }
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected(){
        return isConnected;
    }

    /**
     * BT end
     */
}
