#/usr/bin/env bash

if [[ $EUID -ne 0 ]]; then
	echo "This script must be run as root" 1>&2
	exit 1
fi

setserial /dev/ttyS0 uart none
modprobe lirc_serial
echo mode2 can now be run
