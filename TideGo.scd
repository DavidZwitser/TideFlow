/* Run this line to install the TideFlow quark */
Quarks.checkForUpdates({Quarks.install("https://github.com/DavidZwitser/TideFlow.git", "v1.0.0"); thisProcess.recompile()})

/*
This is how you write expressions.
You can also send expressions to SuperColliders default port 57120 over OSC using the path `/tideflow/new`.
*/
(
~tf = TideFlow.new(postSends:false, sendPort:55660, warnPropUnavailable: true).start;
~tf.execExpression("

1,2 # 808bd,hh | beat -> *, room
rm -> squiz
|everything -> *,orbit
2# |ch2 -> n

");)

~tf.stop;
