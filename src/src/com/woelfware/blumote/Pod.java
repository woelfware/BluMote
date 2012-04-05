// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

/**
 * Pod hardware codes and state information for blumote pod operation.
 * Encapsulates all operations to/from the pod.
 * 
 * @author keusej
 * 
 */
class Pod {
	static String FW_LOCATION;

	static String ORIGINAL_FW_LOCATION;

	// private constructor to make this class non-instantiable
	private Pod() {
	}

	static BluMote blumote; // reference to blumote instance

	// current State of the pod bluetooth communication
	static BT_STATES BT_STATE = BT_STATES.IDLE;

	// used to lock displaying dialogs for get_version cmd
	private static boolean lockDialog = false;

	// if the button has been pushed down recently, this prevents another button
	// press which could overflow the
	// pod with too much button data
	private static boolean buttonLock = false;

	// these are all in ms (milli-seconds)
	private static int LOCK_RELEASE_TIME = 5000; // timeout to release IR
													// transmit lock if pod
													// doesn't send us an ACK

	// number of times that pod should repeat when button held down
	private static final byte REPEAT_IR_LONG = (byte) 150;

	// the firmware log data we downloaded when requesting a firmware update
	// proccess
	static String[] firmwareRevisions = null;

	// holds the current revision code extracted from pod FW
	private static byte[] currentSwRev = null;

	// note to self, byte is signed datatype
	static String new_name;
	static byte[] pod_data;
	static byte sync_data; // data used explicitly for syncing during BSL
							// process
	static byte[] cal_data = null;

	// interrupt vector based BSL flash unlock password
	private static byte[] passwd = { (byte) 0xFF, (byte) 0xFF, // 0xFFE0
			(byte) 0xFF, (byte) 0xFF, // 0xFFE2
			(byte) 0xFF, (byte) 0xFF, // 0xFFE4
			(byte) 0xFF, (byte) 0xFF, // 0xFFE6
			(byte) 0xFF, (byte) 0xFF, // 0xFFE8
			(byte) 0xFF, (byte) 0xFF, // 0xFFEA
			(byte) 0xFF, (byte) 0xFF, // 0xFFEC
			(byte) 0xFF, (byte) 0xFF, // 0xFFEE
			(byte) 0xFF, (byte) 0xFF, // 0xFFF0
			(byte) 0xFF, (byte) 0xFF, // 0xFFF2
			(byte) 0xFF, (byte) 0xFF, // 0xFFF4
			(byte) 0xFF, (byte) 0xFF, // 0xFFF6
			(byte) 0xFF, (byte) 0xFF, // 0xFFF8
			(byte) 0xFF, (byte) 0xFF, // 0xFFFA
			(byte) 0xFF, (byte) 0xFF, // 0xFFFC
			(byte) 0xFF, (byte) 0xFF // 0xFFFE
	};

	// status return codes
	static final int ERROR = -1;

	static int MIN_GAP_TIME = 20000; // us
	static final int US_PER_SYS_TICK = 4; // needs to match pod FW, micro-secs
											// per system clock tick
	static final int HDR_PULSE_TOL = 25; // 1/25 = +/-4%
	static final int HDR_SPACE_TOL = 25; // 1/25 = +/-4%
	static final int GAP_TOL = 10; // 1/10 = +/- 10%
	// define offsets to important pieces of data in bytestream
	static final int LENGTH_OFFSET = 0; // length of data sent from pod
										// (minus length/ack/reserved)
	static final int MODFREQ_OFFSET = 1; // modulation frequency
	static final int RESERVED = 2; // reserved byte
	static final int DATA_SIZE = 2; // # bytes of each data element
	static final int HP_OFFSET = RESERVED + 1; // header space
	static final int HS_OFFSET = HP_OFFSET + DATA_SIZE; // header pulse
	static final int FPULSE_OFFSET = HS_OFFSET + DATA_SIZE; // First pulse after
															// header
	static final int FSPACE_OFFSET = FPULSE_OFFSET + DATA_SIZE; // First space
																// after header

	// keeps track of bytes accumulated in LEARN_MODE
	// private static byte[] learn_data;
	static int data_index = 0;
	static int total_bytes = 0; // number of bytes to be received from pod
	static int offset = 0; // keeps track of offset in pod_data being retrieved

	enum LEARN_STATE {
		IDLE, CARRIER_FREQ, PKT_LENGTH, RESERVED, COLLECTING
	}

	static LEARN_STATE learn_state = LEARN_STATE.IDLE;

	enum INFO_STATE {
		IDLE, BYTE0, BYTE1, BYTE2, BYTE3
	}

	static INFO_STATE info_state = INFO_STATE.IDLE;

	class Codes {
		public static final byte IDLE = (byte) 0xFE; // Default state - nothing
														// going on
		public static final byte RENAME_DEVICE = 0x01; // Unused pod command
		public static final byte LEARN = 0x01; // Pod Command
		public static final byte GET_VERSION = 0x00; // Pod command
		public static final byte IR_TRANSMIT = 0x02; // Pod command
		public static final byte ABORT_LEARN = (byte) 0xFD; // currently matt
															// does not use this
		public static final byte DEBUG = (byte) 0xFF; // testing purpose only
		public static final byte ACK = (byte) 0x06;
		public static final byte NACK = (byte) 0x15;
		public static final byte ABORT_TRANSMIT = (byte) 0x03; // stop repeating
																// IR command
		public static final byte RESET_RN42 = 0x04; // resets the rn42 module
													// from msp430
		public static final byte READ_ADDRESS = 0x05; // read a segment of flash
														// on micro
	}

	class BSL_CODES {
		public static final String CMD_MODE = "$$$";
		public static final byte SYNC = (byte) 0x80;
		public static final byte DATA_ACK = (byte) 0x90;
		public static final byte DATA_NAK = (byte) 0xA0;
	}

	public static final Integer INHIBIT_RESET = 1;
	public static final Integer ENABLE_RESET = 0;

	// this is to keep track of state machines for example
	// for receiving data from bluetooth interface, how that
	// data should be interpreted
	enum BT_STATES {
		IDLE, // not doing anything
		LEARN, // in button learn mode
		GET_VERSION, // getting pod information
		IR_TRANSMIT, // transmit an ir code
		ABORT_LEARN, // aborting the learn mode
		DEBUG, // debug mode for testing
		ABORT_TRANSMIT, BSL, // bootstrap loader
		SYNC, // Syncing the BSL to the Pod
		READ_ADDRESS, // reading address of mem from pod
	}

	static int debug_send = 0;

	// component ID is defined in blumote spec
	static final HashMap<Integer, String> componentMap = new HashMap<Integer, String>();
	static {
		componentMap.put(0, "Hardware");
		componentMap.put(1, "Firmware");
		componentMap.put(2, "Software");
	}

	/**
	 * Sends a command to the pod, waits for response, and then returns the
	 * received byte[]. Allows for a variable number of return bytes.
	 * 
	 * @param message
	 *            The packet of data to send to the Pod
	 * @param returnLength
	 *            the length of the return data expected
	 * @param state
	 *            the BT_STATES encoding for the state machine
	 * @return
	 * @throws BslException
	 *             Often thrown due to a time-out
	 */
	static byte[] sendPodCommand(byte[] message, int returnLength,
			BT_STATES state) throws BslException {
		data_index = 0;
		total_bytes = returnLength;
		offset = 0;
		pod_data = new byte[returnLength - 1]; // exclude response code
		blumote.sendMessage(message);
		receiveResponse(state);
		return pod_data;
	}

	static void getCalibrationData() {
		try {
			// get 10 bytes of data starting at 0x10F6
			cal_data = getMemorySpace(
					Util.bytesToInt((byte) 0x10, (byte) 0xF6), 10);
			applyCalibration();
		} catch (Exception e) {
			cal_data = null;
		}
	}

	// apply the calibration data to the newly downloaded firmware to be
	// flashed
	static void applyCalibration() {
		// parse the hex file and dump the cal_data information to it
		FileInputStream f = null;
		try {
			f = new FileInputStream(new File(FW_LOCATION));
			BufferedReader br = new BufferedReader(new InputStreamReader(f));
			br = new BufferedReader(new InputStreamReader(f));

			final String header = ":"; // 10 bytes
			final String byteCount = "0A"; // 10 bytes
			final String recordType = "00";
			final String address = "10F6"; // # chars

			String line = byteCount + address + recordType
					+ Util.byteArrayToString(cal_data);
			line = header
					+ line
					+ Util.oneHexByteToString(hexChkSum(Util
							.hexStringToByteArray(line)));

			// count # of lines in the file
			int count = 0;
			while (br.readLine() != null)
				count++;
			f.close();

			// insert the new cal_data to the file on second to last line
			Util.FileUtils.insertLine(new File(FW_LOCATION), line, count);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (f != null)
					f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Read a segment of flash on the microcontroller
	 * 
	 * @param startAdress
	 *            for example 0x10F6 (MSB first)
	 * @param bytes
	 *            for example 5 words = 10 bytes
	 * @return
	 */
	static byte[] getMemorySpace(int startAddress, int bytes)
			throws BslException {
		byte[] message;
		byte[] address;

		address = new byte[] { (byte) (startAddress >> 8), (byte) startAddress };

		message = new byte[] { Pod.Codes.READ_ADDRESS, address[0], address[1],
				(byte) bytes };

		return sendPodCommand(message, bytes + 1, BT_STATES.READ_ADDRESS);
	}

	static void setBluMoteRef(BluMote ref) {
		blumote = ref;
	}

	private static int popInt() {
		try {
			int upperByte = 0x00FF & (byte) pod_data[offset++];
			int lowerByte = 0x00FF & (byte) pod_data[offset++];
			return (upperByte << 8) | lowerByte;
		} catch (ArrayIndexOutOfBoundsException e) {
			return 0;
		}
	}

	private static int getLastOffset() {
		// last offset is length as computed by pod + offset of first real data
		// element
		int length = 0x00FF & (byte) pod_data[LENGTH_OFFSET];
		return (length + HP_OFFSET);
	}

	/**
	 * Way of analyzing the raw code from the pod to find the unique packet
	 * 
	 * @return
	 */
	private static int matchHeaders() {
		offset = HP_OFFSET; // start of actual data

		int headerPulse = popInt();
		int headerSpace = popInt();
		int headerPulseMax = headerPulse + headerPulse / HDR_PULSE_TOL;
		int headerPulseMin = headerPulse - headerSpace / HDR_PULSE_TOL;
		int headerSpaceMax = headerSpace + headerSpace / HDR_SPACE_TOL;
		int headerSpaceMin = headerSpace - headerSpace / HDR_SPACE_TOL;

		int workingData;
		int endOffset = getLastOffset();

		offset = FPULSE_OFFSET; // start of first pulse after header
		workingData = popInt();
		// find the start of the next pkt
		while (offset <= endOffset) {
			if (headerPulseMin <= workingData && workingData <= headerPulseMax) {
				workingData = popInt(); // get space
				if (headerSpaceMin <= workingData
						&& workingData <= headerSpaceMax) {
					// found the start of the next packet, return offset right
					// before header
					return (offset - 2 * DATA_SIZE);
				}
			}

			popInt(); // skip over the spaces, start by checking pulses only
			workingData = popInt(); // grab the pulse
		}

		// if we failed to find it , return ERROR
		return ERROR;
	}

	/**
	 * Way of analyzing the raw code from the pod to find the unique packet
	 * 
	 * @return
	 */
	private static int searchGapSize(int minGapTime) {
		int workingData;
		int endOffset = getLastOffset();

		// try finding the gap using fixed size estimate
		offset = FSPACE_OFFSET; // go to the first space data : HP HS P S
		while (offset <= endOffset) {
			workingData = popInt();
			if (workingData >= minGapTime) {
				return offset;
			}
			popInt(); // skip over the pulses
		}

		// if we fail at this then return an error
		return ERROR;
	}

	private static int largeSpace() {
		int minGapTime = MIN_GAP_TIME;
		return searchGapSize(minGapTime);
	}

	/**
	 * Way of analyzing the raw code from the pod to find the unique packet
	 * 
	 * @return
	 */
	private static int threeLargestSpaces() {
		int workingData;
		int endOffset = getLastOffset();
		int minGapTime = MIN_GAP_TIME;

		// try finding the gap by sorting the 3 largest gaps in the data
		offset = FSPACE_OFFSET; // go to first space data : HP HS P S
		int[] threeLargest = { 0, 0, 0 }; // need to save three largest values
		while (offset <= endOffset) {
			workingData = popInt();
			if (workingData > threeLargest[0]) {
				if (workingData > threeLargest[1]) {
					if (workingData > threeLargest[2]) {
						threeLargest[2] = workingData;
					} else {
						threeLargest[1] = workingData;
					}
				} else {
					threeLargest[0] = workingData;
				}
			}
			popInt(); // skip over the pulses
		}
		// now compare the 3 largest values and see if they
		// are relatively close (if so then this must be the gap)
		boolean closeEnough1 = false;
		boolean closeEnough2 = false;
		boolean closeEnough3 = false;
		if (threeLargest[0] > (threeLargest[1] - threeLargest[1] / GAP_TOL)) {
			closeEnough1 = true;
		} else if (threeLargest[0] > (threeLargest[1] + threeLargest[1]
				/ GAP_TOL)) {
			closeEnough1 = true;
		}
		if (threeLargest[1] > (threeLargest[2] - threeLargest[2] / GAP_TOL)) {
			closeEnough2 = true;
		} else if (threeLargest[0] > (threeLargest[1] + threeLargest[1]
				/ GAP_TOL)) {
			closeEnough2 = true;
		}
		if (threeLargest[0] > (threeLargest[2] - threeLargest[2] / GAP_TOL)) {
			closeEnough3 = true;
		} else if (threeLargest[0] > (threeLargest[2] + threeLargest[2]
				/ GAP_TOL)) {
			closeEnough3 = true;
		}

		if (closeEnough1 && closeEnough2 && closeEnough3) {
			// if they are all close enough together....then use this new
			// derated value as the gap
			Arrays.sort(threeLargest);
			minGapTime = threeLargest[0] - threeLargest[0] / GAP_TOL;
		}

		return searchGapSize(minGapTime);
	}

	/**
	 * 
	 * @param startingOffset
	 */
	static int findEndOfPkt() {

		int lastIndex;
		lastIndex = matchHeaders();
		if (lastIndex != ERROR) {
			return lastIndex;
		}
		lastIndex = largeSpace();
		if (lastIndex != ERROR) {
			return lastIndex;
		}
		lastIndex = threeLargestSpaces();
		if (lastIndex != ERROR) {
			return lastIndex;
		}

		// if all methods fail, then return error
		return ERROR;
	}

	/**
	 * Analyzes the data from the pod to determine the packet to store to the
	 * DB.
	 * 
	 * @param startingAddr
	 */
	static void processRawData() {

		int endingOffset = findEndOfPkt();

		if (endingOffset != ERROR) {
			// need to set the size field to be the endingOffset - informational
			// bytes
			// endOffset points to the data element after the last....
			pod_data[0] = (byte) (endingOffset - HP_OFFSET);
			// now need to trim the pod_data to be the exact size of the data
			byte[] temp = new byte[endingOffset];
			System.arraycopy(pod_data, 0, temp, 0, endingOffset);
			pod_data = temp;
			// after data is processed, store it to the database
			blumote.storeButton();
		} else {
			Toast.makeText(blumote, "Data was not good, please retry",
					Toast.LENGTH_SHORT).show();
		}
	}

	static void setFirmwareRevision(String[] rev) {
		firmwareRevisions = rev;
	}

	/**
	 * A state machine error happened while receiving data over bluetooth
	 * 
	 * @param code
	 *            1 is for errors while in LEARN_MODE and 2 is for errors while
	 *            in GET_INFO mode, affects the usage of Toast
	 */
	private static void signalError(int code) {
		if (code == 1) {
			Toast.makeText(blumote, "Error occured, exiting learn mode!",
					Toast.LENGTH_SHORT).show();
			try {
				blumote.dismissDialog(BluMote.DIALOG_LEARN_WAIT);
			} catch (Exception e) {
				// if dialog had not been shown it throws an error, ignore
			}
			blumote.stopLearning();
			// BT_STATE = BT_STATES.IDLE;
			// learn_state = LEARN_STATE.IDLE;
		} else if (code == 2) {
			BT_STATE = BT_STATES.IDLE;
			info_state = INFO_STATE.IDLE;
		}
	}

	static void clearPodData() {
		pod_data = null;
		currentSwRev = null;
	}

	/**
	 * This function just makes sure data comes back from pod before continuing.
	 * A call to 'clearPodData()' may be necessary to prevent this from falsely
	 * assuming pod data was received. This function will not clear out old pod
	 * data.
	 */
	static byte[] receiveResponse(BT_STATES state) throws BslException {
		BT_STATE = state;

		final BluMote.Wait waiter = new BluMote.Wait(300);

		while (waiter.waitTime < waiter.maxWaitTime) {
			// check if data was received yet from Pod
			if (pod_data != null && (BT_STATE == BT_STATES.IDLE)) {
				break;
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new BslException("Error in sync function");
				}
				waiter.waitTime++;
			}
		}

		// process the data received, if it ever came
		if ((waiter.waitTime >= waiter.maxWaitTime)) {
			setIdleState(); // reset state
			throw new BslException("Exceeded max wait time receiveResponse");
		}

		if (pod_data == null) {
			setIdleState(); // reset state
			throw new BslException(
					"never received any data during receiveResponse");
		}

		return pod_data;
	}

	/**
	 * this function sends the byte[] for a button to the pod
	 * 
	 * @param code
	 *            the IR code data to send
	 */
	static void sendButtonCode(byte[] code) {
		if (!buttonLock && code != null) { // make sure we have not recently
											// sent a button
			buttonLock = true;
			// create a new timer to avoid flooding pod with button data
			new CountDownTimer(LOCK_RELEASE_TIME, LOCK_RELEASE_TIME) {
				public void onTick(long millisUntilFinished) {
					// no need to use this function
				}

				public void onFinish() {
					// called when timer expired
					buttonLock = false; // release lock
				}
			}.start();

			byte command = (byte) (Codes.IR_TRANSMIT);
			byte[] toSend = new byte[code.length + 2]; // 1 extra byte for
														// command byte, 1 for
														// repeat #
			toSend[0] = command;
			// insert different repeat flags based on if this
			// is a long press or a short press of the button
			if (BluMote.BUTTON_LOOPING) {
				toSend[1] = REPEAT_IR_LONG; // long press
			} else {
				toSend[1] = 0; // short press
			}
			for (int j = 2; j < toSend.length; j++) {
				toSend[j] = code[j - 2];
			}
			setIrTransmitState();

			blumote.sendMessage(toSend); // send data if matches
		}
	}

	static boolean isLearnState() {
		return BT_STATE == BT_STATES.LEARN;
	}

	static void setStopLearnState() {
		BT_STATE = BT_STATES.ABORT_LEARN;
	}

	static void setStopTransmitState() {
		BT_STATE = BT_STATES.ABORT_TRANSMIT;
	}

	static void setGetVerState() {
		BT_STATE = BT_STATES.GET_VERSION;
	}

	static void setIrTransmitState() {
		BT_STATE = BT_STATES.IR_TRANSMIT;
	}

	static void setLearnState() {
		BT_STATE = BT_STATES.LEARN;
	}

	static void setIdleState() {
		BT_STATE = BT_STATES.IDLE;
	}

	static void setBslState() {
		BT_STATE = BT_STATES.BSL;
	}

	static void setReadMemState() {
		BT_STATE = BT_STATES.READ_ADDRESS;
	}

	static void lockDialog() {
		lockDialog = true;
	}

	static void unlockDialog() {
		lockDialog = false;
	}

	/**
	 * This method should be called whenever we receive a byte[] from the pod.
	 * 
	 * @param response
	 *            the circular buffer that contains the data that was received
	 *            over BT
	 * @param bytes
	 *            how many bytes were received and stored in response[] on the
	 *            call to this method
	 * @param index
	 *            the starting index into the circular data buffer that should
	 *            be read
	 */
	static void interpretResponse(byte[] response, int bytes, int index) {
		switch (BT_STATE) {
		case LEARN:
			try { // catch any unforseen state machine errors.....
					// learn data may not come all together, so need to process
					// data
					// in chunks
				while (bytes > 0) {
					switch (learn_state) {
					case IDLE:
						if (response[index] == Codes.ACK) {
							learn_state = LEARN_STATE.PKT_LENGTH;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
							bytes--;
							data_index = 0;
						} else {
							signalError(1);
							return;
						}
						break;

					case PKT_LENGTH:
						// if we got here then we are on the second byte of data
						if (Util.isGreaterThanUnsignedByte(response[index], 0)) {
							pod_data = new byte[(0x00FF & response[index])
									+ HP_OFFSET];
							// first three bytes are 'pkt_length carrier_freq
							// reserved'
							pod_data[data_index++] = response[index];
							bytes--;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
							learn_state = LEARN_STATE.CARRIER_FREQ;
						} else {
							signalError(1);
							return;
						}
						break;

					case CARRIER_FREQ:
						// third byte should be carrier frequency
						if (checkPodDataBounds(bytes)) {
							learn_state = LEARN_STATE.RESERVED;
							pod_data[data_index++] = response[index];
							bytes--;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
						} else {
							signalError(1);
							return;
						}
						break;

					case RESERVED:
						// fourth byte should be reserved
						if (checkPodDataBounds(bytes)) {
							learn_state = LEARN_STATE.COLLECTING;
							pod_data[data_index++] = 0; // default to 0
							bytes--;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
						} else {
							signalError(1);
							return;
						}
						break;

					case COLLECTING:
						if (checkPodDataBounds(bytes)) {
							pod_data[data_index++] = response[index];
							// first check to see if this is the last byte
							if (Util.isGreaterThanUnsignedByte(data_index,
									pod_data[0] + 2)) {
								// data index at final position is nth or N+1
								// total items, pod_data[0]
								// is the # of bytes of IR data, so adding 2
								// gives (n+2)th index.
								// After last byte received then data_index
								// points to an index after
								// the last so we exit the collecting routine
								// and store data.
								learn_state = LEARN_STATE.IDLE;
								blumote.dismissDialog(BluMote.DIALOG_LEARN_WAIT);
								processRawData(); // first 3 bytes are not to be
													// analyzed
								return;
							}
							bytes--;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
						} else {
							signalError(1);
							return;
						}
						break;
					} // end switch/case
				} // end while loop
			} catch (Exception e) { // something unexpected occurred....exit
				Toast.makeText(blumote,
						"Communication error, exiting learn mode",
						Toast.LENGTH_SHORT).show();
				setIdleState();
				return;
			}
			break;

		case GET_VERSION:
			try {
				while (bytes > 0) {
					switch (info_state) {
					case IDLE:
						if (response[index] == Codes.ACK) {
							pod_data = new byte[INFO_STATE.values().length];
							info_state = INFO_STATE.BYTE0;
							index = (index + 1)
									% BluetoothChatService.buffer_size;
							bytes--;
							data_index = 0;
						} else {
							signalError(2);
							return;
						}
						break;

					case BYTE0:
						pod_data[0] = response[index];
						info_state = INFO_STATE.BYTE1;
						bytes--;
						index = (index + 1) % BluetoothChatService.buffer_size;
						break;

					case BYTE1:
						pod_data[1] = response[index];
						info_state = INFO_STATE.BYTE2;
						bytes--;
						index = (index + 1) % BluetoothChatService.buffer_size;
						break;

					case BYTE2:
						pod_data[2] = response[index];
						info_state = INFO_STATE.BYTE3;
						bytes--;
						index = (index + 1) % BluetoothChatService.buffer_size;
						break;

					case BYTE3:
						pod_data[3] = response[index];
						info_state = INFO_STATE.IDLE;
						bytes--;
						index = (index + 1) % BluetoothChatService.buffer_size;

						// save data we received
						currentSwRev = pod_data;

						if (lockDialog) { // this gets set when we are doing a
											// FW update process
							lockDialog = false; // always unlock after receiving
												// data
							return;
						} else {
							// else this request was sent by the menu
							// create a dialog to display data to user
							blumote.showDialog(BluMote.DIALOG_SHOW_INFO);
						}
						break;
					}
				}
			} catch (Exception e) {
				Toast.makeText(blumote,
						"Communication error, exiting learn mode",
						Toast.LENGTH_SHORT).show();
				setIdleState();
				return;
			}
			break;

		case ABORT_LEARN:
			setIdleState(); // reset state
			if (response[index] == Codes.ACK) {
				// nothing
			}
			break;

		case IR_TRANSMIT:
			setIdleState();
			if (response[index] == Codes.ACK) {
				// release lock if we get an ACK
				buttonLock = false;
				if (BluMote.DEBUG) {
					Toast.makeText(blumote, "ACK received - lock removed",
							Toast.LENGTH_SHORT).show();
				}
			}
			break;

		case ABORT_TRANSMIT:
			setIdleState(); // reset state
			if (BluMote.DEBUG) {
				if (response[index] == Codes.ACK) {
					Toast.makeText(blumote, "ACK received for abort transmit",
							Toast.LENGTH_SHORT).show();
				}
			}
			break;

		case BSL:
			// Just log the messages we get from the Pod during BSL
			//Log.v("BSL", response.toString());
			int i = 0;
			pod_data = new byte[bytes];
			while (bytes > 0) {
				pod_data[i++] = response[index];
				bytes--;
				index = (index + 1) % BluetoothChatService.buffer_size;
			}
			setIdleState();
			break;

		case SYNC:
			// post the result to the Pod class
			sync_data = response[index];
			break;

		case READ_ADDRESS:
			while (bytes > 0) {
				if (data_index == 0) {
					// then parse response code
					if (response[index] == Codes.NACK) {
						setIdleState();
						pod_data = null;
						break;
					}
					data_index++;
					bytes--;
					index = (index + 1) % BluetoothChatService.buffer_size;
					continue;
				}
				pod_data[data_index - 1] = response[index];
				bytes--;
				index = (index + 1) % BluetoothChatService.buffer_size;
				if (data_index++ == total_bytes - 1) {
					// then we are done
					setIdleState(); // reset state
				}
			}
			break;
		}
	}

	static void requestLearn() {
		byte[] toSend;
		toSend = new byte[1];
		toSend[0] = (byte) Codes.LEARN;
		setLearnState();
		blumote.sendMessage(toSend);
	}

	static void abortLearn() {
		byte[] toSend;
		toSend = new byte[1];
		toSend[0] = (byte) Codes.ABORT_LEARN;
		setStopLearnState();
		blumote.sendMessage(toSend);
	}

	static void resetRn42() {
		byte[] toSend;
		toSend = new byte[1];
		toSend[0] = (byte) Codes.RESET_RN42;
		blumote.sendMessage(toSend);		
	}

	static void retrieveFwVersion() {
		clearPodData();
		byte[] toSend;
		setGetVerState();
		toSend = new byte[1];
		toSend[0] = (byte) Codes.GET_VERSION;
		blumote.sendMessage(toSend);
	}

	static void abortTransmit() {
		byte[] toSend;
		setStopTransmitState();
		toSend = new byte[1];
		toSend[0] = (byte) Codes.ABORT_TRANSMIT;
		blumote.sendMessage(toSend);
	}

	/**
	 * Determines if more bytes are being read that is available in the local
	 * data structure. This function should be called whenever a new set of data
	 * is COLLECTING in interpretResponse()
	 * 
	 * @param bytes
	 *            the number of bytes received
	 * @return false if the data is outside of the local storage space available
	 *         and true if there is no error.
	 */
	private static boolean checkPodDataBounds(int bytes) {
		if (bytes > (pod_data.length - data_index)) {
			return false;
		}
		return true;
	}

	static void setFwVersion(byte[] version) {
		currentSwRev = version;
	}

	static byte[] getFwVersion() {
		return currentSwRev;
	}

	/**
	 * Exit the bootstrap loader routine
	 * 
	 * @throws BslException
	 */
	static void exitBsl() throws BslException {
		byte test = 1 << 2; // PIO-10
		byte rst = 1 << 3; // PIO-11

		// http://www.ti.com/lit/ug/slau319a/slau319a.pdf
		// rst ________|------ (actually inverted)
		// test _______________
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", (rst | test), rst));
		Log.d("OUT_BSL_EXIT", String.format("S*,%02X%02X\r\n", (rst | test), rst));
		String returnStr = new String(receiveResponse(BT_STATES.BSL));		
		Log.d("IN_BSL_EXIT", returnStr);
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", rst, 0));
		Log.d("OUT_BSL_EXIT", String.format("S*,%02X%02X\r\n", rst, 0));
		returnStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_BSL_EXIT", returnStr);		
	}

	/**
	 * Enters the bootstrap loader routine
	 * 
	 * @throws BslException
	 */
	static void enterBsl() throws BslException {
		byte test = 1 << 2; // PIO-10
		byte rst = 1 << 3; // PIO-11

		// http://www.ti.com/lit/ug/slau319a/slau319a.pdf
		// rst _________|------ (actually inverted)
		// test ___|-|_|---|____
		// NOTE: inverted rst due to FET on the pod hw
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", (rst | test), rst));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", (rst | test), rst));
		String testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr);
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", test, test));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", test, test));
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", test, 0));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", test, 0));
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", test, test));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", test, test));
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", rst, 0));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", rst, 0));
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
		sendBSLString(String.format("S*,%02X%02X\r\n", test, 0));
		Log.d("OUT_ENTER_BSL", String.format("S*,%02X%02X\r\n", test, 0));
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
		// step 3, set to 9600 baud
		sendBSLString("U,9600,E\r\n");
		Log.d("OUT_ENTER_BSL", "U,9600,E\r\n");
		testStr = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_ENTER_BSL", testStr); 
		clearPodData();
	}

	/**
	 * Implements scheme to enter the BSL and get ready to receive the image
	 * 
	 * @param flag
	 *            flag = 1 when we want to inhibit the automatic reset of the
	 *            RN42
	 */
	static void startBSL(int flag) throws BslException {
		final int retryTime = 300; // 30 seconds
		final int timeOut = 1000; // 1 minute
		int timer = 0;
		if (flag == ENABLE_RESET) {
			// reset the RN42 module
			resetRn42();
			// connection will drop eventually, then need to reconnect....
			while (blumote.getBluetoothState() == BluetoothChatService.STATE_CONNECTED) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
				timer++;
				if (timer == retryTime) {
					Log.v("BSL", "Trying to reset again");
					resetRn42();
				}
				if (timer > timeOut) {
					// give up, indicate the reset failed which means user
					// should try a power cycle of pod
					throw new BslException(
							"Timed out waiting for RN42 to reset",
							BslException.RESET_FAILED);
				}
			}
		} else {
			while (blumote.getBluetoothState() == BluetoothChatService.STATE_CONNECTED) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// Auto-generated catch block
					e.printStackTrace();
				}
				if (timer > 200) {
					// give up, indicate the reset failed which means user
					// should try a power cycle of pod
					throw new BslException(
							"Timed out waiting for Bluetooth connection to drop",
							BslException.RESET_FAILED);
				}
			}
		}

		// once we got here the connection dropped
		// tell it to reconnect
		if (blumote.getBluetoothState() == BluetoothChatService.STATE_NONE) {
			blumote.reconnectPod();
		}
		timer = 0;
		// wait until it reconnects
		while (blumote.getBluetoothState() != BluetoothChatService.STATE_CONNECTED) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			timer++;
			if (timer == 50) {
				// try again
				Log.v("BSL", "Trying to connect again");
				if (blumote.getBluetoothState() == BluetoothChatService.STATE_NONE) {
					blumote.reconnectPod();
				}
			}
			if (timer >= 100) {
				// give up
				throw new BslException("Timed out waiting for RN42 to reset");
			}
		}

		// step 1, enter the command mode
		sendBSLString(BSL_CODES.CMD_MODE);
		Log.d("OUT_BSL_CMD_MODE", BSL_CODES.CMD_MODE);
		String response = new String(receiveResponse(BT_STATES.BSL));
		Log.d("IN_BSL_CMD_MODE", response);
		
		clearPodData();

		// step 2, enter the BSL
		enterBsl();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		// step 3, sending Rx password
		sendPassword();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
		// step 4, sending Rx password
		sendPassword();

		// after this is finished we are ready to start flashing the hex code to
		// the pod
	}

	/**
	 * This function will parse a stored hex file and search for the valid
	 * addresses of the interrupt vector bytes (32 bytes) and then overlay those
	 * bytes with a passwd[] which gets initialized to 0xFF's. This is used as
	 * the password for unlocking the BSL flash. The passwd array retains this
	 * value until this function is called again. This function should be called
	 * before the BSL is run, if it is not then the password will not be correct
	 * and flash will be wiped.
	 * 
	 * @param fileLocation
	 *            the location on flash for the downloaded firmware image
	 */
	static void calculatePassword(String fileLocation) {
		// re-initialize the private class passwd array to 0xFF for address
		// range 0xFFE0 to 0xFFFF (32 bytes)
		for (int i = 0; i < 32; i++) {
			passwd[i] = (byte) 0xFF;
		}

		if (fileLocation == null)
			return;

		// parse the hex file looking for an address in this range, if it exists
		// then dump
		// that value into the array at the appropriate index
		FileInputStream f = null;
		try {
			String strLine;
			f = new FileInputStream(new File(fileLocation));
			BufferedReader br = new BufferedReader(new InputStreamReader(f));
			br = new BufferedReader(new InputStreamReader(f));

			final int header = 3; // # chars
			final int recordType = 2; // # chars
			final int checksum = 2; // # chars
			final int address = 4; // # chars

			int intVectorStart = Util.bytesToInt((byte) 0xFF, (byte) 0xE0);
			while ((strLine = br.readLine()) != null) {
				char lineChars[] = strLine.toCharArray();
				int hexAddress = Integer.parseInt(new String(lineChars, header,
						4), 16);
				if (hexAddress >= intVectorStart) {
					int lineElements = lineChars.length - header - recordType
							- checksum - address;
					for (int i = 0; i < lineElements; i += 2) {
						// copy a byte of data into the array
						int offset = hexAddress - intVectorStart + i / 2;
						String value = new String(lineChars, header
								+ recordType + address + i, 2);
						passwd[offset] = (byte) Integer.parseInt(value, 16);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (f != null)
					f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void sendPassword() throws BslException {
		byte[] msg;

		sync();
		msg = new byte[] { (byte) 0x80, 0x10, 0x24, 0x24, 0x00, 0x00, 0x00,
				0x00 };

		msg = Util.concat(msg, passwd);
		msg = Util.concat(msg, calcChkSum(msg));
		blumote.sendMessage(msg);
		Log.d("OUT_BSL_PASS", Util.byteArrayToString(msg));
		String returnStr = Util.byteArrayToString(receiveResponse(BT_STATES.BSL));
		Log.d("IN_BSL_PASSWORD", returnStr);
		clearPodData();
	}

	/**
	 * Sync's the loader program to the pod, requires a ACK/NAK to continue
	 */
	static void sync() throws BslException {
		sync_data = (byte) 0xFF;
		BT_STATE = BT_STATES.SYNC;

		// send sync byte
		byte[] syncByte = { BSL_CODES.SYNC };
		blumote.sendMessage(syncByte);

		final BluMote.Wait waiter = new BluMote.Wait(100);

		while (waiter.waitTime < waiter.maxWaitTime) {
			// check if data was received yet from Pod
			if (sync_data != (byte) 0xFF) {
				break;
			} else {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new BslException("Error in sync function");
				}
				waiter.waitTime++;
			}
		}

		// process the data received, if it ever came
		if ((waiter.waitTime >= waiter.maxWaitTime)) {
			throw new BslException("Exceeded max wait time during sync");
		}

		if (sync_data == (byte) 0xFF) {
			throw new BslException("never received any data during sync");
		}

		switch (sync_data) {
		case BSL_CODES.DATA_ACK:
			return;

		case BSL_CODES.DATA_NAK:
			throw new BslException("Received NAK sync byte");

		default:
			throw new BslException("Received invalid sync byte "
					+ Integer.toString(0x00FF & (byte) sync_data));
		}
	}

	/**
	 * 1 byte checksum (LSB) for intel hex files calculated by adding all the
	 * fields then taking 2's complement
	 */
	private static byte hexChkSum(byte[] data) {
		byte result = 0;

		for (int i = 0; i < data.length; i++) {
			result = (byte) (data[i] + result);
		}
		// bitwise invert
		result = (byte) (0x100 - result);
		result++; // add 1
		return result;
	}

	/**
	 * calculate the 2 byte checksum for transactions to Pod
	 */
	private static byte[] calcChkSum(byte[] data) {
		byte[] result = { 0, 0 };

		for (int i = 0; i < data.length; i++) {
			if ((i % 2) == 1) {
				result[1] ^= data[i]; // upper byte
			} else {
				result[0] ^= data[i]; // lower byte
			}
		}

		result[0] ^= 0xFF;
		result[1] ^= 0xFF;

		return result;
	}

	/**
	 * Sends a string to the pod in ascii format
	 */
	public static void sendBSLString(String code) throws BslException {
		BT_STATE = BT_STATES.BSL;
		try {
			blumote.sendMessage(code.getBytes("ASCII"));
		} catch (UnsupportedEncodingException e) {
			throw new BslException("Encoding error while starting BSL");
		}
	}

	/**
	 * Sends one line of data from the .hex file to the BSL
	 * 
	 * @param line
	 * @throws BslException
	 */
	public static void sendFwLine(String line) throws BslException {
		sync();
		line = line.replace("\r\n", "");

		// <start_code><byte_cnt><addr><record_type><data><chksum>
		// overhead is bytes of each element in the formatted BSL packet (except
		// data)
		int overhead = 1 + 2 + 4 + 2 + 2;

		// improperly formatted file if doesn't start with :
		if (!line.startsWith(":")) {
			throw new BslException("malformed fw image line");
		}

		// verify the byte count matches the line record
		int assert1 = Integer.parseInt(line.substring(1, 3), 16); // byte_cnt
		int assert2 = (line.length() - overhead) / 2;
		if (assert1 != assert2) {
			throw new BslException("malformed fw image line");
		}

		// See TI MSP430 datasheet for the definition of these fields
		byte AH = (byte) Integer.parseInt(line.substring(3, 5), 16);
		byte AL = (byte) Integer.parseInt(line.substring(5, 7), 16);
		byte HDR = (byte) 0x80;
		byte CMD = 0x12;
		byte LL = (byte) (line.substring(9, line.length() - 1).length() / 2);
		if (LL == 0) {
			return;
		}
		byte LH = 0;
		byte L1 = (byte) (LL + 4);
		byte L2 = (byte) (LL + 4);

		byte[] msg = { HDR, CMD, L1, L2, AL, AH, LL, LH };

		byte[] data = new byte[LL];
		int j = 0;
		for (int i = 0; i < LL * 2; i += 2) {
			data[j] = (byte) Integer
					.parseInt(line.substring(i + 9, i + 11), 16);
			j++;
		}

		msg = Util.concat(msg, data);
		msg = Util.concat(msg, calcChkSum(msg));

		// send to Pod
		blumote.sendMessage(msg);
		byte[] returnData = receiveResponse(BT_STATES.BSL);
		Log.d("OUT_BSL_FW_LINE", Util.byteArrayToString(msg));		
		String stringRet = Util.byteArrayToString(returnData);		
		Log.d("IN_BSL_FW_LINE", stringRet);
		if (stringRet.equals("a0")) {
			throw new BslException("Got a NACK");
		}
		clearPodData();
	}
}
