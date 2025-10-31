(
var superColliderConn = NetAddr("127.0.0.1", 57120);

/*
syntax:  ids # sounds |path -> properties
Explenation: https://github.com/DavidZwitser/TideFlow

Fire with cmd or ctrl enter
*/

// This is just example code
var msg = "

1,2 # bd,hh | beat -> *, orbit
rm -> squiz
|everything ->*,orbit
2# |ch2 -> *

";

superColliderConn.sendMsg("/newTargets", *(msg.split($\n).reject(_=="")));
);