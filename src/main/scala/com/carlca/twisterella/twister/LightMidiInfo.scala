package com.carlca.twisterella.twister

import com.carlca.twisterella.twister.MidiInfo

/** Contains MIDI information for a Twister light. */
class LightMidiInfo(val light: MidiInfo, val animation: MidiInfo):
  def this(channel: Int, animationChannel: Int, cc: Int) =
    this(MidiInfo(channel, cc), MidiInfo(animationChannel, cc))
