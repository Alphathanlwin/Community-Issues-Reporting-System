package com.uit.scirs.report.entity;

/**
 * Image quality and relevance classification produced by the AI Vision inspection.
 */
public enum AiImageQuality {
    /** Clear image with high issue visibility and scene sharpness. */
    CLEAR,

    /** Blurry, low contrast, out of focus, or severe motion artifacts. */
    BLURRY,

    /** Photo does not appear to match the selected category or textual description. */
    MISMATCH,

    /** Image dimensions or file size are too low (< 300x300 or < 4KB) for reliable machine inspection. */
    LOW_RESOLUTION,

    /** No photo was uploaded with the report. */
    NO_IMAGE
}
