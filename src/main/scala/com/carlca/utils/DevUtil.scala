package com.carlca.utils

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import scala.collection.mutable.ListBuffer
import com.bitwig.extension.controller.api.{Action, ActionCategory, Application, ControllerHost}
import scala.jdk.CollectionConverters._ // <-- ADD THIS LINE

/** Extension development utilities. */
object DevUtil:
  /**
   * Dumps all the available Bitwig actions to a text file.
   *
   * @param host The extension controller host.
   * @param outputFile The path of the output file.
   * @throws IOException
   */
  @throws[IOException]
  def dumpBitwigActions(host: ControllerHost, outputFile: Path): Unit =
    val app = host.createApplication()
    val output = ListBuffer[String]()

    for actionCat <- app.getActionCategories() do
      output += actionCat.getName
      for action <- actionCat.getActions() do
        output += "--- ID: " + action.getId + " Name: " + action.getName

    Files.write(outputFile, output.asJava, StandardCharsets.UTF_8)
