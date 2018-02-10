package com.fbiv.ambientecontrolado;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by Igor on 28/01/18.
 */

public class MyBluetoothService {
    private static final String TAG = "BluetoothService";

    private Context context;
    private Handler activityHandler;
    private BluetoothAdapter bluetoothAdapter;// handler that gets info from Bluetooth service

    private BluetoothSocket connectedSocket;
    private BluetoothDevice connectedDevice;
    private InputStream inStream;
    private OutputStream outStream;
    private byte[] buffer;

//    private ConnectThread connectThread;
//    private ConnectedThread connectedThread;

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    public interface MessageConstants {

        static final int TOAST = 0;

        static final int CONNECTED = 101;
        static final int STREAMS_OPENED = 102;

        static final int REQUEST_PAIR = 1;

        static final int PAIRED = -1;

        static final int NEW_DATA = 2;
    }

    private Handler serviceHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what) {

                // Establishing a connection
                case MessageConstants.CONNECTED:
                    // Bluetooth device connected. Openning streams
                    int why = msg.arg1;
                    openStreams(why);
                    break;
                case MessageConstants.STREAMS_OPENED:
                    // Input stream and output stream ready.

                    // Writing data to connected device
                    switch (msg.arg1) {
                        case MessageConstants.REQUEST_PAIR:
                            Message message = activityHandler.obtainMessage(MessageConstants.PAIRED, connectedDevice);
                            message.sendToTarget();
                            break;
                        case MessageConstants.NEW_DATA:

                            byte[] data = listenToNewData();

                            if (data != null) {

                                if (data[0] == 116) {

                                    int temperaturaReferencia = (data[1] - 48) * 10 + data[2] - 48;
                                    int temperaturaAtual = (data[3] - 48) * 10 + data[4] - 48;

                                    Message dataMessage = activityHandler.obtainMessage(MessageConstants.NEW_DATA, temperaturaReferencia, temperaturaAtual, null);
                                    dataMessage.sendToTarget();

                                }

                            }

                            break;
                    }

                    break;

                case MessageConstants.NEW_DATA:



                    break;
                default:
                    break;

            }

            return true;
        }
    });



    MyBluetoothService(Context context, Handler handler) {
        this.context = context;
        this.activityHandler = handler;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // Public methods
    void disconnect() {

        if (connectedSocket != null) {
            try {
                connectedSocket.close();

            } catch (Exception e) {

                e.printStackTrace();

            }
        }
        connectedSocket = null;
        connectedDevice = null;

    }

    void pair(BluetoothDevice device) {

        disconnect();
        bluetoothAdapter.cancelDiscovery();
        connect(device, MessageConstants.REQUEST_PAIR);

    }

    void listenToNewData(BluetoothDevice device) {

        if (connectedDevice != null) {

            Message message = serviceHandler.obtainMessage(MessageConstants.STREAMS_OPENED, MessageConstants.NEW_DATA, 0, null);
            message.sendToTarget();

        } else {

            connectedDevice = device;
            connect(device, MessageConstants.NEW_DATA);

        }

    }

    // Private methods
    private synchronized void connect(final BluetoothDevice device, final int why) {

        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            connectedSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

            this.connectedDevice = device;

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    // Cancel discovery because it otherwise slows down the connection.
                    bluetoothAdapter.cancelDiscovery();

                    try {
                        // Connect to the remote device through the socket. This call blocks
                        // until it succeeds or throws an exception.
                        connectedSocket.connect();

                    } catch (IOException connectException) {

                        try {
                            Log.e("","trying fallback...");

                            connectedSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                            connectedSocket.connect();

                            Log.e("","Connected");
                        } catch (Exception e) {
                            e.printStackTrace();

                            // Unable to connect; close the socket and return.
                            connectException.printStackTrace();

                            try {
                                connectedSocket.close();
                                disconnect();
                            } catch (IOException closeException) {
                                Log.e("tag", "Could not close the client socket", closeException);
                            }

                            return;

                        }

                    }

                    // The connection attempt succeeded
                    Message message = serviceHandler.obtainMessage(MessageConstants.CONNECTED, why, 0, null);
                    message.sendToTarget();
                }
            });

        } catch (IOException e) {
            Log.e("tag", "connect() method failed", e);

        }

    }

    private void openStreams(final int why) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    inStream = connectedSocket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }

                try {
                    outStream = connectedSocket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                Message message = serviceHandler.obtainMessage(MessageConstants.STREAMS_OPENED, why, 0, null);
                message.sendToTarget();

            }
        });


    }

    @Nullable
    private synchronized byte[] listenToNewData() {

        buffer = new byte[5];
        DataInputStream inputStream = new DataInputStream(inStream);
        try {

            inputStream.readFully(buffer, 0, buffer.length);

            return buffer;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }


}