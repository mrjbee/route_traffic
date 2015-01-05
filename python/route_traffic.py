__author__ = 'mrjbee'
import curses, curses.panel
from threading import Thread
import socket
import re
import datetime

version = "v0.1"
user_hit = -1

received_value = "NaN"
sent_value = "NaN"
last_updated = None


def is_valid_message(text):
    return re.match('\[.*app==route_traffic.*\]', text)


def extract_values(text):
    answer = {}
    for param in text[1:-1].split(";"):
       keyValue = param.split("==")
       if keyValue[0] in ("in", "out"):
           answer[keyValue[0]]=int(keyValue[1])
    return answer


def convert_bytes_to_human_look(n):
    symbols = ('K', 'M', 'G', 'T', 'P', 'E', 'Z', 'Y')
    prefix = {}
    for i, s in enumerate(symbols):
        prefix[s] = 1 << (i+1)*10
    for s in reversed(symbols):
        if n >= prefix[s]:
            value = float(n) / prefix[s]
            return '%.1f%s' % (value, s)
    return "%sb" % n

# Create a UDP socket
sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
server_address = ('0.0.0.0', 12399)
sock.bind(server_address)

def read_from_socket():
    global user_hit, received_value, sent_value, last_updated, sock, server_address
    while sock is not None:
        data, address = sock.recvfrom(4096)
        if is_valid_message(data):
            in_out_map = extract_values(data)
            received_value = convert_bytes_to_human_look(in_out_map["in"])
            sent_value = convert_bytes_to_human_look(in_out_map["out"])
            last_updated = datetime.datetime.now().strftime("at %I:%M.%S %p on %B %d, %Y")

sockThread = Thread(target=read_from_socket)
sockThread.start()

try:
    my_screen = curses.initscr()
    while user_hit is not 27:
        my_screen.clear()
        my_screen.border(0)
        my_screen.addstr(1, 2, "Route Traffic Receiver "+version, curses.A_BOLD)
        my_screen.addstr(2, 3, "-- make sure that 'Route Traffic' an Android App is running daemon")

        my_screen.addstr(5, 1, " Today's WAN Traffic", curses.A_STANDOUT)
        my_screen.addstr(7, 3, "Received")
        my_screen.addstr(7, 14, received_value, curses.A_BOLD)
        my_screen.addstr(8, 3, "Sent")
        my_screen.addstr(8, 14, sent_value, curses.A_BOLD)

        if last_updated is not None:
            my_screen.addstr(10, 3, "Last updated", curses.A_BOLD)
            my_screen.addstr(10, 16, last_updated)

        my_screen.addstr(13, 1, "Press 'Ctrl+Alt+Enter' for exit")

        my_screen.refresh()
        my_screen.timeout(1000)
        user_hit = my_screen.getch()

finally:
    my_screen.keypad(0)
    curses.echo()
    curses.nocbreak()
    curses.endwin()
    tmp_sock = sock
    sock = None
    tmp_sock.close()