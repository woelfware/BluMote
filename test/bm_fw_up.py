#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

import bluetooth
import struct
import sys
import time
import ctypes

SYNC = 0x80
DATA_ACK = 0x90
DATA_NAK = 0xA0

def find_blumotes():
	found = False

	print 'Searching for BluMote devices...'

	while not found:
		try:
			nearby_devices = bluetooth.discover_devices(lookup_names = True)
			if len(nearby_devices):
				found = True
		except IOError:
			# no Bluetooth devices found
			pass

	return nearby_devices

def get_target_addr(blumote_devices, auto_select = False):
	valid_selection = False
	while not valid_selection:
		if auto_select:
			target = 0
		else:
			print '\nSelect the BluMote you would like to update:'
			print '\t0) Cancel'
			for i in xrange(len(blumote_devices)):
				print '\t%i) %s [%s]' % (i + 1, blumote_devices[i][1], blumote_devices[i][0])
			else:
				target = int(raw_input()) - 1

		if target < 0:
			return None
		elif target < len(blumote_devices):
			valid_selection = True
		else:
			print 'Invalid selection!  Try again...'
			time.sleep(1)

	if auto_select:
		print 'Auto selecting %s [%s].' % (blumote_devices[0][1], blumote_devices[0][0])

	return blumote_devices[target][0]

class BluMote(bluetooth.BluetoothSocket):
	def __init__(self, protocol = bluetooth.RFCOMM):
		bluetooth.BluetoothSocket.__init__(self, protocol)
		
	def __enter__(self):
		return self

	def __exit__(self, type, value, traceback):
		self.close()
		return isinstance(value, IOError)

	def calc_chksum(self, data):
		ckl = ckh = 0
		for i in xrange(len(data)):
			if i % 2:
				ckh ^= data[i]
			else:
				ckl ^= data[i]
		ckl ^= 0xFF
		ckh ^= 0xFF

		return (ckl, ckh)
		
	def reset_rn42(self):
		self.send('04');

	def enter_bsl(self):
		test = 1 << 2	# PIO-10
		rst = 1 << 3	# PIO-11

		# http://www.ti.com/lit/ug/slau319a/slau319a.pdf
		# rst  ________|------
		# test ___|-|_|--|____
		self.send('S*,%02X%02X\r\n' % (rst | test, rst))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (test, test))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (test, 0))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (test, test))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (rst, 0))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (test, 0))
		self.recv(128)

	def enter_cmd_mode(self):
		self.send('$$$')
		return self.recv(128)

	def exit_bsl(self):
		test = 1 << 2	# PIO-10
		rst = 1 << 3	# PIO-11

		# http://www.ti.com/lit/ug/slau319a/slau319a.pdf
		# rst  ________|------
		# test _______________
		self.send('S*,%02X%02X\r\n' % (rst | test, 0))
		self.recv(128)
		self.send('S*,%02X%02X\r\n' % (rst, rst))
		self.recv(128)

	def get_buffer_size(self):
		msg = (SYNC,)
		self.send(struct.pack('B' * len(msg), *msg))
		return self.recv(128)

	def erase_mem(self):
		self.sync()
		main_erase_cycles = 12
		
		msg = [0x80, 0x16, 0x04, 0x04, 0x00, 0xff , 0x04, 0xA5]
		msg.extend(self.calc_chksum(msg))
		# I guess need to do this multiple times to accommodate
		# write cycles required to finish?
		for i in range(main_erase_cycles):
			self.send(struct.pack('B' * len(msg), *msg))
			return self.recv(128)
		
	def mass_erase(self):
		self.sync()

		msg = [0x80, 0x18, 0x04, 0x04, 0x00, 0x00, 0x06, 0xA5]
		msg.extend(self.calc_chksum(msg))

		self.send(struct.pack('B' * len(msg), *msg))
		return self.recv(128)

	def read_mem(self):
		print 'sending rx data block...'
		self.sync()
		msg = (0x80, 0x14, 0x04, 0x04, 0xF0, 0x0F, 0x0E, 0x00, 0x85, 0xE0)
		self.send(struct.pack('B' * len(msg), *msg))
		self.settimeout(1.0)
		try:
			return self.recv(128)
		except:
			raise

	def test_password(self):
		self.sync()
		msg = [0x80, 0x10, 0x24, 0x24, 0x00, 0x00, 0x00, 0x00]
		
		# TESTING, password generated from phone app
		passwd = (-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 38, -25, 102, -21, -10, -21, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -2, -23)
		passwd = [ctypes.c_ubyte(i).value for i in passwd]
		
		msg.extend(passwd)
		msg.extend(self.calc_chksum(msg))

		self.send(struct.pack('B' * len(msg), *msg))
		return self.recv(128)
		
	def rx_password(self):
		self.sync()
		msg = [0x80, 0x10, 0x24, 0x24, 0x00, 0x00, 0x00, 0x00]
				
		passwd = (
			0xFF, 0xFF,	# 0xFFE0
			0xFF, 0xFF,	# 0xFFE2
			0xFF, 0xFF,	# 0xFFE4
			0xFF, 0xFF,	# 0xFFE6
			0xFF, 0xFF,	# 0xFFE8
			0xFF, 0xFF,	# 0xFFEA
			0xFF, 0xFF,	# 0xFFEC
			0xFF, 0xFF,	# 0xFFEE
			0xFF, 0xFF,	# 0xFFF0
			0xFF, 0xFF,	# 0xFFF2
			0xFF, 0xFF,	# 0xFFF4
			0xFF, 0xFF,	# 0xFFF6
			0xFF, 0xFF,	# 0xFFF8
			0xFF, 0xFF,	# 0xFFFA
			0xFF, 0xFF,	# 0xFFFC
			0xFF, 0xFF)	# 0xFFFE
		
		msg.extend(passwd)
		msg.extend(self.calc_chksum(msg))

		self.send(struct.pack('B' * len(msg), *msg))
		return self.recv(128)

	def send_data_frame(self, cmd, addr, data):
		hdr = struct.pack('B', 80)
		cmd = struct.pack('B', cmd)
		addr = struct.pack('<H', addr)
		nbr_of_bytes = struct.pack('<H', len(addr) + len(data))
		nbr_of_pure_data_bytes = struct.pack('<H', len(data))
		msg = ''.join((hdr, cmd, nbr_of_bytes, addr, nbr_of_pure_data_bytes, data))

		# calculate the checksum
		data = struct.unpack('B' * len(msg), msg)
		ckl = ckh = 0
		for i in xrange(len(data)):
			if i % 2:
				ckh ^= data[i]
			else:
				ckl ^= data[i]
		ckl ^= 0xFF
		ckh ^= 0xFF

		msg = ''.join((msg, struct.pack('BB', ckl, ckh)))
		self.send(msg)

	def send_hex(self, fname):
		fh = open(fname, 'r')

		for line in fh:
			self.sync()
			line = line.rstrip('\r\n')
			# <start_code><byte_cnt><addr><record_type><data><chksum>
			assert(line[0] == ':')
			overhead = 1 + 2 + 4 + 2 + 2
			assert(int(line[1:3], 16) == (len(line) - overhead) / 2)
			AH = int(line[3:5], 16)
			AL = int(line[5:7], 16)
			HDR = 0x80
			CMD = 0x12
			LL = len(line[9:-2]) / 2
			if LL == 0:
				continue
			LH = 0
			L1 = L2 = LL + 4
			msg = [HDR, CMD, L1, L2, AL, AH, LL, LH]
			data = [int(line[i:i + 2], 16) for i in xrange(9, len(line) - 3, 2)]
			msg.extend(data)
			msg.extend(self.calc_chksum(msg))
			self.send(struct.pack('B' * len(msg), *msg))
			msg = self.recv(128)
			print [hex(i) for i in struct.unpack('B' * len(msg), msg)]

	def set_baud_9600(self):
		self.send('U,9600,E\r\n')
		return self.recv(128)

	def set_baud_115k(self):
		self.send('SU,11\r\n')
		return self.recv(128)

	def sync(self):
		print 'Handshaking...'
		self.send(struct.pack('B', SYNC))
		rc = self.recv(128)
		rc = struct.unpack('B' * len(rc), *rc)
		if rc[0] == 0:
			print 'sync received:', [hex(i) for i in rc]
			self.sync()
			rc = struct.unpack('B' * len(rc), *rc)
		if rc[0] == DATA_ACK:
			print 'Handshake is successful.'
			return True
		else:
			print 'Handshake is unsuccessful.'
			return False

if __name__ == '__main__':
	if (len(sys.argv) != 2):
		print 'Usage: %s <blumote.hex>'
		exit()

	blumotes = find_blumotes()
	addr = get_target_addr(blumotes, False)

	if addr is None:
		print 'Cancelled... good bye.'
		exit()

	with BluMote() as bm_up:
		#bm_up.connect(('00:06:66:42:05:91', 1))
		bm_up.connect((addr, 1))		
		
		print 'Entering command mode:', bm_up.enter_cmd_mode()

		print 'Entering the BSL...'
		bm_up.enter_bsl()

		print 'Setting the RN-42 UART baud to 9600:', bm_up.set_baud_9600()

		#### COMMENT OUT FOR OLD CODE
		#print 'sending rx password...'
		#msg = bm_up.rx_password()
		#msg = bm_up.test_password()
		#print [hex(i) for i in struct.unpack('B' * len(msg), msg)]

		print 'sending rx password...'
		msg = bm_up.rx_password()
		print [hex(i) for i in struct.unpack('B' * len(msg), msg)]
		print 'sending rx password...'
		msg = bm_up.rx_password()
		print [hex(i) for i in struct.unpack('B' * len(msg), msg)]
		
		#### COMMENT OUT FOR OLD CODE
		#print 'erasing main memory...'
		#bm_up.erase_mem()

		print 'sending %s' % (sys.argv[1],)
		bm_up.send_hex(sys.argv[1])

		print 'Entering command mode:', bm_up.enter_cmd_mode()
		print 'Exiting the BSL...'
		bm_up.exit_bsl()	# this resets the firmware, which will in turn reset the rn-42

