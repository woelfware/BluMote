#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

'''Parser to convert lirc config files to BluMote config files.

Usage: lirc2bm.py <infile>
'''

import os.path
import sys

parser_ver = '0.1'

class lirc2raw():
	def __init__(self):
		'''Initialize the lirc2raw object with code parameters and buttons.'''
		self.remote_kw = {	# key words for costructing the button codes
			'name' : 'name',
			'bits' : 'bits',
			'header' : 'header',
			'one' : 'one',
			'zero' : 'zero',
			'ptrail' : 'ptrail',
			'gap' : 'gap'}
		self.code_kw = {	# keys that can be automatically assigned
			'0' : '0', 'zero' : '0', 'KEY_0' : '1',
			'1' : '1', 'one' : '1', 'KEY_1' : '1',
			'2' : '2', 'two' : '2', 'KEY_2' : '1',
			'3' : '3', 'three' : '3', 'KEY_3' : '1',
			'4' : '4', 'four' : '4', 'KEY_4' : '1',
			'5' : '5', 'five' : '5', 'KEY_5' : '1',
			'6' : '6', 'six' : '6', 'KEY_6' : '1',
			'7' : '7', 'seven' : '7', 'KEY_7' : '1',
			'8' : '8', 'eight' : '8', 'KEY_8' : '1',
			'9' : '9', 'nine' : '9', 'KEY_9' : '1',
			'PLAY' : 'PLAY',
			'PAUSE' : 'PAUSE',
			'STOP' : 'STOP',
			'FORWARD' : 'FORWARD',
			'REVERSE' : 'REVERSE',
			'SKIP+' : 'SKIP+',
			'SKIP-' : 'SKIP-',
			'MENU' : 'MENU',
			'UP' : 'UP', 'KEY_UP' : 'UP',
			'DOWN' : 'DOWN', 'KEY_DOWN' : 'DOWN',
			'LEFT' : 'LEFT', 'KEY_LEFT' : 'LEFT',
			'RIGHT' : 'RIGHT', 'KEY_RIGHT' : 'RIGHT'}
			

	def parse(self, fh):
		'''Parse an LIRC remote config file.'''
		remote = {'attributes' : {}, 'codes' : {}}
		begin_remote = False
		begin_codes = False

		for line in fh:
			if line.startswith('#'):
				continue

			words = line.split()
			if not words:
				continue

			if begin_codes:
				if ' '.join(words[:2]) == 'end codes':
					begin_codes = False
				elif words[0] in self.code_kw.keys():
					# automatically assign codes for known buttons
					try:
						remote['codes'][self.code_kw[words[0]]] = '0x{:X}'.format(int(words[1]))
					except ValueError:
						remote['codes'][self.code_kw[words[0]]] = '0x{:X}'.format(int(words[1], 16))
				else:
					# store codes in the database for buttons
					# that could be used in an activity
					try:
						remote['codes'][words[0]] = '0x{:X}'.format(int(words[1]))
					except ValueError:
						remote['codes'][words[0]] = '0x{:X}'.format(int(words[1], 16))
			elif begin_remote:
				if words[:2] == ['end', 'remote']:
					begin_remote = False
				elif ' '.join(words[:2]) == 'begin codes':
					begin_codes = True
				elif words[0] in self.remote_kw.keys():
					try:
						remote['attributes'][self.remote_kw[words[0]]] = [int(i) for i in words[1:]]
					except ValueError:
						remote['attributes'][self.remote_kw[words[0]]] = ' '.join(words[1:])
			elif ' '.join(words[:2]) == 'begin remote':
				begin_remote = True

		return remote

	def write_raw_config(self, remote, fin_name, fout_name):
		'''Write the BluMote raw config file.'''
		fout = open(fout_name, 'w')

		# file header
		fout.write('# This file was automatically generated with {} ver {}'
			' using {} from the LIRC project.\n'.format(os.path.basename(__file__),
				parser_ver, fin_name))

		for key, code in sorted(remote['codes'].items()):
			fout.write('{}\n'.format(key))
			code = int(code, 16)

			fout.write('\t{}\n'.format(remote['attributes']['header'][0]))
			fout.write('\t{}\n'.format(remote['attributes']['header'][1]))

			for bit in range(remote['attributes']['bits'][0]):
				if ((code >> (remote['attributes']['bits'][0] - bit - 1)) & 1):
					# '1'
					fout.write('\t{}\n'.format(remote['attributes']['one'][0]))
					fout.write('\t{}\n'.format(remote['attributes']['one'][1]))
				else:
					# '0'
					fout.write('\t{}\n'.format(remote['attributes']['zero'][0]))
					fout.write('\t{}\n'.format(remote['attributes']['zero'][1]))

			fout.write('\t{}\n'.format(remote['attributes']['ptrail'][0]))
			fout.write('\t{}\n'.format(remote['attributes']['gap'][0]))

		fout.close()

if __name__ == '__main__':
	if len(sys.argv) != 2:
		print(__doc__)
		sys.exit(1)

	parser = lirc2raw()
	with open(sys.argv[1]) as fin:
		remote = parser.parse(fin)
		fin_name = os.path.basename(sys.argv[1])
		parser.write_raw_config(remote, fin_name, fin_name + '.bmraw')
