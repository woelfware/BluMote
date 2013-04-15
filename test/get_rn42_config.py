#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

import bm_fw_up

if __name__ == '__main__':
	blumotes = bm_fw_up.find_blumotes()
	addr = bm_fw_up.get_target_addr(blumotes, False)

	if addr is None:
		print 'Cancelled... good bye.'
		exit()

	with bm_fw_up.BluMote() as bm:
		bm.connect((addr, 1))

		print 'Entering command mode:', bm.enter_cmd_mode()

		print 'Display basic settings:'
		bm.send('D\r\n')
		for i in xrange(11):
			print bm.recv(128)

		print 'Setting the Baudrate to 115k:'
		print bm.set_baud_115k()

		print 'Display basic settings:'
		bm.send('D\r\n')
		for i in xrange(11):
			print bm.recv(128)

		print 'Display extended settings:'
		bm.send('E\r\n')
		for i in xrange(7):
			print bm.recv(128)

		print 'Display remote side modem signal status:'
		bm.send('M\r\n')
		for i in xrange(2):
			print bm.recv(128)

		print 'Display other settings:'
		bm.send('O\r\n')
		for i in xrange(10):
			print bm.recv(128)

