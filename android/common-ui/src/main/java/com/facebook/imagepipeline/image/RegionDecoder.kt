package com.facebook.imagepipeline.image

import android.graphics.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import com.facebook.common.references.CloseableReference
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.platform.PlatformDecoder

internal class RegionDecoder(
    private val mPlatformDecoder: PlatformDecoder,
    private val region: Rect,
) : ImageDecoder {
    override fun decode(
        encodedImage: EncodedImage,
        length: Int,
        qualityInfo: QualityInfo,
        options: ImageDecodeOptions
    ): CloseableImage? {
        val decodedBitmapReference = mPlatformDecoder.decodeJPEGFromEncodedImageWithColorSpace(
            encodedImage, options.bitmapConfig, region, length, options.colorSpace
        )
        return try {
            CloseableStaticBitmap.of(decodedBitmapReference, qualityInfo, 0)
        } finally {
            CloseableReference.closeSafely(decodedBitmapReference)
        }
    }

    companion object {
        fun create(left: Int, top: Int, right: Int, bottom: Int): RegionDecoder {
            return create(Rect(left, top, right, bottom))
        }

        fun create(region: IntRect): RegionDecoder  = create(region.toAndroidRect())

        fun create(region: Rect): RegionDecoder = RegionDecoder(
            Fresco.getImagePipelineFactory().getPlatformDecoder(),
            region
        )
    }
}
