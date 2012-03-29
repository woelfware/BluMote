#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

from bluetooth import *
import bluemote
import cPickle
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

	def _learn_unpack_msg(self, msg):
		return_msg = [msg]
		pkt_nbr = 0

		print 'pkt %i len %i' % (pkt_nbr, len(msg))
		print 'ack/nak:', hex(ord(msg[0]))

		if len(msg) == 1:
			if ord(msg[0]) == 0x15:
				return
			msg = self.client_sock.recv(256)
			return_msg.append(msg)
			pkt_nbr += 1
			print 'pkt %i len %i' % (pkt_nbr, len(msg))

		code_len = ord(msg[1])
		print 'code length:', code_len
		frequency = ord(msg[2])
		print 'carrier frequency:', frequency, 'kHz'

		while (sum([len(str) for str in return_msg]) < code_len + 2):
			return_msg.append(self.client_sock.recv(256))

		return_msg = ''.join(return_msg)

		for i in xrange(4, len(return_msg), 2):
			print i, ':', int(ord(return_msg[i]) * 256 + ord(return_msg[i + 1]))

		return return_msg[1:]	# strip the ack

	def learn(self):
		self.transport_tx(self.cmd_codes.learn, "")
		msg = self.client_sock.recv(1024)
		return self._learn_unpack_msg(msg)

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

	done = False
	while not done:
		button_name = raw_input('What button would you like to learn ("done" to quit)? ')
		if button_name in ('done', '"done"'):
			done = True
			continue
		print 'Please push %s on your remote.' % (button_name)
		key_code = bm_remote.learn()
		button = open('%s.pkl' % (''.join(button_name.split())), 'wb')
		cPickle.dump(key_code, button, cPickle.HIGHEST_PROTOCOL)
		button.close()

	bm_remote.client_sock.close()
