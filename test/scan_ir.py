#!/usr/bin/env python
# Copyright (C) 2011 Woelfware

import sys
import subprocess
import time

class ir():
	def __init__(self):
		try:
			self.process = subprocess.Popen("mode2", stdout = subprocess.PIPE)
		except:
			sys.stderr.write("Couldn't open \"mode2\"\n")
			exit(1)
		time.sleep(1)
		if self.process.poll():
			exit(1)

	def get_input(self):
		input = ""

		while len(input) == 0:
			input = self.process.stdout.readline()
		return input
		
if __name__ == "__main__":
	(GET_HEADER_1, GET_HEADER_2, GET_TAIL) = range(3)
	state = GET_HEADER_1
	key_code = {"old": "", "new": ""}

	ir_scanner = ir()

	try:
		while True:
			type, value = ir_scanner.get_input().split()
			if state is GET_HEADER_1:
				if type == "pulse" and int(value) > 8000:
					state = GET_HEADER_2
			elif state is GET_HEADER_2:
				if type == "space" and int(value) > 3500:
					state = GET_TAIL
				else:
					state = GET_HEADER_1
			elif state is GET_TAIL:
				if type == "space" and int(value) > 20000:
					if key_code["new"] != key_code["old"]:
						print "Key code: 0x%08X" % int(key_code["new"][::-1], 2)
						key_code["old"] = key_code["new"]
					key_code["new"] = ""
					state = GET_HEADER_1
				else:
					if type == "space":
						if int(value) < 1000:
							key_code["new"] += "1"
						else:
							key_code["new"] += "0"
			else:
				sys.stderr.write("bad state: " + str(state) + "\n")
				ir_scanner.process.terminate()
				exit(1)

	except KeyboardInterrupt:
		print
	finally:
		ir_scanner.process.terminate()
