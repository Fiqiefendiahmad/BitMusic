package com.music.bitchord.ui.graphics

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.toBitmap

// Android bitmaps are CPU-readable, like Skia's.
actual fun ImageRequest.Builder.forPixelAccess(): ImageRequest.Builder = this

actual fun Image.toImageBitmap(): ImageBitmap = toBitmap().asImageBitmap()

actual fun imageBitmapOf(pixels: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

actual fun paletteSwatches(image: ImageBitmap, maxColors: Int, clearFilters: Boolean): List<Swatch> =
    PaletteQuantizer.swatches(image, maxColors, clearFilters)
