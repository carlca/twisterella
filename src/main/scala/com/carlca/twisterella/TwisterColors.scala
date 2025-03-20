package com.carlca.twisterella

import com.bitwig.extension.api.Color
import scala.collection.immutable.List
import scala.math._

object TwisterColors:
  val ALL: List[Color] = colorList()

  val BITWIG_PARAMETERS: List[Color] = parameterColorList()

  def unpackColor(color: Color): (Int, Int, Int) =
    val r = color.getRed255
    val g = color.getGreen255
    val b = color.getBlue255
    (r, g, b)

  // Helper function to convert linear RGB to CIE XYZ - part 1: sRGB to linear RGB
  private def sRgbToLinearRgb(c: Int): Double =
    val colorFraction = c.toDouble / 255.0
    if (colorFraction <= 0.04045) then
      colorFraction / 12.92
    else
      pow((colorFraction + 0.055) / 1.055, 2.4)

  // Helper function to convert linear RGB to CIE XYZ - part 2: linear RGB to XYZ
  private def linearRgbToXyz(rLinear: Double, gLinear: Double, bLinear: Double): (Double, Double, Double) =
    val x = rLinear * 0.4124564 + gLinear * 0.3575761 + bLinear * 0.1804375
    val y = rLinear * 0.2126729 + gLinear * 0.7151522 + bLinear * 0.0721750
    val z = rLinear * 0.0193339 + gLinear * 0.1191920 + bLinear * 0.9503041
    (x, y, z)

  // Helper function to convert XYZ to CIE Lab - part 1: XYZ to normalized XYZ
  private def normalizeXyz(value: Double, whitePoint: Double): Double =
    val normalizedValue = value / whitePoint
    if (normalizedValue > 0.008856) then
      pow(normalizedValue, 1.0 / 3.0)
    else
      (7.787 * normalizedValue) + (16.0 / 116.0)

  // Main function to convert RGB to CIE Lab
  def rgbToLab(r: Int, g: Int, b: Int): (Double, Double, Double) =
    // Standard sRGB conversion to linear RGB
    val rLinear = sRgbToLinearRgb(r)
    val gLinear = sRgbToLinearRgb(g)
    val bLinear = sRgbToLinearRgb(b)

    // Linear RGB to CIE XYZ
    val (x, y, z) = linearRgbToXyz(rLinear, gLinear, bLinear)

    // Normalize XYZ with D65 white point (standard for sRGB)
    val xNormalized = normalizeXyz(x, 0.95047) // D65 X = 0.95047
    val yNormalized = normalizeXyz(y, 1.00000) // D65 Y = 1.00000 (reference white)
    val zNormalized = normalizeXyz(z, 1.08883) // D65 Z = 1.08883

    // Calculate CIE Lab values
    val l = (116.0 * yNormalized) - 16.0
    val a = 500.0 * (xNormalized - yNormalized)
    val b_lab = 200.0 * (yNormalized - zNormalized) // Renamed to b_lab to avoid name conflict with 'b' from RGB
    (l, a, b_lab)

  // Modified function to find the closest color in TwisterColors.ALL using CIE Lab distance
  def findTwisterColorLab(bitwigColor: Color): Int = // Input now explicitly RGB tuple
    val (r, g, b) = unpackColor(bitwigColor)
    val (l1, a1, b1_lab) = rgbToLab(r, g, b) // Convert input color to Lab
    val size = TwisterColors.ALL.size - 1
    var foundIndex = 0
    var minDistance = Double.MaxValue
    for (i <- 0 to size) do
      val (tr, tg, tb) = unpackColor(TwisterColors.ALL(i))
      val (l2, a2, b2_lab) = rgbToLab(tr, tg, tb)

      // Euclidean distance in Lab space
      val distance = Math.sqrt(Math.pow(l2 - l1, 2) + Math.pow(a2 - a1, 2) + Math.pow(b2_lab - b1_lab, 2))
      if (distance < minDistance) then
        minDistance = distance
        foundIndex = i
    foundIndex

  // Helper function to find the closest color in TwisterColors.ALL
  def findTwisterColorRGB(bitwigColor: Color): Int =
    val (r, g, b) = unpackColor(bitwigColor)
    val size = TwisterColors.ALL.size - 1
    var foundIndex = 0
    var minDistance = Double.MaxValue
    for (i <- 0 to size) do
      val (tr, tg, tb) = unpackColor(TwisterColors.ALL(i))
      val distance = Math.sqrt(Math.pow(tr - r, 2) + Math.pow(tg - g, 2) + Math.pow(tb - b, 2))
      if (distance < minDistance) then
        minDistance = distance
        foundIndex = i
    foundIndex

  /** List based on https://github.com/DJ-TechTools/Midi_Fighter_Twister_Open_Source/blob/master/src/colorMap.c */
  private def colorList(): List[Color] =
    List(
      Color.fromRGB255(0, 0, 0), // 0
      Color.fromRGB255(0, 0, 255), // 1 - Blue
      Color.fromRGB255(0, 21, 255), // 2 - Blue (Green Rising)
      Color.fromRGB255(0, 34, 255), //
      Color.fromRGB255(0, 46, 255), //
      Color.fromRGB255(0, 59, 255), //
      Color.fromRGB255(0, 68, 255), //
      Color.fromRGB255(0, 80, 255), //
      Color.fromRGB255(0, 93, 255), //
      Color.fromRGB255(0, 106, 255), //
      Color.fromRGB255(0, 119, 255), //
      Color.fromRGB255(0, 127, 255), //
      Color.fromRGB255(0, 140, 255), //
      Color.fromRGB255(0, 153, 255), //
      Color.fromRGB255(0, 165, 255), //
      Color.fromRGB255(0, 178, 255), //
      Color.fromRGB255(0, 191, 255), //
      Color.fromRGB255(0, 199, 255), //
      Color.fromRGB255(0, 212, 255), //
      Color.fromRGB255(0, 225, 255), //
      Color.fromRGB255(0, 238, 255), //
      Color.fromRGB255(0, 250, 255), // 21 - End of Blue's Reign
      Color.fromRGB255(0, 255, 250), // 22 - Green (Blue Fading)
      Color.fromRGB255(0, 255, 237), //
      Color.fromRGB255(0, 255, 225), //
      Color.fromRGB255(0, 255, 212), //
      Color.fromRGB255(0, 255, 199), //
      Color.fromRGB255(0, 255, 191), //
      Color.fromRGB255(0, 255, 178), //
      Color.fromRGB255(0, 255, 165), //
      Color.fromRGB255(0, 255, 153), //
      Color.fromRGB255(0, 255, 140), //
      Color.fromRGB255(0, 255, 127), //
      Color.fromRGB255(0, 255, 119), //
      Color.fromRGB255(0, 255, 106), //
      Color.fromRGB255(0, 255, 93), //
      Color.fromRGB255(0, 255, 80), //
      Color.fromRGB255(0, 255, 67), //
      Color.fromRGB255(0, 255, 59), //
      Color.fromRGB255(0, 255, 46), //
      Color.fromRGB255(0, 255, 33), //
      Color.fromRGB255(0, 255, 21), //
      Color.fromRGB255(0, 255, 8), //
      Color.fromRGB255(0, 255, 0), // 43 - Green
      Color.fromRGB255(12, 255, 0), // 44 - Green/Red Rising
      Color.fromRGB255(25, 255, 0), //
      Color.fromRGB255(38, 255, 0), //
      Color.fromRGB255(51, 255, 0), //
      Color.fromRGB255(63, 255, 0), //
      Color.fromRGB255(72, 255, 0), //
      Color.fromRGB255(84, 255, 0), //
      Color.fromRGB255(97, 255, 0), //
      Color.fromRGB255(110, 255, 0), //
      Color.fromRGB255(123, 255, 0), //
      Color.fromRGB255(131, 255, 0), //
      Color.fromRGB255(144, 255, 0), //
      Color.fromRGB255(157, 255, 0), //
      Color.fromRGB255(170, 255, 0), //
      Color.fromRGB255(182, 255, 0), //
      Color.fromRGB255(191, 255, 0), //
      Color.fromRGB255(203, 255, 0), //
      Color.fromRGB255(216, 255, 0), //
      Color.fromRGB255(229, 255, 0), //
      Color.fromRGB255(242, 255, 0), //
      Color.fromRGB255(255, 255, 0), // 64 - Green + Red (Yellow)
      Color.fromRGB255(255, 246, 0), // 65 - Red, Green Fading
      Color.fromRGB255(255, 233, 0), //
      Color.fromRGB255(255, 220, 0), //
      Color.fromRGB255(255, 208, 0), //
      Color.fromRGB255(255, 195, 0), //
      Color.fromRGB255(255, 187, 0), //
      Color.fromRGB255(255, 174, 0), //
      Color.fromRGB255(255, 161, 0), //
      Color.fromRGB255(255, 148, 0), //
      Color.fromRGB255(255, 135, 0), //
      Color.fromRGB255(255, 127, 0), //
      Color.fromRGB255(255, 114, 0), //
      Color.fromRGB255(255, 102, 0), //
      Color.fromRGB255(255, 89, 0), //
      Color.fromRGB255(255, 76, 0), //
      Color.fromRGB255(255, 63, 0), //
      Color.fromRGB255(255, 55, 0), //
      Color.fromRGB255(255, 42, 0), //
      Color.fromRGB255(255, 29, 0), //
      Color.fromRGB255(255, 16, 0), //
      Color.fromRGB255(255, 4, 0), // 85 - End Red/Green Fading
      Color.fromRGB255(255, 0, 4), // 86 - Red/ Blue Rising
      Color.fromRGB255(255, 0, 16), //
      Color.fromRGB255(255, 0, 29), //
      Color.fromRGB255(255, 0, 42), //
      Color.fromRGB255(255, 0, 55), //
      Color.fromRGB255(255, 0, 63), //
      Color.fromRGB255(255, 0, 76), //
      Color.fromRGB255(255, 0, 89), //
      Color.fromRGB255(255, 0, 102), //
      Color.fromRGB255(255, 0, 114), //
      Color.fromRGB255(255, 0, 127), //
      Color.fromRGB255(255, 0, 135), //
      Color.fromRGB255(255, 0, 148), //
      Color.fromRGB255(255, 0, 161), //
      Color.fromRGB255(255, 0, 174), //
      Color.fromRGB255(255, 0, 186), //
      Color.fromRGB255(255, 0, 195), //
      Color.fromRGB255(255, 0, 208), //
      Color.fromRGB255(255, 0, 221), //
      Color.fromRGB255(255, 0, 233), //
      Color.fromRGB255(255, 0, 246), //
      Color.fromRGB255(255, 0, 255), // 107 - Blue + Red
      Color.fromRGB255(242, 0, 255), // 108 - Blue/ Red Fading
      Color.fromRGB255(229, 0, 255), //
      Color.fromRGB255(216, 0, 255), //
      Color.fromRGB255(204, 0, 255), //
      Color.fromRGB255(191, 0, 255), //
      Color.fromRGB255(182, 0, 255), //
      Color.fromRGB255(169, 0, 255), //
      Color.fromRGB255(157, 0, 255), //
      Color.fromRGB255(144, 0, 255), //
      Color.fromRGB255(131, 0, 255), //
      Color.fromRGB255(123, 0, 255), //
      Color.fromRGB255(110, 0, 255), //
      Color.fromRGB255(97, 0, 255), //
      Color.fromRGB255(85, 0, 255), //
      Color.fromRGB255(72, 0, 255), //
      Color.fromRGB255(63, 0, 255), //
      Color.fromRGB255(50, 0, 255), //
      Color.fromRGB255(38, 0, 255), //
      Color.fromRGB255(25, 0, 255), // 126 - Blue-ish
      Color.fromRGB255(240, 240, 225) // 127 - White ?
    )

  /** Generates the list of Twister colors that match Bitwig parameters. */
  private def parameterColorList(): List[Color] =
    // Tweaked by eye for closest match to Bitwig parameter colors
    List(
      ALL(86), // RGB(244, 27, 62)
      ALL(70), // RGB(255, 127, 23)
      ALL(64), // RGB(252, 235, 35)
      ALL(51), // RGB(91, 197, 21)
      ALL(37), // RGB(101, 206, 146)
      ALL(14), // RGB(92, 168, 238)
      ALL(111), // RGB(195, 110, 255)
      ALL(97) // RGB(255, 84, 176)
    )
