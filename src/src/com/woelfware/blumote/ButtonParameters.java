// Copyright (C) 2011 Woelfware

package com.woelfware.blumote;

import android.view.View;

/**
 * Parameters every interface button contains, used in the 
 * initialize() function of ButtonCreator objects
 * @author keusej
 *
 */
public class ButtonParameters {
	private int resourceID;
	private String buttonName;
	private View button;
	
	public ButtonParameters (int resourceId, String buttonName, View button) {
		this.resourceID = resourceId;
		this.button = button;
		this.buttonName = buttonName;
	}
	
	public int getID() {
		return resourceID;
	}
	
	public String getName() {
		return buttonName;
	}
	
	public View getView() {
		return button;
	}
}
