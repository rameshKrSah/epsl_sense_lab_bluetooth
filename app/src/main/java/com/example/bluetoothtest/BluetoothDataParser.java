package com.example.bluetoothtest;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.Semaphore;

/**
 * This class handles the received data on Bluetooth. The tasks of this class are
 *      1. Parse the received data to for requests and data
 *      2. Send the response back the Bluetooth client (Camera)
 *      3. Concatenate the image file (since it is received in chunks)
 *      4. Save the processed image file in local memory.
 */
public class BluetoothDataParser {
    private static final String TAG = "BluetoothDataParser";
    private static final String HANDLER_THREAD_NAME = "DATA_PARSER_THREAD";
    private static final boolean D = true;
    private static final byte BT_REQUEST = 0x00;
    private static final byte BT_DATA = 0x0A;

    private static final int _imageBufferSize = 500 * 1024;     // 500 kb buffer for image
    private static byte[] _imageBuffer = new byte[_imageBufferSize];
    private static int _expectedImageSize = 0;

    private static final int _commandBufferSize = 500;
    private static byte[] _commandBuffer = new byte[_commandBufferSize];
    private static int _commandLength = 0;
    private static byte[] _responseBuffer = new byte[_commandBufferSize];
    private static int _responseLength = 0;

    private static BluetoothController myBtController;
    private dataParserThread myDataParserThread;

    /**
     * Constructor class for the Bluetooth data parser.
     * @param BluetoothController class instance
     */
    public BluetoothDataParser(BluetoothController controller) {
        myBtController = controller;

        // create the handler thread
        myDataParserThread = new dataParserThread(HANDLER_THREAD_NAME);
        myDataParserThread.start();
        myDataParserThread.prepareHandler();
    }

    /**
     * Copy the data in the buffer (received on Bluetooth) and post a new Runnable
     * to parser the data.
     * @param buffer byte
     */
    public void dataParser(byte [] buffer) {
        if (buffer.length > _commandBufferSize || buffer.length == 0) {
            Log.d(TAG, "dataParser: rcvd cmd buf size larger or zero");
            return;
        }

//        System.arraycopy(buffer, 0, _commandBuffer, 0, buffer.length);
//        _commandLength = buffer.length;

        // post runnable to parse the data
        myDataParserThread.postTask(new _parseData(buffer));
    }


    /**
     * Data parser runnable. This task is posted each time command is received on Bluetooth.
     *
     */
    static class _parseData implements Runnable{
        private byte[] cmdBuffer;
        private int cmdLength;

        public _parseData(byte [] buffer) {
            // copy the command
            cmdBuffer = new byte[buffer.length];
            cmdLength = buffer.length;
            System.arraycopy(buffer, 0, cmdBuffer, 0, buffer.length);
        }


        @Override
        public void run() {
            Log.d(TAG, "_parserData run: rcvd cmd "
                    + new String(cmdBuffer, 0, cmdLength) + " len " + cmdLength);

            // now we start parsing the data
            byte header = cmdBuffer[0];
            int payloadLength = (cmdBuffer[1] | cmdBuffer[2] >> 8);
            int packetNumber = (cmdBuffer[3] | cmdBuffer[4] >> 8);

            if(header == BT_REQUEST) {
                // we have received request over Bluetooth
                Log.d(TAG, "_parserData run: bt request");

            } else if (header == BT_DATA){
                Log.d(TAG, "_parserData run: bt data");
            }

            Log.d(TAG, "_parserData run: payload length " + payloadLength
                    + " packet number " + packetNumber);
        }
    }

    /*
    private final Semaphore _dataReveivedSemaphore = new Semaphore(-1, true);
    private static processThread myProcessThread = null;
    public void startProcessThread(){
        // stop any thread that is running
        if(myProcessThread != null){
            myProcessThread.close();
            myProcessThread = null;
        }

        myProcessThread = new processThread();
        myProcessThread.start();
    }
    public void copyReceivedData(byte [] buffer, int length){

    }

    private void _parseCommand(){

    }

    private class processThread extends Thread {
        private boolean toClose = false;

        public processThread(){
            if(D)
                Log.d(TAG, "processThread: starting thread");
        }

        public void run() {
            // this is where we process the received data
            while(!toClose){
                try {
                    // wait for the data receive semaphore
                    _dataReveivedSemaphore.acquire();

                    // semaphore received, so data must be available
                    _parseCommand();

                    // send response

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void close(){
            toClose = true;
        }
    }
     */



    /**
     * Data parser handler thread. Post the data task to the handler thread queue.
     */
    private class dataParserThread extends HandlerThread{
        // handler for this thread, used to post task to message queue
        private Handler dataParserHandler;

        /**
         * Constructor, which takes the name of the HandlerThread and calls the super class.
         * @param name
         */
        public dataParserThread(String name) {
            super(name);
        }

        /**
         * Post a runnable task to the handler thread.
         * @param task
         */
        public void postTask(Runnable task) {
            dataParserHandler.post(task);
        }

        /**
         * Get the handler for the handler thread. Make sure to create the thread for the
         * handler thread before calling this function.
         */
        public void prepareHandler(){
            dataParserHandler = new Handler(getLooper());
        }
    }
}




/**
 * AsyncTask have a single thread dedicated to them, so we spin off more that one AsyncTask,
 * then they aren't truly asynchronous.
 *
 * Service are executed on the main thread by default. We need to create a separate thread if
 * we want the service to be asynchronous.
 */
