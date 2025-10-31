# TideFlow
### A SuperCollider script that gives you a small expressive notation for specifying what messages from Tidal Cycles to send over OSC


## Syntax
`ids # sounds |path -> properties`

#### Explanation
`ids`: Filter messages on ids (`d1`, `d2`, `d3` or `p 1`, `p 2` or `p "id"`).\
`sounds`: Filter messages on the sample or instrument used (`bd`, `808bd`, `supervibe`).\
`path`: Use this to change the OSC path that's send. `/{property}` will be added after the path except for bangs. \
`properties`: The names of the values from Tidal Cycles you want to send. When you write a `*` a `bang` will be send. By default that will just send a `"*"` char, but you can change it.


`ids, sounds, properties` can have multiple entries separated by commas `hi, bye` (spaces are ignored)\
`ids, sounds, rename` are optional and can be left out.
- Leaving out `path` will construct a path with `/id/sound/property` where available.
- Leaving out `ids` or `sounds` will match any id or sound. You can even math everything if you leave them both out! Having just `->*` will send a bang on every event happening!
- When no `#` is written, the text before `|` or `->` will be interpreted as a sound. To only write an `id` follow it by a `#`. So this is an id `id# ->*` and this a sound `sound ->*`.
- You must have at least one `prop` with `->` before it (secretly just `>` but `->` looks more readable). That's the basis.

So the full list possible combinations is: \
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
#### Configuring
On the top of the `TideFlow.sc` script are all the settings. Here you can change what ip or port the messages are sent to. The default is `ip: 127.0.0.1` and `port: 57130`.
#### Starting
Open both the `TideFlow.sc` and `SendScript.sc` files in SuperCollider. Run `TideFlow.sc` once and then specify in the `msg` variable in `SendScript.sc` what you want to send. Run to send.

#### Reading
You'll get a nice message in the Post Window showing what your expression will result to sending like this:
```
         id  sound  path         prop
----------------------------------------
SENDING: 1   bd     /beat        -> *
SENDING: 1   bd     /beat/orbit  -> orbit
SENDING: 1   hh     /beat        -> *
SENDING: 1   hh     /beat/orbit  -> orbit
SENDING: 2   bd     /beat        -> *
SENDING: 2   bd     /beat/orbit  -> orbit
SENDING: 2   hh     /beat        -> *
SENDING: 2   hh     /beat/orbit  -> orbit
SENDING: *   rm     /rm/squiz    -> squiz
SENDING: *   *      /*           -> *
SENDING: *   *      /orbit       -> orbit
SENDING: 2   *      /ch2         -> *
SENDING: *   rm     /rm/squiz    -> squiz
```


#### Error
If your expression doesn't have at least the basis `>*` you'll get an error like this:\
```ERROR: No props readable from `2# |ch2 -> ` ```\
Everything else will be interpreted following the pattern. Spaces in names will be removed.

#### Options
When you have specified what you want to send, but it's not available, the script will warn you about it once and give you the options that áre available like so:
```
WARNING: `banana` not available in sound 1/bd from 1,2 # bd -> *, orbit, banana
WARNING: 1/bd has _id_, cps, cycle, delta, orbit and s available
```


#### Sending
You can also send these lines to SuperCollider's default port `57120` from another program. To do this use the OSC path `/newTargets` and send each line as an individual argument to the OSC message. Maybe you can even send them from Tidal Cycles?? Would be awesome.


## Why
I think it can be really cool to send the rich event information on the fly to some visual coding software! Or possibly another audio thing! Or whatever! Anything is possible!


I wanted to link Tidal Cycles to Touchdesigner. I saw [this video](https://www.youtube.com/watch?v=iB3qsa4e2XQ) and thought, wow there's so much valuable data about what you're hearing going through your system. Then I started to code in SC and loved the language, and I love making tools. So yea.


Feel free to give any tips on the code or request features or bug reports. \
This is my first SC project so I might be falling into some bad patterns. \
And this is my first time trying to make a small language (which was super cool to do). \
Also I haven't spent enough time using it but just wanted to get this out there.
