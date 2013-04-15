// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

/**
 * Simple exception class to use in the bootloader code
 * @author keusej
 *
 */
public class BslException extends Exception {

	private static final long serialVersionUID = 1L;
	
	String oopsMsg;
	int TAG = -1;
	
	static final int RESET_FAILED = 0;
	
	public BslException() {
		super();
		oopsMsg = "unknown";
	}
	
	public BslException(String msg) {
		super();
		oopsMsg = msg;
	}
	
	public BslException(String msg, int tag) {
		super();
		oopsMsg = msg;
		TAG = tag;
	}
	
	@Override
	public String getMessage() {
		return oopsMsg;
	}
	
	public int getTag() {
		return TAG;
	}
}
