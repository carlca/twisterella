package com.carlca
package twisterella

import com.bitwig.extension.api.util.midi.ShortMidiMessage
import com.bitwig.extension.controller.ControllerExtension
import com.bitwig.extension.controller.api.*
import com.carlca.config.Config
import com.carlca.logger.Log
import com.carlca.twisterella.TwisterellaSettings

class TwisterellaExtension(definition: TwisterellaExtensionDefinition, host: ControllerHost)
    extends ControllerExtension(definition, host):
  private val APP_NAME                    = "com.carlca.Twisterella"
  private var last_data1: Int = 0

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
    host.getMidiInPort(0).setMidiCallback((a, b, c) => onMidi0(ShortMidiMessage(a, b, c)))

  private def initOnSysexCallback(host: ControllerHost): Unit =
    host.getMidiInPort(0).setSysexCallback(onSysex0(_))

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
