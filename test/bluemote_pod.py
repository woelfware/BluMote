#!/usr/bin/env python
# Copyright (C) 2010 Woelfware

from bluetooth import *
import bluemote
import time
import sys
import subprocess
import os
#import multiprocessing

# sample key codes of buttons "0" through "9" for use in creating a learning function
sample_key_codes = [[8985, 4475, 564, 569, 539, 1694, 568, 1667, 566, 564, 564, 566, 538, 566, 566, 564, 568, 1668, 570, 1689, 542, 564, 564, 1676, 560, 563, 567, 539, 593, 537, 569, 537, 593, 537, 592, 1644, 588, 544, 588, 542, 561, 1672, 590, 537, 569, 560, 570, 537, 591, 542, 563, 540, 590, 1672, 564, 1670, 590, 537, 569, 1669, 588, 1648, 588, 1672, 564, 1671, 591],
	[9071, 4389, 644, 477, 653, 1592, 648, 1611, 624, 471, 659, 471, 659, 473, 633, 470, 660, 1612, 624, 1609, 650, 469, 633, 1614, 648, 472, 632, 472, 660, 470, 662, 468, 635, 468, 665, 466, 663, 468, 634, 470, 660, 472, 658, 471, 632, 473, 661, 469, 662, 468, 635, 1612, 648, 1585, 651, 1611, 622, 1612, 650, 1586, 650, 1610, 621, 1614, 648, 1586, 652],
	[9087, 4377, 646, 480, 653, 1583, 650, 1584, 622, 499, 661, 477, 655, 477, 626, 478, 655, 1607, 628, 1607, 651, 480, 626, 1609, 653, 475, 628, 475, 656, 474, 657, 476, 625, 480, 651, 479, 650, 480, 629, 475, 654, 476, 655, 475, 630, 473, 657, 475, 653, 479, 624, 1610, 655, 1581, 652, 1608, 625, 1610, 651, 1583, 652, 1612, 624, 1607, 655, 1581, 650],
	[9091, 4372, 650, 477, 626, 1610, 652, 1583, 653, 477, 653, 477, 626, 480, 652, 476, 657, 1578, 656, 1607, 627, 476, 653, 1609, 626, 475, 658, 473, 656, 473, 631, 473, 659, 475, 654, 1580, 652, 480, 655, 473, 630, 473, 657, 473, 659, 474, 630, 473, 655, 477, 655, 475, 626, 1609, 655, 1581, 655, 1605, 599, 1635, 654, 1581, 655, 1607, 627, 1607, 655],
	[9060, 4400, 646, 457, 678, 1585, 646, 1587, 677, 455, 644, 460, 680, 447, 682, 451, 648, 1588, 679, 1556, 675, 455, 677, 1557, 681, 448, 684, 446, 653, 430, 707, 443, 687, 446, 653, 453, 681, 1579, 650, 451, 686, 446, 686, 446, 655, 447, 685, 447, 683, 449, 654, 1579, 684, 422, 679, 1581, 684, 1552, 679, 1581, 650, 1583, 683, 1530, 706, 1576, 654],
	[9057, 4400, 647, 457, 675, 1587, 644, 1592, 670, 458, 648, 455, 675, 455, 677, 455, 649, 1587, 670, 1566, 670, 457, 675, 1561, 674, 456, 679, 453, 646, 457, 673, 460, 677, 452, 647, 1587, 675, 1561, 672, 456, 679, 452, 644, 462, 675, 455, 677, 453, 647, 455, 679, 453, 681, 449, 648, 1588, 670, 1565, 677, 1583, 646, 1579, 687, 1557, 675, 1587, 644],
	[9091, 4391, 682, 448, 682, 1554, 683, 1577, 656, 445, 688, 442, 690, 444, 655, 448, 685, 1576, 659, 1554, 682, 443, 686, 1577, 684, 446, 657, 447, 683, 444, 691, 441, 662, 420, 710, 442, 688, 442, 661, 1577, 683, 447, 657, 424, 710, 442, 688, 442, 661, 443, 690, 1573, 656, 1578, 684, 446, 664, 1572, 685, 1548, 688, 1575, 657, 1576, 690, 1546, 685],
	[9084, 4398, 673, 425, 685, 1561, 699, 1524, 711, 442, 686, 422, 684, 419, 707, 425, 706, 1561, 677, 1561, 681, 413, 714, 1558, 677, 441, 684, 422, 708, 420, 690, 415, 713, 417, 715, 415, 691, 1556, 702, 1534, 699, 420, 710, 419, 691, 413, 715, 415, 713, 419, 689, 1556, 699, 422, 686, 417, 708, 1564, 679, 1556, 702, 1556, 677, 1583, 655, 1557, 703],
	[9087, 4376, 694, 433, 697, 1542, 691, 1568, 666, 442, 688, 440, 695, 455, 648, 433, 697, 1567, 668, 1566, 692, 440, 666, 1590, 672, 433, 673, 432, 698, 433, 697, 433, 668, 437, 693, 1570, 668, 1554, 694, 1553, 694, 456, 672, 438, 666, 439, 693, 437, 695, 433, 671, 435, 695, 435, 697, 433, 670, 1566, 692, 1545, 683, 1575, 668, 1567, 695, 1541, 691],
	[9085, 4376, 668, 450, 678, 1569, 666, 1594, 646, 446, 682, 448, 682, 448, 658, 447, 682, 1590, 650, 1586, 665, 453, 660, 1587, 668, 473, 640, 441, 680, 452, 678, 455, 659, 441, 681, 450, 681, 449, 664, 441, 682, 1590, 653, 441, 677, 455, 676, 452, 664, 439, 683, 1591, 655, 1579, 677, 1558, 677, 442, 682, 1565, 679, 1581, 659, 1574, 681, 1555, 679]]

class Bluemote_Server(bluemote.Services):
	def __init__(self):
		bluemote.Services.__init__(self)
		self.version = ((self.component_codes.software, 0, 1, 0),)
		self.zero = None
		self.one = None
		self.header = None
		self.tailer = None

	def listen(self):
		self.server_sock = BluetoothSocket(RFCOMM)
		self.server_sock.bind(("", PORT_ANY))
		self.server_sock.listen(1)

		port = self.server_sock.getsockname()[1]

		print "Advertising \"%s\" service" % (self.service["name"])
		advertise_service(self.server_sock,
			self.service["name"],
			service_classes = [SERIAL_PORT_CLASS],
			profiles = [SERIAL_PORT_PROFILE],
			provider = self.service["provider"],
			description = self.service["description"])
						   
		print "Waiting for connection on RFCOMM channel %d" % port
		self.client_sock, self.client_info = self.server_sock.accept()
		print "Accepted connection from ", self.client_info

	def transport_tx(self, cmd, msg):
		full_msg = struct.pack("B", cmd)
		full_msg += msg
		self.client_sock.send(full_msg)
		self.last_msg = full_msg

	def get_command(self):
		full_msg = None

		full_msg = self.client_sock.recv(1024)

		cmd_code = struct.unpack("B", full_msg[0])[0]

		try:
			msg = struct.unpack_from(len(full_msg[1:]) * "B", full_msg, 1)
		except:
			msg = None

		# check for valid command code
		for cc in dir(self.cmd_codes):
			if getattr(self.cmd_codes, cc) == cmd_code:
				return (cc, msg) 
		return (None, None)

	def rename_device(self, msg):
		name = ""
		for i in msg:
			name += chr(i)
		print "Renaming to \"%s\"" % (name)
		self.service_name = name
		self.transport_tx(self.cmd_rc.ack, "")

	def learn(self, msg):
		(GET_PKT_1, GET_PKT_1_TAILER, GET_PKT_2) = range(3)
		state = GET_PKT_1
		return_msg = ""

		ir_scanner = ir()

		scan_codes = True
		try:
			while scan_codes:
				type, value = ir_scanner.get_input().split()
				# get the first pause between packets
				if state is GET_PKT_1:
					if type == "space" and int(value) > 20000:
						state = GET_PKT_1_TAILER
				# get the second pause between packets
				elif state is GET_PKT_1_TAILER:
					if type == "space" and int(value) > 20000:
						state = GET_PKT_2
				elif state is GET_PKT_2:
					if type == "space" and int(value) > 20000:
						scan_codes = False
					else:
						return_msg += struct.pack(">H", int(value))
		except KeyboardInterrupt:
			print
		finally:
			ir_scanner.process.terminate()

		self.transport_tx(self.cmd_rc.ack, return_msg)

	def get_version(self, msg):
		return_msg = ""

		for v in self.version:
			return_msg += struct.pack(len(v) * "B", *v)

		self.transport_tx(self.cmd_rc.ack, return_msg)

	def ir_transmit(self, msg):
		self.transport_tx(self.cmd_rc.ack, "")

	def debug(self, msg):
		try:
			rc = self.cmd_rc.ack
			return_msg = ""
			for i in sample_key_codes[msg[0]]:
				return_msg += struct.pack(">H", i)
		except:
			rc = self.cmd_rc.nak
			return_msg = ""
		self.transport_tx(rc, return_msg)

	def reset(self):
		self.client_sock.close()
		self.server_sock.close()

class ir():
	def __init__(self):
		try:
			self.process = subprocess.Popen("mode2", stdout = subprocess.PIPE)
			time.sleep(1)
			if self.process.poll():
				raise
		except:
			sys.stderr.write("Couldn't open \"mode2\"\n")
			exit(1)

	def get_input(self):
		input = ""

		while len(input) == 0:
			input = self.process.stdout.readline()
		return input

if __name__ == "__main__":
	if os.getuid() != 0:
		print "This script requires root priveleges. Re-run as root."
		exit(1)

	try:
		process = subprocess.Popen("lsmod", stdout = subprocess.PIPE)
		input = "dummy"
		lirc_module_loaded = False
		while len(input) != 0:
			input = process.stdout.readline()
			if input.find("lirc_serial") != -1:
				lirc_module_loaded = True
				break

		print "lirc_module_loaded:", lirc_module_loaded
		if lirc_module_loaded == False:
			os.system("setserial /dev/ttyS0 uart none")
			if os.system("modprobe lirc_serial") == 0:
				print "Loaded LIRC driver"
			else:
				print "Failed to load the LIRC driver"
				print "You won't be able to run IR related commands."
	except:
		print "error:", sys.exc_info()[0]
		print "Failed to setup the LIRC driver"
		print "You won't be able to run IR related commands."

	bm_pod = Bluemote_Server()

	try:
		while True:
			bm_pod.listen()
			try:
				while True:
					cmd_code, msg = bm_pod.get_command()
					if cmd_code != None:
						print "Received", cmd_code
						getattr(bm_pod, cmd_code)(msg)
					else:
						print "Invalid Command Code or duplicate packet found"
			except IOError:
				pass
			finally:
				bm_pod.reset()
				print "disconnected"
	except KeyboardInterrupt:
		print
	print "all done"
