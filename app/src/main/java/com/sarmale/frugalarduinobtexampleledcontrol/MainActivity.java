package com.sarmale.frugalarduinobtexampleledcontrol;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
// import android.bluetooth.BluetoothSocket;
// import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
// import android.os.SystemClock;  // For: SystemClock.sleep(5000);
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
// import java.io.IOException;
// import java.io.InputStream;
// import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    
	// Global variables we will use
    private static final String TAG = "LOG TAG";
    private static final int REQUEST_ENABLE_BT = 1;
	
    //We will use a Handler to get the BT Connection status
    public static Handler handler;
    private final static int ERROR_READ = 0;
    // Used in bluetooth handler to identify message update
	
    BluetoothDevice btDevice = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // We declare a default UUID to create the global variable

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    // Should these two be declared here?
    ConnectThread connectThread;  // I added this soon declaration.
	ConnectedThread connectedThread;  // This one already was there.

    @SuppressLint("CheckResult")  // Ignoring result of subscribe method in ConnectButton
    // Actually it can be a potential source of reconnect problems. What should be the result for?
    @RequiresApi(api = Build.VERSION_CODES.M)  // ??
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		// // Instances of BT Manager and BT Adapter needed to work with BT in Android.
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Instances of the Android UI elements that will will use during the execution of the APP
        Button searchDevicesButton = findViewById(R.id.searchDevicesButton);
        TextView btDevices = findViewById(R.id.btDevices);
        Button connectButton = findViewById(R.id.connectButton);
        TextView btConnected = findViewById(R.id.connectedTextView);
        TextView btReadings = findViewById(R.id.btReadingsTextView);
        EditText commandEdit = findViewById(R.id.commandEditText);
        TextView lastCommandLabel = findViewById(R.id.lastCommandLabelTextView);
        TextView lastCommandData = findViewById(R.id.lastCommandDataTextView);
        Button sendCommandButton = findViewById(R.id.sendCommandButton);
        Button clearButton = findViewById(R.id.clearButton);
        Log.d(TAG, "Begin Execution");


        //Using a handler to update the interface in case of an error connecting to the BT device
        //My idea is to show handler vs RxAndroid
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {  // Annotated on behalf of IDE.
                Log.d(TAG, "INFO: handler: handleMessage: msg");
                if (msg.what == ERROR_READ) {
                    Log.d(TAG, "ERROR: handler: msg.what == ERROR_READ");
                    String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                    btReadings.setText(arduinoMsg);
                    // I believe the bellow one is responsible for showing "Unable to connect to BT device."
                    // Still not sure why it happens, nor how to fix it.
                    Toast.makeText(MainActivity.this, arduinoMsg, Toast.LENGTH_LONG).show();
                }
            }
        };

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////// Find all Linked devices ///////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Display all the linked BT Devices
        searchDevicesButton.setOnClickListener(view -> {
            // Check if the phone supports BT.
            if (bluetoothAdapter == null) {
                Log.d(TAG, "Device doesn't support Bluetooth");
            } else {
                Log.d(TAG, "Device support Bluetooth");
                // Check BT enabled.
                if (!bluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "Bluetooth is disabled");
                    // Ask user to enable BT. (--> Do We ??)
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    // This 'if' is here AT THIS MOMENT just to silence the COMPILER.
                    if (ActivityCompat.checkSelfPermission(
                            getApplicationContext(),
                            Manifest.permission.BLUETOOTH_CONNECT)
                            != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                    } else {}
                    /* The previous 'if' had SAME BODY as 'else' block. --> SUPERFLUOUS*/
                    Log.d(TAG, "We \"EITHER DO OR DON'T\" have BT Permissions");
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    Log.d(TAG, "Bluetooth is \"EITHER WAY\" enabled now");
                } else {
                    Log.d(TAG, "Bluetooth is enabled");
                }

                String btDevicesString = "";
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    // Get the name and address of each paired device.
                    for (BluetoothDevice device: pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress(); // MAC address
                        Log.d(TAG, "deviceName:" + deviceName);
                        Log.d(TAG, "deviceHardwareAddress:" + deviceHardwareAddress);
                        // We append all devices to a String that we will display in the UI
                        btDevicesString = btDevicesString + deviceName +
                                        " || " + deviceHardwareAddress + "\n";
                        // If we find the HC 05 device (the Arduino BT module)
                        // We assign the device value to the Global variable BluetoothDevice
                        // We enable the button "Connect to HC 05 device"
                        if (deviceName.equals("HC-05")) {
                            Log.d(TAG, "HC-05 found");
                            arduinoUUID = device.getUuids()[0].getUuid();
                            btDevice = device;
                            // HC -05 Found, enabling the button to read results
                            connectButton.setEnabled(true);
                        }
                        btDevices.setText(btDevicesString);
                    }
                }
            }
            Log.d(TAG, "Button Pressed");
        });

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////// Call the observable to connect to the HC-05 ////////////////////////////////////////////
////////////////////////////////////////////// If it connects, the button to configure the LED will be enabled  ///////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // Create an Observable from RxAndroid
        // The code will be executed when an Observer subscribes to the the Observable
        final Observable<Exchange> exchangeObservable = Observable.create(emitter -> {

            Log.d(TAG, "Calling ConnectThread class");
            Log.d(TAG, "Arg - btDevice: >"+btDevice+"< end.");
            // ConnectThread connectThread;  // I've decided to declare it in the file header.
            connectThread = new ConnectThread(btDevice, arduinoUUID, handler);
            connectThread.run();  // MUST BE RUN !!

            // Check if Socket connected
            if (connectThread.getMmSocket().isConnected()) {

                /* THIS ONE CAUSES TROUBLE */
//                connectButton.setEnabled(false);
//                btConnected.setText(getString(R.string.connectedTextViewStringTrue));
                /* THIS ONE CAUSES TROUBLE */

                Log.d(TAG, "Calling ConnectedThread class");
                connectedThread = new ConnectedThread(connectThread.getMmSocket());
                connectedThread.run();  // MUST BE RUN !!

                if(connectedThread.getMmInStream() != null) {

                    Log.d(TAG, "Calling Exchange class");
                    Exchange exchange = new Exchange();
                    exchange.setConnected(true);

                    String receivedValueRead = connectedThread.getValueRead();

                    if (receivedValueRead != null) {

                        // If we have read a value from the Arduino
                        // we call the onNext() method of emitter
                        // This value will be observed by the observer

                        Log.d(TAG, "Setting Message");
                        Log.d(TAG, receivedValueRead);
                        exchange.setMessage(receivedValueRead);
                        Log.d(TAG, "emitter.onNext");
                        emitter.onNext(exchange);

                        // What is the purpose of this?
                        // MyApplication.setupConnectedThread();

                    } else {
                        Log.d(TAG, "getValueRead() returned null message, continuing..");
                    }
                }
                /* THIS ONE ALSO CAUSES TROUBLE */ /*
                else {
                    connectButton.setEnabled(true);
                    btConnected.setText(getString(R.string.connectedTextViewDefaultString));
                } */
                /* THIS ONE ALSO CAUSES TROUBLE */

//                connectedThread.cancel();
                /* Should be in some onExit method. */
            }

            /* I think one of those cancels disables me to read more data */


            // SystemClock.sleep(5000); // Why would this be needed?
            // Close the socket connection
//            connectThread.cancel();
            // We could Override the    onComplete function
            /* Should be in some onExit method. */

            emitter.onComplete();  // Does it even have a purpose to call it, when NOT OVERRIDDEN?

            /* SOMETIMES THE RECONNECT WORKS, SOMETIMES STILL IT DOES NOT */
            /* Calling the 'cancel()' method on both Threads doesn't seem to solve it. */
            /* Maybe its not enough, as it might only cancel threads but not bt comm / socket. */
        });

/////////////////////////////////////////////////////////////////////
///////////////////////   END OF OBSERVABLE   ///////////////////////
/////////////////////////////////////////////////////////////////////

        connectButton.setOnClickListener(view -> {
            Log.d(TAG, "INFO: connectButton.setOnClickListener(v->{");
            btReadings.setText("");
            if (btDevice != null) {
                // We subscribe to the observable until the onComplete() is called
                // We also define control the thread management with
                // subscribeOn:  the thread in which you want to execute the action
                // observeOn: the thread in which you want to get the response
                exchangeObservable.
                        observeOn(AndroidSchedulers.mainThread()).
                        subscribeOn(Schedulers.io()).
                        subscribe(exchangeObservable_p -> {

                            if(exchangeObservable_p.isConnected()){
//                                configureLEDButton.setEnabled(true);
                                sendCommandButton.setEnabled(true);
                                btConnected.setText(getString(R.string.connectedTextViewStringTrue));
                                connectButton.setEnabled(false);
                            } else {
                                sendCommandButton.setEnabled(false);
                                btConnected.setText(getString(R.string.connectedTextViewDefaultString));
                                connectButton.setEnabled(true);
                            }


                            String observedMessage = exchangeObservable_p.getMessage();
//                            exchangeObservable_p.setMessage("");

                            if (observedMessage != null) {
                                Log.d(TAG, "Got Message");
                                Log.d(TAG, observedMessage);
//                                btReadings.setText(observedMessage);
                                String messageLog = btReadings.getText().toString();
                                String newMessage = "";
                                int maxChars = 200;
                                if (messageLog.length() >= maxChars) {
                                    messageLog = "";
                                    String errorMessage = "messageLog exceeded "+maxChars+" characters. CLEAR.";
                                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                                newMessage = messageLog + "\n" + observedMessage;
                                btReadings.setText(newMessage);
                            } else {
                                Log.d(TAG, "exchange.getMessage() returned null message, continuing..");
                            }

                            // valueRead returned by the onNext() from the Observable
//                            btReadings.setText(valueRead);  // not in ExampleLEDControl
                            // We just scratched the surface with RxAndroid
                        });

            }
        });


//         Next activity to configure the RGB LED
//        configureLEDButton.setOnClickListener(view -> {
//            Log.d(TAG, "INFO: MyApplication.getApplication().setupConnectedThread(connectedThread)");
//            MyApplication.getApplication().setupConnectedThread(connectedThread);
//            Intent intent = new Intent(MainActivity.this, ConfigureLed.class);
//            startActivity(intent);
//        });

        sendCommandButton.setOnClickListener(view -> {
            Log.d(TAG, "INFO: SendStringButton pressed.");
            String givenCommand = commandEdit.getText().toString();
            Log.d(TAG, givenCommand);
//            Toast.makeText(MainActivity.this, givenCommand, Toast.LENGTH_LONG).show();
            lastCommandData.setText(givenCommand);
            
            if (connectedThread != null) {
                Log.d(TAG, "INFO: Sending Command Given.");
                connectedThread.write(givenCommand);
                Log.d(TAG, "INFO: Given Command Sent. (Not acknowledged by the target device.)");
                String toastMessage = "Given Command >"+givenCommand+"< Sent";
                Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();
            } else {
                String errorMessage = "Unable to send command - BT not connected!";
                Log.d(TAG, "INFO:"+" "+errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });


        // Set a listener event on a button to clear the texts
        clearButton.setOnClickListener(view -> {
            btDevices.setText("");
            btReadings.setText("");
            commandEdit.setText("");
            lastCommandData.setText(R.string.lastCommandDataClearedString);
        });
    }
}