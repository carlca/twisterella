package com.carlca.twisterella.twister

import com.bitwig.extension.controller.api.DoubleValue
import com.bitwig.extension.controller.api.SettableRangedValue
import com.carlca.twisterella.TwisterellaExtension
import com.carlca.utils.CursorNormalizedValue
import com.carlca.utils.MathUtil
import com.carlca.twisterella.twister.TwisterLight.AnimationState

/** The ring light on a Twister knob. */
class TwisterRingLight(extension: TwisterellaExtension, lightMidiInfo: LightMidiInfo) extends TwisterLight(extension, lightMidiInfo.animation, TwisterRingLight.ANIMATION_START_VALUE, TwisterRingLight.BRIGHTNESS_START_VALUE):

  private val midiInfo: MidiInfo = lightMidiInfo.light

  /**
   * Sets the ring value using raw MIDI data.
   *
   * @param value The desired ring value in MIDI.
   */
  def setRawValue(value: Int): Unit =
    midiOut.sendMidi(midiInfo.statusByte, midiInfo.cc, MathUtil.clamp(value, 0, 127))

  /**
   * Sets the ring value using a normalized 0-1 range.
   *
   * @param value The desired ring value in normalized range.
   */
  def setValue(value: Double): Unit =
    setRawValue((value * 127.0).toInt)

  /**
   * A special normalized 0-1 range value for ring lights being used as cursors in "dot" mode.
   *
   * Intended to be used with the value from a CursorNormalizedValue wrapper.
   *
   * Values in the 0-1 range will show the dot, even when at 0. Negative values hide the dot.
   *
   * @param value The cursor value to set.
   */
  def setCursorValue(value: Double): Unit =
    if value < 0 then
      setRawValue(0)
      return

    setRawValue(((value * 126.0) + 1.0).toInt)

  /**
   * Makes this light an observer of the passed in value.
   *
   * @param value The value to observe.
   */
  def observeValue(value: SettableRangedValue): Unit =
    value.markInterested()
    value.addValueObserver(128, new com.bitwig.extension.callback.IntegerValueChangedCallback {
      override def valueChanged(newValue: Int): Unit = {
        setRawValue(newValue)
      }
    })

  /**
   * Makes this light an observer of the passed in value.
   *
   * @param value The value to observe.
   */
  def observeValue(value: DoubleValue): Unit =
    value.markInterested()
    value.addValueObserver(new com.bitwig.extension.callback.DoubleValueChangedCallback {
      override def valueChanged(newValue: Double): Unit = {
        setValue(newValue)
      }
    })

  /**
   * Makes this light an observer of the passed in wrapper.
   *
   * @param wrapper The wrapper to observe.
   */
  def observeValue(wrapper: CursorNormalizedValue): Unit =
    wrapper.addValueObserver(new com.bitwig.extension.callback.DoubleValueChangedCallback {
      override def valueChanged(newValue: Double): Unit = {
        setCursorValue(newValue)
      }
    })

  override def lightOff(): Unit =
    setAnimationState(AnimationState.OFF)
    setRawValue(0)

object TwisterRingLight:
  private val ANIMATION_START_VALUE: Int = 49
  private val BRIGHTNESS_START_VALUE: Int = 65
