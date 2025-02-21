package com.carlca
package twisterella

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorDevice;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.Device;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.DocumentState;
import com.bitwig.extension.controller.api.HardwareSurface;
import com.bitwig.extension.controller.api.MidiIn;
import com.bitwig.extension.controller.api.MidiOut;
import com.bitwig.extension.controller.api.Parameter;
import com.bitwig.extension.controller.api.PinnableCursor;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.Preferences;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.Send;
import com.bitwig.extension.controller.api.SendBank;
import com.bitwig.extension.controller.api.SettableBooleanValue;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.SettableEnumValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.carlca.config.Config
import com.carlca.logger.Log
import com.carlca.twisterella.TwisterellaSettings

class TwisterellaExtension(definition: TwisterellaExtensionDefinition, host: ControllerHost)
    extends ControllerExtension(definition, host):
  private val APP_NAME                    = "com.carlca.Twisterella"
  private var last_data1: Int = 0
  val hardwareSurface: HardwareSurface = host.createHardwareSurface()
  val midiIn: MidiIn = host.getMidiInPort(0)
  val midiOut: MidiOut = host.getMidiOutPort(0)

  override def init: Unit =
    val host = getHost
    // MidiMixLights.init(host)
    Config.init(APP_NAME)
    TwisterellaSettings.init(host)
    // Tracks.init(host)
    initEvents(host)
  override def exit: Unit = None

  override def flush: Unit =
    last_data1 = -1
    // Log.send("flush")
  end flush

  private def initEvents(host: ControllerHost): Unit =
    initOnMidiCallback(host)
    initOnSysexCallback(host)
  end initEvents

  private def initOnMidiCallback(host: ControllerHost): Unit =
    midiIn.setMidiCallback((a, b, c) => onMidi0(ShortMidiMessage(a, b, c)))

  private def initOnSysexCallback(host: ControllerHost): Unit =
    midiIn.setSysexCallback(onSysex0(_))

  private def onMidi0(msg: ShortMidiMessage): Unit =
    // var status = msg.status()
    // var channel = msg.channel()
    // var data1 = msg.getData1()
    // // var data2 = msg.data2()
    // if last_data1 != data1 then
    //   last_data1 = data1
    Log.send(msg.toString())
    // MidiProcessor.process(msg)
  end onMidi0

  @FunctionalInterface
  private def onSysex0(data: String): Unit = ()

end TwisterellaExtension
