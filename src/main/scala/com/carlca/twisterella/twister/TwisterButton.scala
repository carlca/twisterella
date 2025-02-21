package com.carlca.twisterella.twister

import java.awt.event.{ActionEvent, ActionListener}
import javax.swing.Timer
import com.bitwig.extension.controller.api.{BooleanValue, ControllerHost, HardwareButton, MidiIn}
import com.carlca.twisterella.TwisterellaExtension
import scala.collection.mutable

/**
 * A button on the Twister. Can be either a knob button or side button.
 *
 * Provides a clicked, double clicked and long press action that can be observed. An optional
 * "shift" button can be set for alternative actions when this button is held.
 */
class TwisterButton(extension: TwisterellaExtension, midiInfo: MidiInfo, idPrefix: String):
  import TwisterButton._

  assert(!idPrefix.isEmpty, "ID prefix is empty")

  private val midiIn: MidiIn = extension.midiIn
  private val host: ControllerHost = extension.getHost
  private val channel: Int = midiInfo.channel
  private val cc: Int = midiInfo.cc

  private val _clickedObservers = mutable.Set[Runnable]()
  private val _doubleClickedObservers = mutable.Set[Runnable]()
  private val _longPressedObservers = mutable.Set[Runnable]()
  private val _shiftClickedObservers = mutable.Set[Runnable]()
  private val _shiftDoubleClickedObservers = mutable.Set[Runnable]()
  private val _shiftLongPressedObservers = mutable.Set[Runnable]()
  private val longPressTimer: Timer =
    Timer(LONG_PRESS_DURATION, new ActionListener:
      override def actionPerformed(evt: ActionEvent): Unit =
        notifyLongPressedObservers()
    )

  private val button: HardwareButton = extension.hardwareSurface.createHardwareButton(s"$idPrefix Button ${midiInfo.cc}")

  button.pressedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, cc, PRESSED_VALUE))
  button.releasedAction().setActionMatcher(midiIn.createCCActionMatcher(channel, cc, RELEASED_VALUE))
  button.pressedAction().setBinding(host.createAction(ActionAdapter(handlePressed), () => "Handle button pressed"))
  button.releasedAction().setBinding(host.createAction(ActionAdapter(handleReleased), () => "Handle button released"))

  longPressTimer.setRepeats(false)

  private var lastReleasedTime: Long = 0L
  private var shiftButton: Option[TwisterButton] = None

  /**
   * Sets the button that will be used for shift functionality.
   *
   * @param shiftButton The button to use for shift functionality.
   */
  def setShiftButton(shiftButton: TwisterButton): Unit =
    this.shiftButton = Some(shiftButton)

  /**
   * Sets an observer of the double clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to double click action.
   */
  def setDoubleClickedObserver(observer: Runnable): Unit =
    _doubleClickedObservers.clear()
    _doubleClickedObservers.add(observer)

  /**
   * Sets an observer of the shift double clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observers.
   *
   * @param shiftObserver Observer to set to shift double click action.
   */
  def setShiftDoubleClickedObserver(shiftObserver: Runnable): Unit =
    _shiftDoubleClickedObservers.clear()
    _shiftDoubleClickedObservers.add(shiftObserver)

  /**
   * Adds an observer of the double clicked action.
   *
   * @param observer Observer to add to double click action.
   * @return True if the observer was already added.
   */
  def addDoubleClickedObserver(observer: Runnable): Boolean =
    _doubleClickedObservers.add(observer)

  /**
   * Adds an observer of the shift double clicked action.
   *
   * @param shiftObserver Observer to add to shift double click action.
   * @return True if the observer was already added.
   */
  def addShiftDoubleClickedObserver(shiftObserver: Runnable): Boolean =
    _shiftDoubleClickedObservers.add(shiftObserver)

  /** Clears all observers of the double clicked action. */
  def clearDoubleClickedObservers(): Unit =
    _doubleClickedObservers.clear()

  /** Clears all observers of the shift double clicked action. */
  def clearShiftDoubleClickedObservers(): Unit =
    _shiftDoubleClickedObservers.clear()

  /**
   * Sets an observer of the clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to the clicked action.
   */
  def setClickedObserver(observer: Runnable): Boolean =
    _clickedObservers.clear()
    _clickedObservers.add(observer)

  /**
   * Sets an observer of the shift clicked action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param shiftObserver Observer to set to the shift clicked action.
   */
  def setShiftClickedObserver(shiftObserver: Runnable): Boolean =
    _shiftClickedObservers.clear()
    _shiftClickedObservers.add(shiftObserver)

  /**
   * Adds an observer of the clicked action.
   *
   * @param observer Observer to add to the clicked action.
   * @return True if the observer was already added.
   */
  def addClickedObserver(observer: Runnable): Boolean =
    _clickedObservers.add(observer)

  /**
   * Adds an observer of the shift clicked action.
   *
   * @param shiftObserver Observer to add to shift clicked action.
   * @return True if the observer was already added.
   */
  def addShiftClickedObserver(shiftObserver: Runnable): Boolean =
    _shiftClickedObservers.add(shiftObserver)

  /** Clears all observers of the clicked action. */
  def clearClickedObservers(): Unit =
    _clickedObservers.clear()

  /** Clears all observers of the shift clicked action. */
  def clearShiftClickedObservers(): Unit =
    _shiftClickedObservers.clear()

  /**
   * Sets an observer of the long pressed action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observer.
   *
   * @param observer Observer to set to the long pressed action.
   */
  def setLongPressedObserver(observer: Runnable): Boolean =
    _longPressedObservers.clear()
    _longPressedObservers.add(observer)

  /**
   * Sets an observer of the shift long pressed action. This will then be the only observer.
   *
   * A convenience function that clears and then adds the observers.
   *
   * @param shiftObserver Observer to set to the shift long pressed action.
   */
  def setShiftLongPressedObserver(shiftObserver: Runnable): Boolean =
    _shiftLongPressedObservers.clear()
    _shiftLongPressedObservers.add(shiftObserver)

  /**
   * Adds an observer of the long pressed action.
   *
   * @param observer Observer to add to the long pressed action.
   * @return True if the observer was already added.
   */
  def addLongPressedObserver(observer: Runnable): Boolean =
    _longPressedObservers.add(observer)

  /**
   * Adds an observer of the shift long pressed action.
   *
   * @param shiftObserver Observer to add to the shift long pressed action.
   * @return True if the observer was already added.
   */
  def addShiftLongPressedObserver(shiftObserver: Runnable): Boolean =
    _shiftLongPressedObservers.add(shiftObserver)

  /** Clears all observers of the long pressed action. */
  def clearLongPressedObserver(): Unit =
    _longPressedObservers.clear()

  /** Clears all observers of the shift long pressed action. */
  def clearShiftLongPressedObserver(): Unit =
    _shiftLongPressedObservers.clear()

  /** Returns the buttons pressed state. */
  def isPressed(): BooleanValue =
    button.isPressed()

  /** Internal handler of the hardware pressed action. */
  private def handlePressed(): Unit =
    longPressTimer.start()

  /** Internal handler of the hardware released action. */
  private def handleReleased(): Unit =
    if longPressTimer.isRunning then
      longPressTimer.stop()
      notifyClickedObservers()

      val now = System.nanoTime()
      if (now - lastReleasedTime) < DOUBLE_CLICK_DURATION then
        notifyDoubleClickedObservers()

      lastReleasedTime = now

  private def isShiftPressed: Boolean =
    shiftButton.exists(_.isPressed().get())

  private def notifyClickedObservers(): Unit =
    val observersToNotify = if isShiftPressed then _shiftClickedObservers else _clickedObservers
    observersToNotify.foreach(_.run())

  private def notifyDoubleClickedObservers(): Unit =
    val observersToNotify = if isShiftPressed then _shiftDoubleClickedObservers else _doubleClickedObservers
    observersToNotify.foreach(_.run())

  private def notifyLongPressedObservers(): Unit =
    val observersToNotify = if isShiftPressed then _shiftLongPressedObservers else _longPressedObservers
    observersToNotify.foreach(_.run())

object TwisterButton:
  private val DOUBLE_CLICK_DURATION: Long = 300 * 1000000L // ns
  private val LONG_PRESS_DURATION: Int = 250 // ms
  private val PRESSED_VALUE: Int = 127
  private val RELEASED_VALUE: Int = 0

  // Helper for converting Runnable to Action
  case class ActionAdapter(action: () => Unit) extends java.util.function.Consumer[com.bitwig.extension.controller.api.HardwareAction]:
    override def accept(t: com.bitwig.extension.controller.api.HardwareAction): Unit =
      action()
