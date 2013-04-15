// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

/** 
 * 
 * This class will structure button data
 * The buttonId, buttonCategory and buttonName fields are write only
 * buttonData can be set after creation if needed
 * @author keusej
 *
 */
public class ButtonData {
	private int buttonId;
	private byte[] buttonData;
	private String buttonName;
	
	// takes an activity and activity button and converts to a device and button
	public ButtonData(int buttonId, String buttonName, byte[] buttonData) {
		this.buttonId = buttonId;
		this.buttonName = buttonName;
		this.buttonData = buttonData;
	}
	
	int getButtonId() {
		return buttonId;
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
