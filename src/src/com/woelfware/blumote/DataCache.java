package com.woelfware.blumote;

/**
 * singleton class to hold data (for example from bluetooth) for later processing
 * @author keusej
 *
 */
class DataCache {

	private DataCache() { }
	// private instance for getInstance()
	private static DataCache mInstance = new DataCache();
	
	private byte[] dataCache;
	
	static DataCache getInstance()
	{
	   return mInstance;
	}	
		
	synchronized void storeBluetoothData(byte[] data, int bytes, int index) {
		// check for possibility of data resource leak
		int length;
		if (dataCache == null) {
			length = 0; 
		} else {
			length = dataCache.length;
		}
		if (length > 1000) {
			dataCache = null; // delete and start over
			length = 0;
		}
		
		byte[] C= new byte[length+bytes];
		if (dataCache != null) {
			System.arraycopy(dataCache, 0, C, 0, length);
		}
		System.arraycopy(data, index, C, length, bytes);
		dataCache = C;
	}
	
	synchronized void clearBluetoothData() {
		dataCache = null;
	}
	
	synchronized boolean hasData() {
		if (dataCache != null) {
			return true;
		} else {
			return false;
		}
	}
	
	synchronized byte[] getData() {
		if (dataCache != null) {
			byte[] toReturn = new byte[dataCache.length];
			System.arraycopy(dataCache, 0, toReturn, 0, dataCache.length);
			dataCache = null;
			return toReturn;
		} else 
			return null;
	}
	
}
