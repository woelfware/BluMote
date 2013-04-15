#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

class Command_Codes():
	def __init__(self):
		self.get_version	= 0x00
		self.learn	= 0x01
		self.ir_transmit	= 0x02
		self.ir_transmit_abort	= 0x03
		self.reset_bluetooth	= 0x04
		self.get_calibration	= 0x05
		self.debug         = 0xFF	# specialized debug command whose functionality change whenever

class Command_Return_Codes():
	def __init__(self):
		self.ack             = 0x06
		self.nak             = 0x15

class Component_Codes():
	def __init__(self):
		self.hardware = 0x00
		self.firmware = 0x01
		self.software = 0x02

class Services():
	def __init__(self):
		self.cmd_codes = Command_Codes()
		self.cmd_rc = Command_Return_Codes()
		self.component_codes = Component_Codes()
		self.service = {"name" : "Blumote",
			"provider" : "Woelfware",
			"description" : "IR XPDR"}
