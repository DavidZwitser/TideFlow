/* The variable where the target dicts will be stored in */
~targets = [].asDict;


(// --- Configuration ---
/*
Put your cursor here and and press cmd or ctrl enter
*/

var targetIP = "127.0.0.1";
var targetPort = 57130;
// scListenPort = 57120  // default port SuperCollider listens on

// Client
var conn = NetAddr(targetIP, targetPort);

var bangSend = "*"; // The char send when requesting a bang
var logSends = false; // Send logs on what's happening
var warnIncompleteString = true; // Print warning
var warnPropUnavailable = true; // Print warning when a selected property is not available

/* Helper functions */
var ft  = {|str,size,fillChar=" "| str ++ (fillChar ! (size - str.size)).join}; // fill till - padd string on the right if the given size is bigger then the string
var rin = {|value,replace=""| value ?? {replace} }; // replace if nil
var pic = {|v c t f| if(c.(v), {t.(v)}, f)}; // (argument) passing if check
var pnnc = {|v t f| pic.(v, (_!=nil), t, f)}; // (argument) passing not nil check
var pia = {|v else=""| pnnc.(v, ("/"++_), {else})}; // path if available - add `/` before `v` if it's not nil
var noe = {|v| v == nil or: {v.isEmpty}}; // Nil or empty
var sltcs = {|sl endWithAnd=true|
	var newSize = sl.size*2-1;
	var res = [sl, ", "].lace(newSize);
	if (newSize > 2 && endWithAnd == true) {res.put(newSize-2, " and ")};
	res.join;
}; // string list to comma seperated

var parseLine = ({|str lineNmbr=nil|
	// add nil if array empty (I use this here so `allTuples` still return a tuple in cases where id or sound is nil
	var ani = {|a| if (a!=nil and: {a.isEmpty}) {[nil]} {a}};

	// \s is for line start and \e for line end
	// The order of the chars in `afterChars` and `beforeChars` determine match priority
	var cutStringInbetween = {|str afterChars beforeChars|
		var split = {|str split isStart|
			split.collect({|ch|
				if (ch == \s) {-1} { if (ch == \e) {str.size} {
					if (isStart == true) { str.detectLastIndex(_==ch) } { str.detectIndex(_==ch) }
				};
			};} ).reject(_==nil)[0];
		};

		var start = split.(str, afterChars, true);
		var end = split.(str, beforeChars, false);

		if ( ((start==nil) || (end==nil)) or: {(start max: 0) == (end min: str.size-1)} ) {nil} {
			var cut = str[start+1 .. end-1].reject(_==$ );
			if (cut.size > 0) {cut} {nil};
		}
	};

	var ids    = cutStringInbetween.(str, afterChars: [\s],     beforeChars: [$#]);
	var sounds = cutStringInbetween.(str, afterChars: [$#, \s], beforeChars: [$|, $-]);
	var path   = cutStringInbetween.(str, afterChars: [$|],     beforeChars: [$-,$>]); // $> is here for sneaky people who want to leave out the `-` from the `->`
	var props  = cutStringInbetween.(str, afterChars: [$>],     beforeChars: [\e]);

	var sac = {|s| s.split($,).collect(_.reject(_==($ ))) }; // split and clean
	#ids, sounds, path, props = [ids, sounds, path, props].collect({|v| if (v!=nil) {sac.(v)}});

	if (props == nil) {("No props readable from `" ++ str ++ "`").error; nil;}
	{
	/* return a dict for each combination of ids and sounds */
		[ids, sounds].collect(ani.(_)).allTuples.collect({|tup|
			var id=tup[0], sound=tup[1];
			[
				\id -> id,
				\sound -> sound,
				\path -> pnnc.(path, (_.at(0))),
				\props -> props,
				\origStr -> str,
				\lineNmbr -> lineNmbr
			].asDict;
		}).reject(_==nil);
	};
});

// When there's a custom path, use that, if there's a bang, send /id/sound, otherwise send /id/sound/prop
var generatePath = {|t prop|
	var pathEnd = pic.(prop, (_!="*"), ("/"++_), "");
	pnnc.(t[\path], ({|p|"/" ++ p ++ pathEnd}), {
		if ((t[\id] == nil) && (t[\sound] == nil) && prop=="*") { "/"++bangSend } {
			pia.(t[\id]) ++ pia.(t[\sound])
		} ++ pathEnd;
	});
};

// Return list of strings to print for the nice list view
var getPrintableList = {|targets sp|
	targets.collect({|t|
		t[\props].collect({|prop|

			var path = generatePath.(t, prop);
			var id = ft.(rin.(t[\id],"*"), sp[\id]);
			var sound = ft.(("" ++ rin.(t[\sound],"*")), sp[\sound]);
			var pathStr = ft.("" ++ rin.(path), sp[\path]);
			var propStr = "-> " ++ rin.(if (prop=="*") {bangSend} {prop});

			id ++ sound ++ pathStr ++ propStr;
		});
	}).flatten(1);
};

// Get longest version of every item so the spacing of the print list is nice (I know this is not very necausery)
var getSpacings = {|targets padding=2|
	var sizes = [\id->"id".size, \sound->"sound".size, \path->"path".size, \prop->"prop".size].asDict;

	var grow = {|s k| if (s.size>sizes[k]) {sizes[k]=s.size}};
	targets.do({|t|
		grow.(t[\id], \id);
		grow.(t[\sound], \sound);
		t[\props].do({|p|
			grow.(generatePath.(t,p), \path);
			grow.(p, \prop);
		});
	});
	sizes.collect(_+padding);
};

// Print out the list of things that will be send
var printWhatWillBeSend = {|targets|
	// print spacings
	var sp = getSpacings.(targets);

	var pretext = "SENDING: ";
	// Print top header
	var header = ((" "!pretext.size).join ++ ft.("id", sp[\id]) ++ ft.("sound", sp[\sound]) ++ ft.("path", sp[\path]) ++ ft.("prop", sp[\prop])).postln;
	// A nice header line
	var headerLine = ("-"!header.size).join.postln;
	// The target list
	var targetList = getPrintableList.(targets,sp);
	targetList.do({|l| (pretext ++ l).postln});
};


/* Send msg to target port */
var sendMsgs = {|targets id sound valPairs|
	targets.do({|target|

		var path = target[\path];
		var props = target[\props];

		// Does this target match the current message (either not specified (which matches all cases) or specified)
		if (((target[\id] == nil) || (target[\id].asSymbol == id)) && ((target[\sound] == nil) || (target[\sound].asSymbol == sound)))
		{
			var prntLog = {|str|
				("Sending: " ++ str).postln
			};

			var realProps = props.reject(_=="*").collect(_.asSymbol);

			// Fire off bang msgs
			props.select(_=="*").do({|v|
				var path = generatePath.(target,"*");
				conn.sendMsg(path, bangSend);
				if (logSends == true) {prntLog.(path ++ "->" ++ bangSend)};
			});

			// Fire of msgs for matching properties
			valPairs.select({|vp|realProps.includes(vp[0])}).do({|prop|
				var name = prop[0], val = prop[1];
				var path = generatePath.(target, name);
				conn.sendMsg(path, val);
				if (logSends == true) { prntLog.(path ++ "->" ++ val); }
			});

			// Warn about unavailable properties
			if (warnPropUnavailable == true) {
				var valNames = valPairs.collect(_[0]);
				var unusedProps = realProps.reject({|p| valNames.any(_==p.asSymbol) }).collect(_.asString);
				var origin = id ++ "/" ++ sound;
				var key = (origin + unusedProps.asString).asSymbol;

				if (unusedProps.size > 0 && haveWarnedAbout.any(_==key).not() ) {
					haveWarnedAbout.add(key);
					("`" ++ sltcs.(unusedProps) ++ "` not available in sound " ++ origin ++ " from " ++ target[\origStr]).warn;
					(origin ++ " has " ++ sltcs.(valNames) ++ " available").warn;
				};
			}
		};
	});
};

var haveWarnedAbout = List[];
/* Receive new strings */
OSCdef(\newTargets, { |msg|
	var lines = msg.drop(1);

    // Targets value overwritten
	~targets = lines
	.collect({|l i| parseLine.(l.asString, i) }) // Let's interpret the lines
	.reject(_==nil)
	.flat; // It pregenerates all possible combinations so lets flatten all the results together

	haveWarnedAbout = List[];

	printWhatWillBeSend.(~targets);
}, path: "/newTargets");

/* Send out to target */
OSCdef(\dirtPlay, { |msg, time, addr, recvPort|
	// Pair up values
	var nameValuePairs = msg.drop(1).clump(2);

	// Filter out id and sound values
	var id = nameValuePairs.detect({|v|v[0] == \_id_}).flat[1];
	var sound = nameValuePairs.detect({|v|v[0] == \s}).flat[1];

	sendMsgs.(~targets, id, sound, nameValuePairs);

}, path: "/dirt/play");

)

OSCdef(\dirtPlay).free;

// --- Debugging: print all incoming OSC messages ---
OSCFunc.trace(true);
OSCFunc.trace(false);


