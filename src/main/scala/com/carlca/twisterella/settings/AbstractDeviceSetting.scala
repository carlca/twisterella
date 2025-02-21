package com.carlca.twisterella.settings

import com.bitwig.extension.controller.api.Device
import com.bitwig.extension.controller.api.Parameter
import scala.collection.mutable.Map

abstract class AbstractDeviceSetting[IdType, ParamType](val id: IdType, val params: Map[String, ParamType]):
  def parameters(): Map[String, ParamType] =
    params
  def createParameter(device: Device, key: String): Parameter
