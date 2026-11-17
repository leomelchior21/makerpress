package com.makerpress.app

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Small, local 24-point silhouettes keep the app independent of the full icon catalog.
private fun glyph(name: String, path: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black)).build()
private val printerIcon = glyph("Printer", "M6,2H18V7H6ZM4,8H20V18H17V22H7V18H4ZM9,15V20H15V15ZM17,10V12H19V10Z")
private val textIcon = glyph("Text", "M3,4H21V7H14V21H10V7H3Z")
private val imageIcon = glyph("Image", "M2,3H22V21H2ZM4,5V18L9,12L13,16L16,11L20,16V5ZM7,7H10V10H7Z")
private val gridIcon = glyph("Grid", "M2,2H22V22H2ZM4,4V9H9V4ZM11,4V9H13V4ZM15,4V9H20V4ZM4,11V13H9V11ZM11,11V13H13V11ZM15,11V13H20V11ZM4,15V20H9V15ZM11,15V20H13V15ZM15,15V20H20V15Z")
private val qrIcon = glyph("QR", "M2,2H10V10H2ZM4,4V8H8V4ZM14,2H22V10H14ZM16,4V8H20V4ZM2,14H10V22H2ZM4,16V20H8V16ZM13,13H17V17H13ZM18,18H22V22H18ZM18,12H22V15H18ZM12,19H15V22H12Z")
private val mathIcon = glyph("Math", "M2,2H22V22H2ZM4,4V20H20V4ZM6,8H8V6H10V8H12V10H10V12H8V10H6ZM14,8H18V10H14ZM6,15H12V17H6ZM14,14H18V16H14ZM14,18H18V19H14Z")
private val bluetoothIcon = glyph("Bluetooth", "M11,1L20,7L14,12L20,17L11,23V14L6,19L4,17L9,12L4,7L6,5L11,10ZM13,5V10L16,7ZM13,14V19L16,17Z")
private val receiptIcon = glyph("Receipt", "M4,2L7,4L10,2L13,4L16,2L20,4V22L17,20L14,22L11,20L8,22L4,20ZM7,7V9H17V7ZM7,12V14H17V12ZM7,17V19H14V17Z")
private val tuneIcon = glyph("Tune", "M2,5H7V2H9V5H22V7H9V10H7V7H2ZM2,16H15V13H17V16H22V18H17V22H15V18H2Z")
private val addIcon = glyph("Add", "M3,3H21V21H3ZM5,5V19H19V5ZM11,7H13V11H17V13H13V17H11V13H7V11H11Z")
private val eyeIcon = glyph("Preview", "M1,12L6,6H18L23,12L18,18H6ZM4,12L7,16H17L20,12L17,8H7ZM9,9H15V15H9Z")
private val diceIcon = glyph("Dice", "M3,3H21V21H3ZM6,6V9H9V6ZM15,6V9H18V6ZM10.5,10.5V13.5H13.5V10.5ZM6,15V18H9V15ZM15,15V18H18V15Z")
private val flagIcon = glyph("Flag", "M5,2H7V22H5ZM8,3L19,7.5L8,12Z")
private val rocketIcon = glyph("Rocket", "M12,1L16,6V15L19,19V22L15,19H9L5,22V19L8,15V6Z")
private val castleIcon = glyph("Castle", "M2,22V8H5V10H7V8H10V10H14V8H17V10H19V8H22V22H14V16A2,2 0 0 0 10,16V22Z")
private val mazeIcon = glyph("Maze", "M2,2H22V22H13V20H20V4H4V20H11V22H2ZM6,6H18V8H8V12H6ZM10,10H16V18H10V16H14V12H10Z")
private val mosaicIcon = glyph("Mosaic", "M3,3H9V9H3ZM10,3H14V9H10ZM15,10H21V14H15ZM3,10H9V21H3ZM10,15H21V21H10Z")
private val ballIcon = glyph("Ball", "M12,2A10,10 0 1 1 11.99,2ZM12,7A5,5 0 1 0 12.01,7Z")
private val codeIcon = glyph("Code", "M8,2H10L9,8H15L16,2H18L17,8H21V10H16.7L16,14H20V16H15.7L14.7,22H12.7L13.7,16H7.7L6.7,22H4.7L5.7,16H2V14H6L6.7,10H3V8H7ZM8.7,10L8,14H14L14.7,10Z")
private val calendarIcon = glyph("Calendar", "M3,4H21V22H3ZM5,9V20H19V9ZM7,2H9V6H7ZM15,2H17V6H15ZM7,11H11V15H7Z")
private val classIcon = glyph("Class", "M8,4A3,3 0 1 1 7.99,4ZM16,4A3,3 0 1 1 15.99,4ZM2,20C2,14 14,14 14,20ZM10,20C10,14 22,14 22,20Z")
private val rulerIcon = glyph("Ruler", "M2,8H22V16H2ZM4,10V14H6V12H8V14H10V12H12V14H14V12H16V14H18V12H20V10Z")
val Icons.Outlined.Casino get() = diceIcon
val Icons.Outlined.GolfCourse get() = flagIcon
val Icons.Outlined.RocketLaunch get() = rocketIcon
val Icons.Outlined.Castle get() = castleIcon
val Icons.Outlined.Maze get() = mazeIcon
val Icons.Outlined.Mosaic get() = mosaicIcon
val Icons.Outlined.Ball get() = ballIcon
val Icons.Outlined.Tag get() = codeIcon
val Icons.Outlined.Today get() = calendarIcon
val Icons.Outlined.Groups get() = classIcon
val Icons.Outlined.Straighten get() = rulerIcon
val Icons.Outlined.Print get() = printerIcon
val Icons.Outlined.TextFields get() = textIcon
val Icons.Outlined.Image get() = imageIcon
val Icons.Outlined.GridOn get() = gridIcon
val Icons.Outlined.QrCode2 get() = qrIcon
val Icons.Outlined.Calculate get() = mathIcon
val Icons.Outlined.Bluetooth get() = bluetoothIcon
val Icons.Outlined.ReceiptLong get() = receiptIcon
val Icons.Outlined.Tune get() = tuneIcon
val Icons.Outlined.AddCircleOutline get() = addIcon
val Icons.Outlined.Visibility get() = eyeIcon
