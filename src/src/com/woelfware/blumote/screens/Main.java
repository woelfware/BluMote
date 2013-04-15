// Copyright (C) 2011 Woelfware

package com.woelfware.blumote.screens;

import android.widget.ImageButton;

import com.woelfware.blumote.BluMote;
import com.woelfware.blumote.ButtonCreator;
import com.woelfware.blumote.ButtonParameters;
import com.woelfware.blumote.R;

public class Main implements ButtonCreator {
	private BluMote blumote;
	
	public ButtonParameters[] getButtons(BluMote blumote) {
		this.blumote = blumote;
		
		ButtonParameters[] buttons = {
			new ButtonParameters(R.id.btn_volume_up, "btn_volume_up", blumote.findViewById(R.id.btn_volume_up)),
			new ButtonParameters(R.id.btn_volume_down, "btn_volume_down", blumote.findViewById(R.id.btn_volume_down)),
			new ButtonParameters(R.id.btn_channel_up, "btn_channel_up", blumote.findViewById(R.id.btn_channel_up)),
			new ButtonParameters(R.id.btn_channel_down, "btn_channel_down", blumote.findViewById(R.id.btn_channel_down)),
			new ButtonParameters(R.id.btn_input, "btn_input", blumote.findViewById(R.id.btn_input)),
			new ButtonParameters(R.id.power_on_btn, "power_on_btn", blumote.findViewById(R.id.power_on_btn)),
			new ButtonParameters(R.id.power_off_btn, "power_off_btn", blumote.findViewById(R.id.power_off_btn)),
			new ButtonParameters(R.id.back_skip_btn, "back_skip_btn", blumote.findViewById(R.id.back_skip_btn)),
			new ButtonParameters(R.id.back_btn, "back_btn", blumote.findViewById(R.id.back_btn)),
			new ButtonParameters(R.id.forward_btn, "forward_btn", blumote.findViewById(R.id.forward_btn)),
			new ButtonParameters(R.id.skip_forward_btn, "skip_forward_btn", blumote.findViewById(R.id.skip_forward_btn)),
			new ButtonParameters(R.id.record_btn, "record_btn", blumote.findViewById(R.id.record_btn)),
			new ButtonParameters(R.id.stop_btn, "stop_btn", blumote.findViewById(R.id.stop_btn)),
			new ButtonParameters(R.id.play_btn, "play_btn", blumote.findViewById(R.id.play_btn)),
			new ButtonParameters(R.id.eject_btn, "eject_btn", blumote.findViewById(R.id.eject_btn)),
			new ButtonParameters(R.id.disc_btn, "disc_btn", blumote.findViewById(R.id.disc_btn)),
			new ButtonParameters(R.id.mute_btn, "mute_btn", blumote.findViewById(R.id.mute_btn)),
			new ButtonParameters(R.id.info_btn, "info_btn", blumote.findViewById(R.id.info_btn)),
			new ButtonParameters(R.id.return_btn, "return_btn", blumote.findViewById(R.id.return_btn)),
			new ButtonParameters(R.id.pgup_btn, "pgup_btn", blumote.findViewById(R.id.pgup_btn)),
			new ButtonParameters(R.id.pgdn_btn, "pgdn_btn", blumote.findViewById(R.id.pgdn_btn)),
			new ButtonParameters(R.id.guide_btn, "guide_btn", blumote.findViewById(R.id.guide_btn)),
			new ButtonParameters(R.id.exit_btn, "exit_btn", blumote.findViewById(R.id.exit_btn)),
			new ButtonParameters(R.id.pause_btn, "pause_btn", blumote.findViewById(R.id.pause_btn)),
			new ButtonParameters(R.id.fav_btn, "fav_btn", blumote.findViewById(R.id.fav_btn)),
			new ButtonParameters(R.id.btn_last, "btn_last", blumote.findViewById(R.id.btn_last)),
			new ButtonParameters(R.id.btn_n0, "btn_n0", blumote.findViewById(R.id.btn_n0)),
			new ButtonParameters(R.id.btn_n1, "btn_n1", blumote.findViewById(R.id.btn_n1)),
			new ButtonParameters(R.id.btn_n2, "btn_n2", blumote.findViewById(R.id.btn_n2)),
			new ButtonParameters(R.id.btn_n3, "btn_n3", blumote.findViewById(R.id.btn_n3)),
			new ButtonParameters(R.id.btn_n4, "btn_n4", blumote.findViewById(R.id.btn_n4)),
			new ButtonParameters(R.id.btn_n5, "btn_n5", blumote.findViewById(R.id.btn_n5)),
			new ButtonParameters(R.id.btn_n6, "btn_n6", blumote.findViewById(R.id.btn_n6)),
			new ButtonParameters(R.id.btn_n7, "btn_n7", blumote.findViewById(R.id.btn_n7)),
			new ButtonParameters(R.id.btn_n8, "btn_n8", blumote.findViewById(R.id.btn_n8)),
			new ButtonParameters(R.id.btn_n9, "btn_n9", blumote.findViewById(R.id.btn_n9)),
			new ButtonParameters(R.id.btn_dash, "btn_dash", blumote.findViewById(R.id.btn_dash)),
			new ButtonParameters(R.id.btn_enter, "btn_enter", blumote.findViewById(R.id.btn_enter)),
			new ButtonParameters(R.id.btn_exit, "btn_exit", blumote.findViewById(R.id.btn_exit)),
			new ButtonParameters(R.id.btn_home, "btn_home", blumote.findViewById(R.id.btn_home)),
			new ButtonParameters(R.id.left_btn, "left_btn", blumote.findViewById(R.id.left_btn)),
			new ButtonParameters(R.id.right_btn, "right_btn", blumote.findViewById(R.id.right_btn)),
			new ButtonParameters(R.id.btn_up, "btn_up", blumote.findViewById(R.id.btn_up)),
			new ButtonParameters(R.id.down_btn, "down_btn", blumote.findViewById(R.id.down_btn)),
			new ButtonParameters(R.id.btn_misc1, "btn_misc1", blumote.findViewById(R.id.btn_misc1)),
			new ButtonParameters(R.id.btn_misc2, "btn_misc2", blumote.findViewById(R.id.btn_misc2)),
			new ButtonParameters(R.id.btn_misc3, "btn_misc3", blumote.findViewById(R.id.btn_misc3)),
			new ButtonParameters(R.id.btn_misc4, "btn_misc4", blumote.findViewById(R.id.btn_misc4)),
			new ButtonParameters(R.id.btn_misc5, "btn_misc5", blumote.findViewById(R.id.btn_misc5)),
			new ButtonParameters(R.id.btn_misc6, "btn_misc6", blumote.findViewById(R.id.btn_misc6)),
			new ButtonParameters(R.id.btn_misc7, "btn_misc7", blumote.findViewById(R.id.btn_misc7)),
			new ButtonParameters(R.id.btn_misc8, "btn_misc8", blumote.findViewById(R.id.btn_misc8)),
			new ButtonParameters(R.id.green_btn, "green_btn", blumote.findViewById(R.id.green_btn)),
			new ButtonParameters(R.id.red_btn, "red_btn", blumote.findViewById(R.id.red_btn)),
			new ButtonParameters(R.id.blue_btn, "blue_btn", blumote.findViewById(R.id.blue_btn)),
			new ButtonParameters(R.id.yellow_btn, "yellow_btn", blumote.findViewById(R.id.yellow_btn)),
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
