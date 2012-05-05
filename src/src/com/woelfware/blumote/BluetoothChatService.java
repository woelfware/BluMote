// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    //private static final String NAME = "BlueMote";

    // Unique UUID for this application
    //private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    //private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    static final int buffer_size = 512;  // size of BT buffer
    
    // Constants that indicate the current connection state
    static final int STATE_NONE = 0;       // we're doing nothing
//    static final int STATE_LISTEN = 1;     // now listening for incoming connections
    static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(BluMote.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
//        if (mAcceptThread == null) {
//            mAcceptThread = new AcceptThread();
//            mAcceptThread.start();
//        }
//        setState(STATE_LISTEN);
        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        // Cancel the accept thread because we only want to connect to one device
        //if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(BluMote.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluMote.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        //if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
    	setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluMote.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluMote.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
    	setState(STATE_NONE);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(BluMote.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluMote.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }



    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
            	if (Build.VERSION.SDK_INT >= 10 && !(Build.MANUFACTURER.equals("HTC")) ) {
            		Log.v(TAG, "Using connection method 1");
            		// the newer SDK includes this function call, but it doesn't appear to work on HTC phones
            		// so the tryConnect() tries to use reflection before giving up entirely
            		tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);            		
            	}
            	else if (Build.VERSION.SDK_INT >= 10 && Build.MANUFACTURER.equals("HTC")
            			&& BluMote.getHtcInsecureSetting() == true) {
            		Log.v(TAG, "Using connection method 2");
            		// Even newer HTC phones need to use the reflection technique
            		Method m = device.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});                       			
        			tmp = (BluetoothSocket) m.invoke(device, 1);	        		
            	}
            	else {
            		Log.v(TAG, "Using connection method 3");
            		// older phones will have to use secure which requires a PIN  
            		// Note, HTC Eris works with this call using android 2.1
            		tmp = device.createRfcommSocketToServiceRecord(MY_UUID);  
            	} 
    			
            }
            catch (Exception e) {           
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
            	// This is a blocking call and will only return on a
        		// successful connection or an exception
        		mmSocket.connect();
        		
                // Reset the ConnectThread because we're done
                synchronized (BluetoothChatService.this) {
                    mConnectThread = null;
                }
                
                // Start the connected thread
                connected(mmSocket, mmDevice);
            } catch (Exception e) {	  
            	Log.e(TAG, "original call to connect() failed!", e);

            	// if we got here then the tryConnectSecureReflection failed
            	connectionFailed();
            	// Close the socket
            	try {
            		mmSocket.close();
            	} catch (IOException e2) {
            		Log.e(TAG, "unable to close() socket during connection failure", e2);
            	}        
            	// Start the service over to restart listening mode
            	BluetoothChatService.this.start();
            	return;
            }                       
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }       

    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[buffer_size];
            byte[] circ_buffer = new byte[buffer_size];
            int bytes;
            int index = 0; // for circular buffer index
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    int local_index = index; // working copy of circ buffer index
                    for (int i= 0; i < bytes; i++) {
                    	circ_buffer[local_index] = buffer[i];
                    	local_index = (local_index + 1) % (buffer_size);
                    }
                    //System.arraycopy(buffer, 0, circ_buffer, index, bytes);
                    
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(BluMote.MESSAGE_READ, bytes, index, circ_buffer)
                            .sendToTarget();
                    // increment starting circular buffer index
                    index = (index + bytes) % 255;
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(BluMote.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
    
//  /**
//  * This thread runs while listening for incoming connections. It behaves
//  * like a server-side client. It runs until a connection is accepted
//  * (or until cancelled).
//  */
// private class AcceptThread extends Thread {
//     // The local server socket
//     private final BluetoothServerSocket mmServerSocket;
//
//     public AcceptThread() {
//         BluetoothServerSocket tmp = null;
//
//         // Create a new listening server socket
//         try {
//         	tmp = null;
//             tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
//         } 
//         catch (IOException e) {
//             Log.e(TAG, "listen() failed", e);
//         }
//         mmServerSocket = tmp;
//     }
//
//     public void run() {
//         if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
//         setName("AcceptThread");
//         BluetoothSocket socket = null;
//
//         // Listen to the server socket if we're not connected
//         while (mState != STATE_CONNECTED) {
//             try {
//                 // This is a blocking call and will only return on a
//                 // successful connection or an exception
//                 socket = mmServerSocket.accept();
//             } catch (IOException e) {
//                 Log.e(TAG, "accept() failed", e);
//                 break;
//             }
//
//             // If a connection was accepted
//             if (socket != null) {
//                 synchronized (BluetoothChatService.this) {
//                     switch (mState) {
//                     case STATE_LISTEN:
//                     case STATE_CONNECTING:
//                         // Situation normal. Start the connected thread.
//                         connected(socket, socket.getRemoteDevice());
//                         break;
//                     case STATE_NONE:
//                     case STATE_CONNECTED:
//                         // Either not ready or already connected. Terminate new socket.
//                         try {
//                             socket.close();
//                         } catch (IOException e) {
//                             Log.e(TAG, "Could not close unwanted socket", e);
//                         }
//                         break;
//                     }
//                 }
//             }
//         }
//         if (D) Log.i(TAG, "END mAcceptThread");
//     }
//
//     public void cancel() {
//         if (D) Log.d(TAG, "cancel " + this);
//         try {
//             mmServerSocket.close();
//         } catch (IOException e) {
//             Log.e(TAG, "close() of server failed", e);
//         }
//     }
// }

}
