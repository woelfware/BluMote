// Copyright (C) 2011 Woelfware

package com.woelfware.blumote.screens;

import android.widget.ImageButton;

import com.woelfware.blumote.BluMote;
import com.woelfware.blumote.ButtonCreator;
import com.woelfware.blumote.ButtonParameters;

public class Blank implements ButtonCreator {
	@SuppressWarnings("unused")
	private BluMote blumote;
	
	public ButtonParameters[] getButtons(BluMote blumote) {
		this.blumote = blumote;
		
		ButtonParameters[] buttons = null;
		return buttons;
	}
	
	public ImageButton getPowerOnBtn() {
		return null;
	}
	
	public ImageButton getPowerOffBtn() {
		return null;
	}
}
