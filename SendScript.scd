(
var superColliderConn = NetAddr("127.0.0.1", 57120);

/*
syntax:  ids # sounds |path -> properties

ids: Filter messages on ids (d1, d2, d3 or `p 1`, `p 2` or `p "id"`)
sounds: Filter messages on the sample or instrument used (`bd`, `808bd`, `supervibe`)
path: Use this to change the osc path that's send (normally constructed with `/id/sound/property`)
properties: The values that are send (if you specify a value that's not availble the post window will hint to you what can be send)

[ids, sounds, properties] can have multiple entries seperated by comma's `hi,bye` or `hi, bye`
[ids, path] are optional and can be left out
*/

var msg = "

1,2 # bd -> *, orbit, banana
rm -> squiz

";


superColliderConn.sendMsg("/newTargets", *(msg.split($\n).reject(_=="")));
);