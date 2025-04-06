package com.carlca.twisterella

// import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.api.MidiOut
import javax.sound.midi.MidiMessage
import javax.sound.midi.ShortMessage
import javax.sound.midi.SysexMessage
import com.carlca.twisterella.TwisterMidiHelper.MidiOutput // Import the MidiOutput trait

class BitwigMidiOutput(bitwigMidiOut: MidiOut) extends MidiOutput:

  override def sendMidi(message: MidiMessage): Unit =
    message match
      case sysexMessage: SysexMessage =>
        bitwigMidiOut.sendSysex(sysexMessage.getData)
      case shortMessage: ShortMessage =>
        bitwigMidiOut.sendMidi(
          shortMessage.getStatus & 0xFF,
          shortMessage.getData1 & 0xFF,
          shortMessage.getData2 & 0xFF
        )

  override def sendMidi(status: Int, data1: Int, data2: Int): Unit =
    bitwigMidiOut.sendMidi(status, data1, data2)
