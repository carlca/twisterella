package com.carlca.bitwigutils

import com.bitwig.extension.controller.api.*

object Settings:
  enum PanSendMode derives CanEqual:
    case `FX Send`, `Pan`
  enum TrackMode derives CanEqual:
    case `One to One`, `No Groups`, `Groups Only`, `Tagged "<>" Only`

  var sensitivity: Double = 50.0
  var exclusiveSolo: Boolean = false
  var panSendMode: PanSendMode = PanSendMode.`FX Send`
  var trackMode:  TrackMode = TrackMode.`One to One`

  def init(host: ControllerHost) =
    initPreferences(host)

  def initPreferences(host: ControllerHost): Unit =
    val prefs = host.getPreferences

    val sensitivitySetting = prefs.getNumberSetting("Sensitivity", "Sensitivity", 0.0, 100.0, 1.0, null, 50)
    sensitivitySetting.addValueObserver((value) => Settings.sensitivity = value)

    val soloSetting = prefs.getBooleanSetting("Exclusive Solo", "Solo Behaviour", false)
    soloSetting.addValueObserver((value) => Settings.exclusiveSolo = value)

    val values = PanSendMode.values.map(_.toString).toArray
    val panSetting = prefs.getEnumSetting("Send/Pan Mode", "Third Row Behaviour", values, PanSendMode.`FX Send`.toString())
    panSetting.addValueObserver((value) => Settings.panSendMode = PanSendMode.valueOf(value))

    val trackModes = TrackMode.values.map(_.toString).toArray
    val trackSetting = prefs.getEnumSetting("Track Mode", "Track Mapping Behaviour", trackModes, TrackMode.`One to One`.toString())
    trackSetting.addValueObserver((value) => Settings.trackMode = TrackMode.valueOf(value))
