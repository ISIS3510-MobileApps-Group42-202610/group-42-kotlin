package com.example.unimarketfrontend.ui.utils

object ImageUtils {
    /**
     * Appends Cloudinary thumbnail transformations if the URL is from Cloudinary.
     * Transformation: w_128,h_128,c_fill,q_auto,f_auto
     */
    fun getThumbnailUrl(originalUrl: String?): String? {
        if (originalUrl == null) return null

        return if (isCloudinaryUrl(originalUrl)) {
            // Find the /upload/ part and insert transformations
            val uploadPart = "/upload/"
            if (originalUrl.contains(uploadPart)) {
                originalUrl.replace(uploadPart, "${uploadPart}w_128,h_128,c_fill,q_auto,f_auto/")
            } else {
                originalUrl
            }
        } else {
            originalUrl
        }
    }

    private fun isCloudinaryUrl(url: String): Boolean {
        return url.contains("res.cloudinary.com") || url.contains(".cloudinary.com")
    }
}
