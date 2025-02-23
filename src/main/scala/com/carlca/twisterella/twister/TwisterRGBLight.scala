package com.carlca.twisterella.twister

import com.bitwig.extension.api.Color
import com.bitwig.extension.controller.api.{HardwareLightVisualState, InternalHardwareLightState, MidiOut, MultiStateHardwareLight}
import com.carlca.twisterella.TwisterellaExtension
import java.util.function.Consumer
import java.util.function.Supplier
import com.carlca.twisterella.twister.TwisterLight.AnimationState

/** The RGB light on a twister knob. */
class TwisterRGBLight(extension: TwisterellaExtension, lightMidiInfo: LightMidiInfo, color: Color) extends TwisterLight(extension, lightMidiInfo.animation, TwisterRGBLight.ANIMATION_START_VALUE, TwisterRGBLight.BRIGHTNESS_START_VALUE):
  import TwisterRGBLight._

  def this(extension: TwisterellaExtension, lightMidiInfo: LightMidiInfo) = this(extension, lightMidiInfo, Color.nullColor())

  private val midiInfo: MidiInfo = lightMidiInfo.light

  private val light: MultiStateHardwareLight = extension.hardwareSurface.createMultiStateHardwareLight(s"RGB Light ${midiInfo.cc}")

  light.setColorToStateFunction((col: Color) => new LightState(col))
  light.state().onUpdateHardware(new LightStateSender(midiOut, midiInfo))
  light.setColor(color)

  /**
   * Sets the color of the light.
   *
   * @param color Desired color.
   */
  def setColor(color: Color): Unit =
    light.setColor(color)

  /**
   * Sets the color of the light using a raw MIDI value.
   *
   * @param value Desired color as a MIDI value.
   */
  def setRawValue(value: Int): Unit =
    light.setColor(TwisterColors.ALL(value))

  /**
   * Sets the color supplier for the light.
   *
   * @param colorSupplier Color supplier for the light.
   */
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
        case _ => false

    /**
     * Converts a color to the the nearest representable color of the twister.
     *
     * @param color Desired color.
     */
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
        colorIndex = Math.min(Math.floorMod(((125 * (baseSaturation - hue) + 1).toInt), 126), 125)
        _color = color
      else
        colorIndex = 0 // Desaturated colors turn off LED
        _color = Color.blackColor()

    /** @return Twister color index of the current color state. */
    def getColorIndex(): Int =
      colorIndex

  /** Consumer that sends the light state to the twister. */
  private class LightStateSender(midiOut: MidiOut, midiInfo: MidiInfo) extends Consumer[LightState]:
    override def accept(state: LightState): Unit =
      if state == null then return
      midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, state.getColorIndex())

object TwisterRGBLight:
  private val ANIMATION_START_VALUE: Int = 1
  private val BRIGHTNESS_START_VALUE: Int = 17
