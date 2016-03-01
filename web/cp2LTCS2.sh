#!/bin/sh

FLIST=" \
favicon.ico \
index.html \
laser.png \
libs \
logo.gif \
ltcs.css \
ltcs.html \
ltcs2.js \
menu.css \
scene.html \
scene.js \
scenes \
schedule.html \
schedule.js \
status.html \
status.js \
aim.html \
aim.js \
"

rsync -rlvu $FLIST ltcs2:ltcs2/web/
