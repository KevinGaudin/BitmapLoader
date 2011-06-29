package com.kg.util;

import android.graphics.BitmapFactory.Options;

/**
 * The results from the first pass : final dimension of the picture fitting
 * the requested dimension, preserving the picture aspect ratio.
 * 
 * @author Normal
 * 
 */
class FirstPassResult {
    /** The picture width calculated to fit in the requested dimensions. */
    public int finalHeight = 0;
    /** The picture height calculated to fit in the requested dimensions. */
    public int finalWidth = 0;
    /**
     * Options containing the best downsampling factor to avoid loading the
     * full picture.
     */
    public Options options = new Options();

    @Override
    public String toString() {
        return "{finalWidth=" + finalWidth + ", finalHeight=" + finalHeight
                + ", options={inSampleSize=" + options.inSampleSize + "}}";
    }
}