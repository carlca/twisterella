package com.carlca
package twisterella

import com.bitwig.extension.api.PlatformType
import com.bitwig.extension.controller.AutoDetectionMidiPortNamesList
import com.bitwig.extension.controller.ControllerExtensionDefinition
import com.bitwig.extension.controller.api.ControllerHost

import java.util.UUID

class TwisterellaExtensionDefinition extends ControllerExtensionDefinition:
  private val DRIVER_ID              = UUID.fromString("a9540dda-6ce5-4694-9195-928de837c07d")
  override def getName               = "Twisterella"
  override def getAuthor             = "carlcaulkett"
  override def getVersion            = "0.1.o"
  override def getId: UUID           = DRIVER_ID
  override def getHardwareVendor     = "DJ TechTools"
  override def getHardwareModel      = "Twisterella"
  override def getRequiredAPIVersion = 18
  override def getNumMidiInPorts     = 1
  override def getNumMidiOutPorts    = 1
  override def listAutoDetectionMidiPortNames(list: AutoDetectionMidiPortNamesList, platformType: PlatformType): Unit = ()
  override def createInstance(host: ControllerHost) = new TwisterellaExtension(this, host)
end TwisterellaExtensionDefinition
