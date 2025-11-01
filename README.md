# TideFlow
### A SuperCollider quark that gives you a small expressive notation for specifying what messages to send from Tidal Cycles over OSC


## Syntax
`ids # sounds |path -> properties`

#### Parts
`ids`: Filter messages on ids produced by `d1`, `d2`, `d3` or `p 1`, `p 2` or `p "id"` etc.\
`sounds`: Filter messages on the sample or instrument used like `bd`, `808bd`, `supervibe` etc.\
`path`: Use this to change the OSC path that's send. `/{property}` will be added after the path except when sending bangs. \
`properties`: The names of the values from Tidal Cycles you want to send. When you write a `*` a `bang` will be send to capture something just playing. By default that will just send a `"*"` char, but you can change it.

#### Rules
`ids, sounds, properties` can have multiple entries separated by commas `hi, bye` (spaces are ignored).\
`ids, sounds, path` are optional and can be left out.
- Leaving out `path` will construct a path with `/id/sound/property` based on those properties availabilities.
- Leaving out `ids` or `sounds` will match any id or sound. You can even match everything if you leave them both out! Having just `->*` will send a bang on every event happening!
- When no `#` is written, the text before `|` or `->` will be interpreted as a sound. To only write an `id` follow it by a `#`. So this filters ids `id# ->*`, this filters sounds `sound ->*` and this gives a path `|path ->*`.
- You must have at least one `prop` with `->` before it (secretly just `>` but `->` looks more readable). That's the basis.

#### Full list of possibilities
`ids # sounds |path -> props`\
`sounds |path -> props`\
`ids # |path -> props`\
`ids # sounds -> props`\
`sounds -> props`\
`ids # -> props`\
`|path -> props`\
`-> props`

## Examples
Some of these are very impractical but here for demonstrational purposes.
```
1: 1,2 # bd,hh | beat -> *, orbit
2: rm -> squiz
3: |everything -> *,orbit
4: 2# |ch2 -> *
```


## Usage

#### Starting
Open the `TideGo.scd` file in supercollider and run line two (shift+enter). Underneath in parenthesies will be some example code. You can run this with ctr/cmd+enter.

#### Configuring
The `TideFlow.new` method can take a whole bunch of args. They're listed here:
|name|default value|explenation|
|----|-------------|-----------|
|sendIP|NetAddr.localIP|The ip that TF will send messages to.|
|sendPort|55660|The port that TF will send messages to.|
|bangStr|"*"|The value send when requesting a bang.|
|postSends|false|Post a message in the Post window on messages send.|
|warnPropUnavailable|true|Send a warning (and alternative suggestion) when a prop is not available.|
|listenOnOSCForExpr|true|Toggle if TF should listen for msgs coming in over OSC.|
|listenPath|"/tideflow/new"|The OSC path TF will listen to for receiving messages over OSC.|

#### Reading
You'll get a nice message in the Post Window showing what your expression will result to sending like this:
```
Interpreting:
0: 1,2 # 808bd,hh | beat -> *, room
1: rm -> squiz
2: |everything -> *,orbit
3: 2# |ch2 -> n

Got:
-----------------------------------------------
         id  sound  path               prop
-----------------------------------------------
SENDING: 1   808bd  /beat              -> *
SENDING: 1   808bd  /beat/room         -> room
SENDING: 1   hh     /beat              -> *
SENDING: 1   hh     /beat/room         -> room
SENDING: 2   808bd  /beat              -> *
SENDING: 2   808bd  /beat/room         -> room
SENDING: 2   hh     /beat              -> *
SENDING: 2   hh     /beat/room         -> room
SENDING: *   rm     /rm/squiz          -> squiz
SENDING: *   *      /everything        -> *
SENDING: *   *      /everything/orbit  -> orbit
SENDING: 2   *      /ch2/n             -> n
```


#### Error
If your expression doesn't have at least the basis `>*` you'll get an error like this:\
```
Interpreting:
0: 1,2 # 808bd,hh | beat -> *, room
1: rm squiz
ERROR: No props readable from `rm squiz`
2: |everything -> *,orbit
3: 2# |ch2 ->
ERROR: No props readable from `2# |ch2 -> `
```
Everything else will be interpreted following the pattern. Spaces in names will be removed.

#### Options
When you have specified what you want to send, but it's not available, the script will warn you about it once and give you the options that are available like so:
```
WARNING: `banana` not available in sound 1/bd from 1,2 # 808bd -> *, room, banana
WARNING: 1/bd has _id_, cps, cycle, delta, orbit and s available
```


#### Sending
You can also send these lines to SuperCollider's default port `57120` from another program. To do this use the OSC path `/tideflow/new` and send the whole string as one argument to the OSC message (it'll be split on `\n`). Maybe you can even send them from Tidal Cycles?? Would be awesome.


## Why
I think it can be really cool to send the rich event information on the fly to some visual coding software! Or possibly another audio thing! Or whatever! Anything is possible!


I wanted to link Tidal Cycles to Touchdesigner. I saw [this video](https://www.youtube.com/watch?v=iB3qsa4e2XQ) and thought, wow there's so much valuable data about what you're hearing going through your system. Then I started to code in SC and loved the language, and I love making tools. So yea.


Feel free to give any tips on the code or request features or bug reports. \
This is my first SC project so I might be falling into some bad patterns. \
And this is my first time trying to make a small language (which was super cool to do). \
Also I haven't spent enough time using it but just wanted to get this out there.
