#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

'''Parser to convert lirc config files to BluMote config files.

Usage: lirc2bm.py <infile>
'''

import os.path
import sys

parser_ver = '0.1'

# used for compatibility with the below keywords
config_spec_ver = '0.1'

# key words BluMote uses in the "remote" section
# the key is the value found in the LIRC config
# the value is what will be used in place of the key in the BluMote config
remote_kw = {'name' : 'name',
	'bits' : 'bits',
	'header' : 'header',
	'one' : 'one',
	'zero' : 'zero',
	'ptrail' : 'ptrail',
	'gap' : 'gap'}

# key codes BluMote uses in the "codes" section
# the key is the value found in the LIRC config
# the value is what will be used in place of the key in the BluMote config
code_kw = {'0' : '0', 'zero' : '0', 'KEY_0' : '1',
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

def parse_lirc(fh):
	'''Parse an LIRC remote config file.'''
	remotes = []
	begin_remote = False
	begin_codes = False
	unused_attributes = []
	unused_codes = []

	for line in fh:
		if line.startswith('#'):
			continue

		words = line.split()
		if len(words) == 0:
			continue

		if begin_codes:
			if words[:2] == ['end', 'codes']:
				begin_codes = False
			elif words[0] in code_kw.keys():
				try:
					codes[code_kw[words[0]]] = '0x{:X}'.format(int(words[1]))
				except ValueError:
					codes[code_kw[words[0]]] = '0x{:X}'.format(int(words[1], 16))
			else:
				unused_codes.append('{}\t{}'.format(*words[:2]))
		elif begin_remote:
			if words[:2] == ['end', 'remote']:
				begin_remote = False
				if unused_attributes:
					print('Not using attributes:')
					for attribute in unused_attributes:
						print('\t{}'.format(attribute))
					unused_attributes = []
				if unused_codes:
					print('Not using codes:')
					for code in unused_codes:
						print('\t{}'.format(code))
					unused_codes = []
			elif words[:2] == ['begin', 'codes']:
				begin_codes = True
			elif words[0] in remote_kw.keys():
				try:
					attributes[remote_kw[words[0]]] = [int(i) for i in words[1:]]
				except ValueError:
					attributes[remote_kw[words[0]]] = ' '.join(words[1:])
			else:
				unused_attributes.append('\t'.join(words))
		elif words[:2] == ['begin', 'remote']:
			begin_remote = True
			remotes.append({})
			print('Found remote {}'.format(len(remotes)))
			remote = remotes[-1]
			attributes = remote['attributes'] = {}
			codes = remote['codes'] = {}

	return remotes

def write_bm_config(name, remotes):
	'''Write the BluMote config files for each remote.'''
	for remote in remotes:
		# strip out the lircd and config from the file name
		fname = name.split('.')
		for i in ('lircd', 'config'):
			try:
				fname.remove(i)
			except:
				pass
		if len(remotes) > 1:
			fname = '{}.{}.{}'.format(''.join(fname),
					remote['attributes'].get('name', ''),
					'bmconfig')
		else:
			fname = '{}.{}'.format(''.join(fname),
					'bmconfig')
		print('Writing BluMote config file', fname)
		fout = open(fname, 'w')

		# file header
		fout.write('# This file was automatically generated with {}'
			' using {} from the LIRC project.\n'.format(os.path.basename(__file__), name))
		fout.write('# BluMote config spec version {}\n\n'.format(config_spec_ver))

		# attributes section
		fout.write('Section Attributes\n')
		remote_name = remote['attributes'].pop('name', '')
		if remote_name:
			fout.write('\t{}\t{}\n'.format('name', remote_name))
		for attr in sorted(remote['attributes']):
			vals = [str(i) for i in remote['attributes'][attr]]
			fout.write('\t{}\t{}\n'.format(attr, '\t'.join(vals)))

		# codes section
		fout.write('\tSection Codes\n')
		for key, code in sorted(remote['codes'].items()):
			fout.write('\t\t{}\t{}\n'.format(key, code))
		fout.write('\tEndSection\n')
		fout.write('EndSection\n')

		fout.close()

if __name__ == '__main__':
	if len(sys.argv) < 2:
		print(__doc__)
		exit(1)

	fin = open(sys.argv[1], 'r')

	print('Parsing LIRC config file {}'.format(sys.argv[1]))
	remotes = parse_lirc(fin)
	fin.close()
	print('Finished parsing {}'.format(os.path.basename(sys.argv[1])))

	write_bm_config(os.path.basename(sys.argv[1]), remotes)
