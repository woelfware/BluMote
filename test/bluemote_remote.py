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
		# Create the client socket
		self.client_sock = BluetoothSocket(RFCOMM)
		self.client_sock.connect((addr, 1))

	def transport_tx(self, cmd, msg):
		full_msg = ''.join((struct.pack("B", cmd), msg))
		self.client_sock.send(full_msg)

	def rename_device(self, name):
		self.transport_tx(self.cmd_codes.rename_device, name)
		pass

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
		msg = self.client_sock.recv(256)
		return self._learn_unpack_msg(msg)

	def _get_calibration_unpack_msg(self, msg):
		calibration = []
		full_msg = struct.unpack(len(msg) * 'B', msg)
		return full_msg

	def get_calibration(self, addr, length):
		print 'Sending request for calibration data.'

		self.transport_tx(self.cmd_codes.get_calibration, struct.pack('>HB', addr, length))
		msg = self.client_sock.recv(256)
		return self._get_calibration_unpack_msg(msg)

	def _get_version_unpack_msg(self, msg):
		version = []
		full_msg = struct.unpack(len(msg) * "B", msg)
		flags = full_msg[0]
		versions = full_msg[1:]

		i = 0
		while i < len(versions):
			for cc in dir(self.component_codes):
				if getattr(self.component_codes, cc) == versions[i]:
					version.append((cc,
							"%u.%u.%u" % \
								(versions[i + 1], \
								 versions[i + 2], \
								 versions[i + 3])))
			i += 4

		return version

	def get_version(self):
		self.transport_tx(self.cmd_codes.get_version, "")
		msg = self.client_sock.recv(256)
		return self._get_version_unpack_msg(msg)

	def ir_transmit(self, msg):
		self.transport_tx(self.cmd_codes.ir_transmit, msg)
		return self.client_sock.recv(256)

	def _debug_unpack_msg(self, msg):
		return self._learn_unpack_msg(msg)

	def debug(self, msg = ""):
		self.transport_tx(self.cmd_codes.debug, msg)
		msg = self.client_sock.recv(256)
		return self._debug_unpack_msg(msg)

if __name__ == "__main__":
	bm_remote = Bluemote_Client()

	try:
		found = False
		while found == False:
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

		print 'getting version info'
		version = bm_remote.get_version()
		for component in version:
			print "%s version: %s" % component

		print ['%X' % i for i in bm_remote.get_calibration(0x10F6, 10)]

		bm_remote.client_sock.close()
	except IOError:
		pass
	finally:
		try:
			bm_remote.client_sock.close()
		except:
			pass
