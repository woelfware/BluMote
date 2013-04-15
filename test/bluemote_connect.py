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
		#self.client_sock.settimeout(500)
		self.client_sock.connect((addr, 1))

	def transport_tx(self, cmd, msg):
		full_msg = ''.join((struct.pack("B", cmd), msg))
		self.client_sock.send(full_msg)

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

if __name__ == "__main__":
	bm_remote = Bluemote_Client()

	try:
		found = False
		
		if len(sys.argv) > 1:
			found = True
			addr = sys.argv[1]
			bm_remote.connect_to_bluemote_pod(addr)
		#if no argument passed then search
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

		version = bm_remote.get_version()
		for component in version:
			print "%s version: %s" % component

		bm_remote.client_sock.close()
	except IOError:
		pass
	finally:
		try:
			bm_remote.client_sock.close()
		except:
			pass
