TideFlow {
	var <sendIP, <sendPort;
	var <conn; // The connection object

	var <bangStr; // The char send when requesting a bang
	var <postSends; // Send logs on what's happening
	var <warnPropUnavailable; // Print warning when a selected property is not available
	var <listenOnOSCForExpr; // If TideFlow should listen for new expressions on port 55660
	var <listenPath; // The osc path TideFlow will listen for to receive new expressions

	var <dirtListenSymbol; // Symbol used in OSCdef for listening to superdirt
	var <exprReceiveSymbol; // Symbol used to receive new lines (if you want)

	var <>targets; // where all the target dicts will be stored in
	var <>haveWarnedAbout; // list of keys that warnings have been send about

	*new { arg
		sendIP= NetAddr.localIP, sendPort=55660,
		bangStr = "*", postSends=false, warnPropUnavailable=true,
		listenOnOSCForExpr=true, listenPath="/tideflow/new";

		^super.newCopyArgs(
			sendIP: sendIP,
			sendPort: sendPort,
			conn: NetAddr(sendIP, sendPort),

			bangStr: bangStr,
			postSends: postSends,
			warnPropUnavailable: warnPropUnavailable,
			listenOnOSCForExpr: listenOnOSCForExpr,
			listenPath: listenPath,

			// Generating unique symbol so you can have multiple TideFlows
			dirtListenSymbol: ("DirtListen"++this.identityHash.asString).asSymbol,
			exprReceiveSymbol: ("TideFlowListen"++this.identityHash.asString).asSymbol,

			targets: [].asDict,
			haveWarnedAbout: List[],
		)
	}

	/* Execute a string! Lines are sperated by \n */
	execExpression { |str printNewList=true|

		var prt = ["","Interpreting: "].do(_.postln);

		var dicts = str
		.split($\n) // split lines
		.reject(_=="") // strip out empty lines
		.collect({|l i| (i.asString ++ ": " ++ l).postln; this.parseLine( l, i )}) // parse lines
		.reject(_==nil) // reject faulty lines
		.flat; // flatten results (single lines can produce multiple dicts)

		this.targets = dicts;
		this.haveWarnedAbout = [];
		this.start; // restarting every new execution for now in case it wan't running
		if (printNewList) { this.printWhatWillBeSend; }

		^"TideFlowing 🌊"; // funny result message
	}

	start { this.listenAndSend; if (listenOnOSCForExpr == true) {this.listenForNewExpr} }
	stop  { this.stopListenAndSend; if (listenOnOSCForExpr == true) {this.stopListenForNewExpr} }

	/* Create the osc listener to Tidal Cycles that will send the messages */
	listenAndSend { | path = "/dirt/play" |
		^OSCdef(this.dirtListenSymbol, { |msg|
			// Pair up values
			var nameValuePairs = msg.drop(1).clump(2);

			// Filter out id and sound values
			var id = nameValuePairs.detect({|v|v[0] == \_id_}).flat[1];
			var sound = nameValuePairs.detect({|v|v[0] == \s}).flat[1];

			this.sendMsgs(id, sound, nameValuePairs);

		}, path: path);
	}
	stopListenAndSend { ^OSCdef(this.dirtListenSymbol).free; }

	/* Create the osc listener that will listen for new expressions */
	listenForNewExpr { arg path = this.listenPath;
	/* Receive new strings */
		^OSCdef(this.exprReceiveSymbol, { |msg|
			this.execExpression(msg[1].asString);
		}, path: path );
	}
	stopListenForNewExpr { ^OSCdef(this.exprReceiveSymbol).free; }


	/* Used for cutting out the parts of the strings
	\s is for line start and \e for line end
	The order of the chars in `afterChars` and `beforeChars` determine match priority
	 */
	cutStringInbetween {|str afterChars beforeChars|

		var split = {|str split isStart|
			split.collect({|ch|
				if (ch == \s) {-1} { if (ch == \e) {str.size} {
					if (isStart == true) { str.detectLastIndex(_==ch) } { str.detectIndex(_==ch) }
				};
			};} ).reject(_==nil)[0];
		};

		var start = split.(str, afterChars, true);
		var end = split.(str, beforeChars, false);

		/* var b is throwaway just so I can define vars after it again (I don't understand this language feature) */
		var b = if ( ((start==nil) || (end==nil)) or: {(start max: -1) == (end min: str.size-1)} ) {^nil};

		var cut = str[start+1 .. end-1].reject(_==$ );
		if (cut.size == 0) { ^nil };

		^cut;
	}

	/* Parse a TideFlow expression */
	parseLine {|str lineNmbr=nil|
		// add nil if array empty (I use this here so `allTuples` still return a tuple in cases where id or sound is nil
		var ani = {|a| if (a!=nil and: {a.isEmpty}) {[nil]} {a}};

		var ids    = this.cutStringInbetween(str, afterChars: [\s],     beforeChars: [$#]);
		var sounds = this.cutStringInbetween(str, afterChars: [$#, \s], beforeChars: [$|, $-, $>]);
		var path   = this.cutStringInbetween(str, afterChars: [$|],     beforeChars: [$-,$>]); // $> is here for sneaky people who want to leave out the `-` from the `->`
		var props  = this.cutStringInbetween(str, afterChars: [$>],     beforeChars: [\e]);

		var sac = {|s| s.split($,).collect(_.reject(_==($ ))) }; // split and clean
		#ids, sounds, path, props = [ids, sounds, path, props].collect({|v| if (v!=nil) {sac.(v)}});

		if (props == nil) { ("No props readable from `" ++ str ++ "`").error; ^nil; }

	    /* return a dict for each combination of ids and sounds */
		^[ids, sounds].collect(ani.(_)).allTuples.collect({|tup|
			var id=tup[0], sound=tup[1];
			[
				\id -> id,
				\sound -> sound,
				\path -> this.pnnc(path, (_.at(0))),
				\props -> props,
				\origStr -> str,
				\lineNmbr -> lineNmbr
			].asDict;
		}).reject(_==nil);
	}

	/* `passing if check` pass an argument to the true branch of an if statement */
	pic {|v c t f| ^(if(c.(v), {t.(v)}, f))}
	/* `passing not nil check ` pass the argument to the true branch if the value is not nil */
	pnnc {|v t f| ^(this.pic(v, (_!=nil), t, f))}
	/* Return the string with a `/` before it if it's not nil, else do else branch */
	pia {|v else=""| ^(this.pnnc(v, ("/"++_), {else}))}

	/* Construct msg path */
	generatePath {|t prop|
		// When a props available use it as `/{prop}` otherwise empty
		var pathEnd = this.pic(prop, (_!="*"), ("/"++_), "");

		// Custom path
		if (t[\path] != nil) { ^"/" ++ t[\path] ++ pathEnd };
		// The rare case of using `->*`
		if (t[\id] == nil && (t[\sound] == nil) && (prop=="*")) { ^"/" ++ this.bangStr ++ pathEnd };
		// Construct path normally
		^this.pia(t[\id]) ++ this.pia(t[\sound]) ++ pathEnd;
	}

	/* `fill till` Padd a string up with a fill character if the given size is bigger then the string */
	ft {|str,size,fillChar=" "| ^(str ++ (fillChar ! (size - str.size)).join) }
	/* replace if nil */
	rin {|value,replace=""| ^(value ?? {replace}) }

	/* Return list of strings to print for the nice list view. `sp` is a dict of spacings. */
	getPrintableList {|sp|
		^this.targets.collect({|t|
			t[\props].collect({|prop|

				var path = this.generatePath(t, prop);
				var id = this.ft(this.rin(t[\id],"*"), sp[\id]);
				var sound = this.ft(("" ++ this.rin(t[\sound],"*")), sp[\sound]);
				var pathStr = this.ft("" ++ this.rin(path), sp[\path]);
				var propStr = "-> " ++ this.rin(if (prop=="*") {this.bangStr} {prop});

				id ++ sound ++ pathStr ++ propStr;
			});
		}).flatten(1);
	}

	/* Get longest version of every item so the spacing of the print list is nice (using your valuable cpu power for this silly thing) */
	getSpacings {|padding=2|
		// Dict with default min sizes (length of the words used in the header)
		var sizes = [\id, \sound, \path, \prop].collect({|s| s-> s.asString.size }).asDict;

		var grow = {|str k| if (str.size>sizes[k]) {sizes[k]=str.size}};
		this.targets.do({|t|
			grow.(t[\id], \id);
			grow.(t[\sound], \sound);
			t[\props].do({|p|
				grow.(this.generatePath(t,p), \path);
				grow.(">"++p, \prop);
			});
		});

		^sizes.collect(_+padding);
	}

	/* Print out the list of things that will be send */
	printWhatWillBeSend  {
		/* Calculate the spacings based on the biggest msgs sizes */
		var sp = this.getSpacings;

		var pretext = "SENDING: ";
		// This could be more nifty if dicts ware ordered
		var names = [\id,\sound,\path,\prop].collect({|k| this.ft(k.asString, sp[k])}).join;



		// Print top header
		var header = ((" "!pretext.size).join ++ names);
		// A nice header line
		var headerLine = ("-"!header.size).join;
		// The target list
		var targetList = this.getPrintableList(sp);

		"".postln;
		"Got: ".postln;
		headerLine.postln;
		header.postln;
		headerLine.postln;
		targetList.do({|l| (pretext ++ l).postln});
		"".postln;
	}


	/* string list to comma seperated - so [orange, dragonfruit, lychee] to "orange, dragonfruit and lychee" */
	sltcs {|str_list endWithAnd=true|
		var newSize = str_list.size*2-1;
		var res = [str_list, ", "].lace(newSize);
		if (newSize > 2 && endWithAnd == true) {res.put(newSize-2, " and ")};
		^res.join;
	}

	/* Send a messasge given an individual prop (this is seperate from `sendMsg` because then I can use the nice `^` return) */
	sendMsgFromProp {|prop msgID msgSound target valPairs preText = "Sending: "|
		var prntLog = {|str|
			(preText ++ str).postln
		};

		// var b is throwaway so I can continue using vars in this function (agian what a weird language quirck)
		var b = if (prop == "*") {
			var path = this.generatePath(target,"*");
			conn.sendMsg(path, this.bangStr);
			if (this.postSends == true) { ^prntLog.(path ++ "->" ++ this.bangStr) };
			^"*";
		};

		var match = valPairs.detect({|vp| vp[0].asString == prop});
		// var p is throwaway again
		var p = if (match != nil) {
			var name = match[0], val = match[1];
			var path = this.generatePath(target, name.asString);
			conn.sendMsg(path, val);
			if (this.postSends == true) { ^prntLog.(path ++ "->" ++ val); }
			^name;
		};

		// e also throwaway
		var e = if (this.warnPropUnavailable == false) { ^(prop ++ " not available"); };

		// Just creating a key to put in the `haveWarnedAbout` array
		var origin = msgID ++ "/" ++ msgSound;
		var key = (origin + prop.asString).asSymbol;

		if (this.haveWarnedAbout.any(_==key).not() ) {
			var not_available = ("`" ++ prop ++ "` not available in sound " ++ origin ++ " from " ++ target[\origStr]).warn;
			var available = (origin ++ " has " ++ this.sltcs(valPairs.collect(_[0])) ++ " available").warn;
			"".postln;
			this.haveWarnedAbout = this.haveWarnedAbout.add(key);
			^not_available;
		};

		^"prop not available"
	}

	/* Send msg to target port */
	sendMsgs {|id sound valPairs|
		this.targets.do({|target|

			// Does this target match the current message (either not specified (which matches all cases) or specified)
			var isMatch = {
				var matchID = ((target[\id] == nil) or: {target[\id].asSymbol == id});
				var matchSound = ((target[\sound] == nil) or: {target[\sound].asSymbol == sound});
				matchID && matchSound;
			}.value;

			if (isMatch == true) {
				target[\props].do({|prop| this.sendMsgFromProp(prop, id, sound, target, valPairs) });
			};
		});
	}
}
