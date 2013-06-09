// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

/** 
 * 
 * This class will structure button data
 * The sourceDevice, buttonId, buttonCategory and buttonName fields are read only
 * buttonData can be set after creation if needed
 * @author keusej
 *
 */
public class ButtonData {
	private int buttonId;
	private byte[] buttonData;
	private String buttonName;
	private String sourceDevice; // what is the device associated with this button
	
	// takes an activity and activity button and converts to a device and button
	public ButtonData(int buttonId, String buttonName, byte[] buttonData, String sourceDevice) {
		this.buttonId = buttonId;
		this.buttonName = buttonName;
		this.buttonData = buttonData;
		this.sourceDevice = sourceDevice;
	}
	
	int getButtonId() {
		return buttonId;
	}
	
	String getSourceDevice() {
		return sourceDevice;
	}
	
	String getButtonName() {
		return buttonName;
	}
	
	byte[] getButtonData() {
		return buttonData;
	}		
	
	void setButtonData(byte[] buttonData) {
		this.buttonData = buttonData;
	}
}
