#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

'''Parser to convert BluMote config files to raw IR pulses/spaces.

Usage: bm2raw.py <infile>
'''

import os.path
import sys

parser_ver = '0.1'

def parse_bm(fh):
	'''Parse an LIRC remote config file.'''
	remote = {'attributes' : {}, 'codes' : {}}
	begin_remote = False
	begin_codes = False
	version = None	# not used at the moment, but for when the api stabilizes

	for line in fh:
		if line.startswith('#'):
			if not version:
				comment = line.split()
				if comment[1:-1] == ['BluMote', 'config', 'spec', 'version']:
					version = comment[-1]
			else:
				continue

		words = line.split()
		if len(words) == 0:
			continue

		if begin_codes:
			if words[0] == 'EndSection':
				begin_codes = False
			else:
				try:
					remote['codes'][words[0]] = '0x{:X}'.format(int(words[1]))
				except ValueError:
					remote['codes'][words[0]] = '0x{:X}'.format(int(words[1], 16))
		elif begin_remote:
			if words[0] == 'EndSection':
				begin_remote = False
			elif words[:2] == ['Section', 'Codes']:
				begin_codes = True
			else:
				try:
					remote['attributes'][words[0]] = [int(i) for i in words[1:]]
				except ValueError:
					remote['attributes'][words[0]] = ' '.join(words[1:])
		elif words[:2] == ['Section', 'Attributes']:
			begin_remote = True

	return remote

def write_bm_config(name, remote):
	'''Write the raw IR pulse/space codes.'''
	fname = os.path.splitext(name)[0] + '.bmconfigraw'
	print('Writing BluMote config file', fname)
	fout = open(fname, 'w')

	# file header
	fout.write('# This file was automatically generated with {}'
		' using {}.\n\n'.format(os.path.basename(__file__), name))

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
	if len(sys.argv) < 2:
		print(__doc__)
		exit(1)

	fin = open(sys.argv[1], 'r')

	print('Parsing BluMote config file {}'.format(sys.argv[1]))
	remote = parse_bm(fin)
	fin.close()
	print('Finished parsing {}'.format(os.path.basename(sys.argv[1])))

	write_bm_config(os.path.basename(sys.argv[1]), remote)
