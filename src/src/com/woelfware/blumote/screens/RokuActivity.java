// Copyright (C) 2011 Woelfware

package com.woelfware.blumote.screens;

import android.widget.ImageButton;

import com.woelfware.blumote.BluMote;
import com.woelfware.blumote.ButtonCreator;
import com.woelfware.blumote.ButtonParameters;
import com.woelfware.blumote.R;

public class RokuActivity implements ButtonCreator {
	private BluMote blumote;
	
	public ButtonParameters[] getButtons(BluMote blumote) {
		this.blumote = blumote;
		
		ButtonParameters[] buttons = {
			new ButtonParameters(R.id.btn_volume_up, "btn_volume_up", blumote.findViewById(R.id.btn_volume_up)),
			new ButtonParameters(R.id.btn_volume_down, "btn_volume_down", blumote.findViewById(R.id.btn_volume_down)),
			new ButtonParameters(R.id.power_on_btn, "power_on_btn", blumote.findViewById(R.id.power_on_btn)),
			new ButtonParameters(R.id.power_off_btn, "power_off_btn", blumote.findViewById(R.id.power_off_btn)),
			new ButtonParameters(R.id.back_btn, "back_btn", blumote.findViewById(R.id.back_btn)),
			new ButtonParameters(R.id.forward_btn, "forward_btn", blumote.findViewById(R.id.forward_btn)),
			new ButtonParameters(R.id.play_btn, "play_btn", blumote.findViewById(R.id.play_btn)),
			new ButtonParameters(R.id.mute_btn, "mute_btn", blumote.findViewById(R.id.mute_btn)),
			new ButtonParameters(R.id.btn_enter, "btn_enter", blumote.findViewById(R.id.btn_enter)),
			new ButtonParameters(R.id.btn_home, "btn_home", blumote.findViewById(R.id.btn_home)),
			new ButtonParameters(R.id.left_btn, "left_btn", blumote.findViewById(R.id.left_btn)),
			new ButtonParameters(R.id.right_btn, "right_btn", blumote.findViewById(R.id.right_btn)),
			new ButtonParameters(R.id.btn_up, "btn_up", blumote.findViewById(R.id.btn_up)),
			new ButtonParameters(R.id.down_btn, "down_btn", blumote.findViewById(R.id.down_btn)),
			new ButtonParameters(R.id.btn_misc1, "btn_misc1", blumote.findViewById(R.id.btn_misc1)),
			new ButtonParameters(R.id.fav_btn, "fav_btn", blumote.findViewById(R.id.fav_btn)),
			new ButtonParameters(R.id.return_btn, "return_btn", blumote.findViewById(R.id.return_btn)),
		};
		return buttons;
	}
	
	public ImageButton getPowerOnBtn() {
		return (ImageButton)blumote.findViewById(R.id.power_on_btn);
	}
	
	public ImageButton getPowerOffBtn() {
		return (ImageButton)blumote.findViewById(R.id.power_off_btn);
	}
}
