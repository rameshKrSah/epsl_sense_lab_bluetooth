package com.example.bluetoothtest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
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
    private static Context myContext;

    private static final int _PREAMBLE_LENGTH = 6;
    private static final int _imageBufferSize = 1024 * 1024;     // 1 mb buffer for image
    private static byte[] _imageBuffer = new byte[_imageBufferSize];
    private static int _currentImageBufferPosition = 0;
    private volatile boolean _image_flag = false;
    private static int _currentImagePacketNumber = 0;

//    private static final int _commandBufferSize = 500;
//    private static byte[] _commandBuffer = new byte[_commandBufferSize];
//    private static int _commandLength = 0;
//    private static byte[] _responseBuffer = new byte[_commandBufferSize];
//    private static int _responseLength = 0;

    private static BluetoothController myBtController;
    private dataParserThread myDataParserThread;


    enum BLUETOOTH_COMM_TYPE {
        BT_REQUEST((byte)0x0A),
        BT_DATA((byte)0x0B),
        BT_RESPONSE((byte)0x0C);

        private final byte value;
        BLUETOOTH_COMM_TYPE(byte ip) {
            this.value = ip;
        }

        public byte getValue() {
            return this.value;
        }
    }

    enum BLUETOOTH_REQUEST_TYPE {
        TIME_REQUEST((byte)0x00),
        IMAGE_INCOMING_REQUEST((byte)0x01),
        ARE_YOU_READY_REQUEST((byte)0x02),
        IMAGE_SENT_REQUEST((byte)0x03);

        private final byte value;
        BLUETOOTH_REQUEST_TYPE(byte ip) {
            this.value = ip;
        }

        public byte getValue(){
            return this.value;
        }
    }

    enum BLUETOOTH_DATA_TYPE {
        IMAGE_DATA((byte)0x00),
        OTHER_DATA((byte)0x01);

        private final byte value;
        BLUETOOTH_DATA_TYPE(byte b) {
            this.value = b;
        }

        public byte getValue() {
            return this.value;
        }
    }

    enum BLUETOOTH_RESPONSE_TYPE {
        RESPONSE_FOR_TIME_REQUEST((byte)0x00),
        RESPONSE_FOR_IMAGE_INCOMING_REQUEST((byte)0x01),
        RESPONSE_FOR_ARE_YOU_READY_REQUEST((byte)0x02),
        RESPONSE_FOR_IMAGE_SENT_REQUEST((byte)0x03),
        RESPONSE_FOR_IMAGE_DATA((byte)0x04),
        RESPONSE_FOR_OTHER_DATA((byte)0x05);

        private byte value;
        BLUETOOTH_RESPONSE_TYPE(byte b) {
            this.value = b;
        }

        public byte getValue(){
            return this.value;
        }
    }

    private static final String BT_TIME_REQUEST = "time please";
    private static final String BT_SENDING_IMAGE_REQUEST = "image incoming";
    private static final String BT_R_U_READY_REQUEST = "are you ready";
    private static final String BT_IMAGE_TX_COMPLETED_REQUEST = "image sent";


    private static final String I_AM_READY_RESPONSE = "i am ready";
    private static final String OK_RESPONSE = "ok";
    private static final String WAIT_RESPONSE = "wait";
    private static final String IMAGE_RECEIVED_RESPONSE = "image received";
    private static final String TIME_RESPONSE = "time:";
    private static final String INVALID_PACKET_NUMBER_RESPONSE = "invalid packet number";

    /**
     * Constructor class for the Bluetooth data parser.
     * @param BluetoothController class instance
     */
    public BluetoothDataParser(BluetoothController controller, Context context) {
        myBtController = controller;
        myContext = context;

        // create the handler thread
        myDataParserThread = new dataParserThread(HANDLER_THREAD_NAME);
        myDataParserThread.start();
        myDataParserThread.prepareHandler();
    }

    /**
     * Stop the handler thread looper.
     * This function must be called from the onDestroy method of the application
     * or somewhere else.
     */
    void stopHandlerThread() {
        if (myDataParserThread != null) {
            myDataParserThread.quit();
        }
    }

    /**
     * Copy the data in the buffer (received on Bluetooth) and post a new Runnable
     * to parser the data.
     * @param buffer byte
     */
    public void dataParser(byte [] buffer) {
        if (buffer.length == 0) {
            Log.d(TAG, "dataParser: rcvd buffer size zero");
            return;
        }

        // post runnable to parse the data
        myDataParserThread.postTask(new _parseData(buffer));
    }

    /**
     * Runnable class that sends response to the Bluetooth device.
     */
    private class _sendResponse implements Runnable {
        private byte _responseBuffer[];
        private BluetoothController btCnt;

        public _sendResponse(byte[] responseBuffer, BluetoothController btController) {
            _responseBuffer = new byte[responseBuffer.length];
            System.arraycopy(responseBuffer, 0, _responseBuffer, 0,
                    responseBuffer.length);
            btCnt = btController;
        }

        @Override
        public void run() {
            btCnt.sendData(_responseBuffer);
        }
    }

    /**
     * Get int value for the given byte value
     * @param byte
     * @return int
     */
    int getIntFromByte(byte b) {
//        return b < 0 ? b + 256 : b;
        return ((int) b) & 0xff;
    }

    /**
     * Get long for the given byte value
     * @param byte
     * @return long
     */
    long getLongFromInt(byte b) {
       return ((long) b) & 0xffL;
    }

    /**
     * Data parser runnable. This task is posted each time command is
     * received on Bluetooth.
     *
     */
    private class _parseData implements Runnable {
        private byte[] cmdBuffer;
        private int cmdLength;

        public _parseData(byte[] buffer) {
            // copy the command
            cmdBuffer = new byte[buffer.length];
            cmdLength = buffer.length;
            System.arraycopy(buffer, 0, cmdBuffer, 0, buffer.length);
        }

        @Override
        public void run() {
            Log.d(TAG, "_parserData run: len: " + cmdLength);

            if (cmdLength < _PREAMBLE_LENGTH) {
                return;
            }

            // now we start parsing the data
            byte header = cmdBuffer[0];
            byte category = cmdBuffer[1];

            // in JAVA bytes are signed and we are sending unsigned bytes from camera. This can be
            // a problem. Even after casting bytes into long we can only go up to 250, any values
            // beyond that does not work and is capped out at 250. The problem is evident when
            // sending image, where the payload length is > 250 and maybe this will also effect the
            // image data. But that is yet to be verified.
//            long payloadLength = getLongFromInt(cmdBuffer[2]) | getLongFromInt(cmdBuffer[3]) >> 8;
            int packetNumber = getIntFromByte(cmdBuffer[4]) | getIntFromByte(cmdBuffer[5]) >> 8;

            // to fix the problem with the payload length, we will extract that information directly
            // from the fact that we know how the data packet is organized.
            int payloadLength = cmdLength - _PREAMBLE_LENGTH;

            if (header == BLUETOOTH_COMM_TYPE.BT_REQUEST.getValue()) {
                // we have received request over Bluetooth
                Log.d(TAG, "_parserData run: bt request");
                _handleBTRequest(category, new String(cmdBuffer, _PREAMBLE_LENGTH, payloadLength));

            } else if (header == BLUETOOTH_COMM_TYPE.BT_DATA.getValue()) {
                Log.d(TAG, "_parserData run: bt data");
                _handleBTData(category, cmdBuffer, packetNumber);

            } else if (header == BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue()) {
                Log.d(TAG, "_parserData run: bt response");

            }

            Log.d(TAG, "_parserData run: payload length " + payloadLength
                    + " packet number " + packetNumber);
        }
    }

    /**
     * Process the data received over Bluetooth and send response if needed.
     * @param dataCategory
     * @param data
     * @param packetNumber
     */
    public void _handleBTData(byte dataCategory, byte [] data, int packetNumber) {
        if (dataCategory == BLUETOOTH_DATA_TYPE.IMAGE_DATA.getValue()) {
            Log.d(TAG, "_handleBTData: image date, pkt number "+packetNumber);
            if(data.length > 0 && _image_flag) {
                // extract the payload length
                int len = data.length - _PREAMBLE_LENGTH;

                // check the packet number. We are expecting a packet that is one greater than
                // current one.
//                if (_currentImagePacketNumber + 1 == packetNumber) {
                    // copy the image data into the buffer
                    System.arraycopy(data, _PREAMBLE_LENGTH, _imageBuffer,
                            _currentImageBufferPosition,
                            len);

                    // change the image buffer properties
                    _currentImageBufferPosition += len;
                    _currentImagePacketNumber = packetNumber;

                    // send the response
                    myDataParserThread.postTask(new _sendResponse(
                            _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
                                    BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_IMAGE_DATA.getValue(),
                                    String.valueOf(len).getBytes()),
                            myBtController));
//                } else {
//                    // invalid packet number response
//                    myDataParserThread.postTask(new _sendResponse(
//                            _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
//                                    BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_IMAGE_DATA.getValue(),
//                                    INVALID_PACKET_NUMBER_RESPONSE.getBytes()),
//                            myBtController));
//                }
            }
        }
    }

    /**
     * Process the Bluetooth request and send response if needed.
     * @param requestCategory
     * @param requestPayload
     */
    public void _handleBTRequest(byte requestCategory, String requestPayload) {
        Log.d(TAG, "_handleBTRequest: payload:  "+ requestPayload);

        if (requestCategory == BLUETOOTH_REQUEST_TYPE.TIME_REQUEST.getValue()) {
            Log.d(TAG, "_handleBTRequest: time request");

            // send the millis from Epoch time as response
            myDataParserThread.postTask(new _sendResponse(
                    _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
                            BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_TIME_REQUEST.getValue(),
                            _getCurrentTimeResponse()),
                    myBtController));

        } else if (requestCategory == BLUETOOTH_REQUEST_TYPE.IMAGE_INCOMING_REQUEST.getValue()) {
            Log.d(TAG, "_handleBTRequest: incoming image request");

            // prepare to receive the image data
            _image_flag = true;
            _currentImageBufferPosition = 0;
            _currentImagePacketNumber = 0;

            // send the response
            myDataParserThread.postTask(new _sendResponse(
                    _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
                            BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_IMAGE_INCOMING_REQUEST.getValue(),
                            OK_RESPONSE.getBytes()),
                    myBtController));

        } else if (requestCategory == BLUETOOTH_REQUEST_TYPE.ARE_YOU_READY_REQUEST.getValue()) {
            Log.d(TAG, "_handleBTRequest: are you ready request");

            // send the response
            myDataParserThread.postTask(new _sendResponse(
                    _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
                            BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_ARE_YOU_READY_REQUEST.getValue(),
                            I_AM_READY_RESPONSE.getBytes()),
                    myBtController));

        } else if (requestCategory == BLUETOOTH_REQUEST_TYPE.IMAGE_SENT_REQUEST.getValue()) {
            Log.d(TAG, "_handleBTRequest: image sent request, file name " + requestPayload);

            // send the response
            myDataParserThread.postTask(new _sendResponse(
                    _prepareResponse(BLUETOOTH_COMM_TYPE.BT_RESPONSE.getValue(),
                            BLUETOOTH_RESPONSE_TYPE.RESPONSE_FOR_IMAGE_SENT_REQUEST.getValue(),
                            IMAGE_RECEIVED_RESPONSE.getBytes()),
                    myBtController));

            // reset the image flag.
            _image_flag = false;

            // save the image to local storage
            new SavePhotoTask().execute(_imageBuffer, requestPayload);

            // reset the image buffer position.
            _currentImageBufferPosition = 0;
            _currentImagePacketNumber = 0;
        }
    }

    /**
     * Prepare the response for Bluetooth requests.
     * @param Bluetooth communication type: Request, Data, or Response
     * @param category Category of the communication
     * @param payload
     * @return byte []
     */
    private byte [] _prepareResponse(byte commType, byte category, byte [] payload) {
        int temp = payload.length;
        byte[] returnArr = new byte[temp + _PREAMBLE_LENGTH + 1];

        // set the communication type: Request, Data, or Response
        returnArr[0] = commType;

        // set the category of the communication type
        returnArr[1] = category;

        // set the length
        returnArr[3] = (byte) (temp >> 8);
        returnArr[2] = (byte) (temp);

        // set the packet number
        returnArr[5] = (byte) (1 >> 8);
        returnArr[4] = (byte) (1);

        // now copy the payload.
        System.arraycopy(payload, 0, returnArr, _PREAMBLE_LENGTH, payload.length);
        returnArr[payload.length + _PREAMBLE_LENGTH] = '\0';    // this is added for string
        return returnArr;
    }

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

    /**
     * Reverse a byte array in-place.
     * @param array
     */
    public static void reverse(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    /**
     * Prepare the response for BT_TIME_REQUEST.
     * @return byte []
     */
    private byte [] _getCurrentTimeResponse() {
        long timeStamp = Utils.getCurrentTime();
        byte[] bytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(timeStamp).array();
        Log.d(TAG, "_getCurrentTimeResponse: " + String.valueOf(timeStamp));
        reverse(bytes);
        return bytes;
    }


    /**
     * AsyncTask to save the photo in the memory
     */
    class SavePhotoTask extends AsyncTask<Object, String, String> {
        @Override
        protected String doInBackground(Object... objects) {
            File photo = new File(myContext.getExternalFilesDir("Pictures"), (String)objects[1]);
            if (photo.exists()) {
                Log.d(TAG, "SavePhotoTask: file exists, deleting...");
                photo.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                fos.write((byte [])objects[0]);
                fos.close();
            }
            catch (java.io.IOException e) {
                Log.e("SavePhotoTask", "Exception in photoCallback", e);
            }

            return(null);
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
