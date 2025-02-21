package com.carlca.twisterella.twister

import com.bitwig.extension.api.util.midi.ShortMidiMessage

/** Contains basic Twister hardware MIDI information. */
class MidiInfo(val channel: Int, val cc: Int):
  val statusByte: Int = ShortMidiMessage.CONTROL_CHANGE + channel
