#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

from bluetooth import *
import bluemote
import cPickle
from glob import glob
import os
import sys
import time

class Bluemote_Client(bluemote.Services):
	def __init__(self):
		bluemote.Services.__init__(self)
		self.addr = None

	def find_bluemote_pods(self, pod_name = None):
		if pod_name is None:
			pod_name = self.service["name"]
		print "Searching for \"%s\" service..." % (pod_name)
		return find_service(name = pod_name)

	def connect_to_bluemote_pod(self, addr):
		self.client_sock = BluetoothSocket(RFCOMM)
		self.client_sock.connect((addr, 1))

	def transport_tx(self, cmd, msg):
		full_msg = struct.pack("B", cmd)
		full_msg += msg
		self.client_sock.send(full_msg)

	def ir_transmit(self, msg):
		self.transport_tx(self.cmd_codes.ir_transmit, msg)
		return self.client_sock.recv(128)

if __name__ == "__main__":
	bm_remote = Bluemote_Client()

	found = False
	while not found:
		try:
			nearby_devices = discover_devices(lookup_names = True)
		except:
			print 'failed to find a blumote... retrying'
			nearby_devices = ()
		print 'found %d device(s)' % len(nearby_devices)
		for addr, name in nearby_devices:
			if name[:len('BluMote')] == 'BluMote':
				print 'connecting to', addr, name
				bm_remote.connect_to_bluemote_pod(addr)
				found = True
				break

	buttons = glob('*.pkl')

	print 'Available buttons:'
	for i, button in enumerate(buttons):
		print '\t%i: %s' % (i, os.path.splitext(button)[0])
	print

	while True:
		selection = raw_input('Select a button to transmit (-1 to quit): ')
		try:
			selection = int(selection)
		except ValueError:
			print 'Invalid selection'
			continue
		if selection == -1:
			break
		if ((selection < 0) or (selection >= len(buttons))):
			print 'Invalid selecion'
			continue

		button = open(buttons[selection], 'rb')
		key_code = cPickle.load(button)
		button.close()
		bm_remote.ir_transmit(''.join(['\x03', key_code]))

	bm_remote.client_sock.close()

