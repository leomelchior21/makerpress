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
