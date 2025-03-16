package com.carlca.twisterella.twister

import com.bitwig.extension.api.Color
import com.bitwig.extension.controller.api.HardwareLightVisualState
import com.bitwig.extension.controller.api.InternalHardwareLightState
import com.bitwig.extension.controller.api.MidiOut
import com.bitwig.extension.controller.api.MultiStateHardwareLight
import com.carlca.twisterella.TwisterellaExtension
import com.carlca.twisterella.twister.TwisterLight.AnimationState

import java.util.function.Consumer
import java.util.function.Supplier

/** The RGB light on a twister knob. */
class TwisterRGBLight(
    extension: TwisterellaExtension,
    lightMidiInfo: LightMidiInfo,
    color: Color
) extends TwisterLight(
      extension,
      lightMidiInfo.animation,
      TwisterRGBLight.ANIMATION_START_VALUE,
      TwisterRGBLight.BRIGHTNESS_START_VALUE
    ):

  def this(extension: TwisterellaExtension, lightMidiInfo: LightMidiInfo) =
    this(extension, lightMidiInfo, Color.nullColor())

  val midiInfo: MidiInfo = lightMidiInfo.light

  private val light: MultiStateHardwareLight =
    extension.hardwareSurface.createMultiStateHardwareLight(
      s"RGB Light ${midiInfo.cc}"
    )

  light.setColorToStateFunction((col: Color) => new LightState(col))
  light.state().onUpdateHardware(new LightStateSender(midiOut, midiInfo))
  light.setColor(color)

  def setColor(color: Color): Unit =
    light.setColor(color)

  def setRawValue(value: Int): Unit =
    light.setColor(TwisterColors.ALL(value))

  def setColorSupplier(colorSupplier: Supplier[Color]): Unit =
    light.setColorSupplier(colorSupplier)

  override def lightOff(): Unit =
    setAnimationState(AnimationState.OFF)

    light.setColor(Color.blackColor())

    // Force MIDI to be sent immediately
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, 0)

  /** Handler of internal light state. */
  private class LightState(color: Color) extends InternalHardwareLightState:
    private var colorIndex: Int = 0
    private var _color: Color = Color.nullColor()
    colorToState(color)

    override def getVisualState(): HardwareLightVisualState =
      HardwareLightVisualState.createForColor(_color)

    override def equals(obj: Any): Boolean =
      obj match
        case other: LightState => colorIndex == other.getColorIndex()
        case _                 => false

    private def colorToState(color: Color): Unit =
      // Find if an exact match exists and use this MIDI value if it does
      val existingIndex = TwisterColors.ALL.indexOf(color)

      if existingIndex >= 0 then
        colorIndex = existingIndex
        _color = color
        return

      // No exact match found, proceed with approximation
      val r = color.getRed255
      val g = color.getGreen255
      val b = color.getBlue255

      // hue = 0, saturation = 1, brightness = 2
      val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
      val hue = hsb(0)
      val saturation = hsb(1)

      /*
       * 0 turns off the color override and returns to the inactive color set via sysex. Both 126 &
       * 127 enable override but set the color to the active color set via sysex. Seems that the
       * inclusion of 126 for this behaviour is a bug.
       *
       * ref: process_sw_rgb_update() in encoders.c
       */
      if saturation > 0.0 then
        val baseSaturation = 2.0 / 3.0 // RGB 0000FF
        colorIndex = Math.min(
          Math.floorMod(((125 * (baseSaturation - hue) + 1).toInt), 126),
          125
        )
        _color = color
      else
        colorIndex = 0 // Desaturated colors turn off LED
        _color = Color.blackColor()

    /** @return Twister color index of the current color state. */
    def getColorIndex(): Int =
      colorIndex

  /** Consumer that sends the light state to the twister. */
  private class LightStateSender(midiOut: MidiOut, midiInfo: MidiInfo)
      extends Consumer[LightState]:
    override def accept(state: LightState): Unit =
      if state == null then return
      midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, state.getColorIndex())

object TwisterRGBLight:
  private val ANIMATION_START_VALUE: Int = 1
  private val BRIGHTNESS_START_VALUE: Int = 17
