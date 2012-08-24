/**
 * Copyright 2009, 2010 Kevin Gaudin
 *
 * This file is part of EmailAlbum.
 *
 * EmailAlbum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EmailAlbum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EmailAlbum.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kg.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Helper class for enhanced picture loading with downscaling within a specified
 * dimension. Picture loading is split in 2 passes :
 * <ul>
 * <li>1st pass don't load the picture, just find it's real width and height and
 * calculate the optimal size and downsampling for next pass.</li>
 * <li>2nd pass read the picture with the best downsampling option to load only
 * the strictly necessary pixels.</li>
 * </ul>
 * 
 * @author Kevin Gaudin
 * 
 */
public class BitmapLoader {

    /**
     * A cache for storing the latest accessed bitmaps. If another call asks for
     * a larger resolution, we reload it and keep the latest.
     */
    private static NewLRUCache<String, Bitmap> bmpCache = new NewLRUCache<String, Bitmap>(500);
    /**
     * A cache for storing real dimensions of all accessed bitmaps. With this
     * the cost of the first pass is reduced when loading a previously accessed
     * picture.
     */
    private static ConcurrentHashMap<String, int[]> dimensionCache = new ConcurrentHashMap<String, int[]>();

    private static final String LOG_TAG = BitmapLoader.class.getSimpleName();

    /**
     * First pass: read the picture real size and calculate what will be the
     * final size and downsampling option considering the dimensions asked by
     * the caller.
     * 
     * @param context
     *            The application context.
     * @param width
     *            The maximum width of the final picture.
     * @param height
     *            The maximum height of the final picture.
     * @param input
     *            The input stream of the original picture.
     * @param cachedDimension
     *            If the BitmapLoader has already
     * @return
     * @throws IOException
     */
    private static FirstPassResult firstPass(Context context, Integer width,
            Integer height, InputStream input, int[] cachedDimension)
            throws IOException {
        Log.d(LOG_TAG, "Requested " + width + " x "
                + height);

        
        FirstPassResult fpResult = new FirstPassResult();

        // First, get image size
        fpResult.options.inJustDecodeBounds = true;
        if (cachedDimension != null) {
            fpResult.options.outWidth = cachedDimension[0];
            fpResult.options.outHeight = cachedDimension[1];
            // Log.d(LOG_TAG, "Cached size : " + cachedDimension[0] + " x "
            // + cachedDimension[1]);
        } else if (input != null) {
            // Log.d(LOG_TAG, "Fetching size...");
            BitmapFactory.decodeStream(new FlushedInputStream(input), null, fpResult.options);
            // Log.d(LOG_TAG, "... size fetched.");
            input.close();
        }
        int srcWidth = fpResult.options.outWidth;
        int srcHeight = fpResult.options.outHeight;

        Log.d(LOG_TAG, "Source picture has dimension " + srcWidth + " x "
         + srcHeight);

        float srcImageRatio = (float) srcWidth / (float) srcHeight;

        // If no resolution given, use the device screen resolution
        if (width == null && height == null) {
            Display display = ((WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            fpResult.finalWidth = display.getWidth();
            fpResult.finalHeight = display.getHeight();
             Log.d(LOG_TAG, "Display is : " + fpResult.finalWidth + " x "
             + fpResult.finalHeight);
        } else if (width == null) {
            // If only one dimension is given, keep source proportions
            fpResult.finalWidth = (int) (height * srcImageRatio);
            fpResult.finalHeight = height;
        } else if (height == null) {
            // If only one dimension is given, keep source proportions
            fpResult.finalHeight = (int) (width / srcImageRatio);
            fpResult.finalWidth = width;
        } else {
            fpResult.finalWidth = width;
            fpResult.finalHeight = height;
        }

        float requestedImageRatio = (float) fpResult.finalWidth
                / (float) fpResult.finalHeight;

        // Switch requested orientation to allow best quality without
        // reloading if device orientation changes
        if ((srcImageRatio > 1 && requestedImageRatio < 1)
                || (srcImageRatio < 1 && requestedImageRatio > 1)) {
            int oldValue = fpResult.finalWidth;
            fpResult.finalWidth = fpResult.finalHeight;
            fpResult.finalHeight = oldValue;
            requestedImageRatio = 1 / requestedImageRatio;
        }

        // 2 final requested dimensions are now given, adjust for best fit
        // with aspect ratio preserved

        // Calculates which dimension should be used to preserve aspect
        // ratio
        if (requestedImageRatio <= srcImageRatio) {
            fpResult.finalHeight = (int) (fpResult.finalWidth / srcImageRatio);
        } else if (requestedImageRatio > srcImageRatio) {
            fpResult.finalWidth = (int) (fpResult.finalHeight * srcImageRatio);
        }

        // Calculate the sample size needed to load image with minimum
        // required memory consumption.
        // We eventually load a larger bitmap if orientation is different so
        // that if device orientation changes, we don't have to reload a
        // finer sampled bitmap
        if (srcWidth > fpResult.finalWidth) {
            fpResult.options.inSampleSize = srcWidth / fpResult.finalWidth;
        }
        return fpResult;
    }

    /**
     * Load a picture from the given Uri.
     * 
     * @param context
     *            The application context.
     * @param uri
     *            The Uri where the picture is located.
     * @param width
     *            The maximum width of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param height
     *            The maximum height of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @return A Bitmap loaded from the Uri, scaled down to fit the given width
     *         and height.
     * @throws IOException
     */
    public static Bitmap load(Context context, Uri uri, Integer width,
            Integer height) throws IOException {
        return load(context, uri, width, height, Bitmap.Config.RGB_565, true);
    }

    /**
     * Load a picture from the given Uri.
     * 
     * @param context
     *            The application context.
     * @param uri
     *            The Uri where the picture is located.
     * @param width
     *            The maximum width of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param height
     *            The maximum height of the result bitmap. The original bitmap
     *            will be scaled down with aspect ratio preserved to fit both
     *            width/height. If null, the default value is the device screen
     *            size.
     * @param colorConfig
     *            The desired result color configuration, as defined in
     *            {@link Config}. If null, will use {@link Config#RGB_565} which
     *            is enough for screen display but might reduce color depth of
     *            the original picture.
     * @param cacheResult
     *            Wether we should store the result bitmap in cache or not. If
     *            you know that the result will be a big Bitmap, you should set
     *            this parameter to false (do not cache) and take care of
     *            recycling the result Bitmap as soon as it is not necessary
     *            anymore.
     * @return A Bitmap loaded from the Uri, scaled down to fit the given width
     *         and height.
     * @throws IOException
     */
    public static Bitmap load(Context context, Uri uri, Integer width,
            Integer height, Bitmap.Config colorConfig, boolean cacheResult)
            throws IOException {
        // The resulting Bitmap.
        Bitmap result = null;

        if (colorConfig == null) {
            // Default color configuration. Is good enough for screen display
            // and reduce memory usage, but might decrease the color depth of
            // the original picture.
            colorConfig = Bitmap.Config.RGB_565;
        }

        Log.d(LOG_TAG, "" + width + "x" + height + " - Open Uri" + uri.toString());
        InputStream input = context.getContentResolver().openInputStream(uri);

        if (input == null)
            return null;

        InputStream fpInput = null;
        int[] cachedDimension = null;
        if (!dimensionCache.containsKey(uri.toString())) {
            fpInput = input;
        } else {
            // We already have the result of the first pass. We should not
            // preload anything more.
            cachedDimension = dimensionCache.get(uri.toString());
            fpInput = null;
        }

        FirstPassResult fpResult = firstPass(context, width, height, fpInput,
                cachedDimension);
        int[] dimensionToCache = { fpResult.options.outWidth,
                fpResult.options.outHeight };
        // Store the dimension in cache so we don't have to get it again
        dimensionCache.put(uri.toString(), dimensionToCache);

        if (fpInput == null) {
            // The first pass input is null, so we can reuse the original input
            // stream as it has not been used for the first pass.
            fpInput = input;
        } else {
            // The original input stream has been consumed for the first pass.
            // Get a new one.
            fpInput = context.getContentResolver().openInputStream(uri);
        }

        Bitmap cachedBitmap = null;
        Log.d(LOG_TAG, "Check if " + uri.toString() + " is in cache.");
        cachedBitmap = bmpCache.get(uri.toString());
        boolean overWriteCache = true;
        if (cachedBitmap != null) {
            overWriteCache = false;
            Log.d(LOG_TAG, uri.toString() + " is in cache.");
            cachedBitmap = bmpCache.get(uri.toString());
            // We have a Bitmap in cache, but we have to check if its resolution
            // is large enough.
            Log.d(LOG_TAG, (cachedBitmap.getWidth() + 1) + " < "
                    + fpResult.finalWidth + " || "
                    + (cachedBitmap.getHeight() + 1) + " < "
                    + fpResult.finalHeight);
            if ((cachedBitmap.getWidth() + 1) < fpResult.finalWidth
                    || (cachedBitmap.getHeight() + 1) < fpResult.finalHeight) {
                // invalidate the existing entry
                Log.d(LOG_TAG, uri.toString() + " is not big enough !");
                overWriteCache = true;
                cachedBitmap = null;
            }

        }
        result = secondPass(context, fpInput, fpResult, colorConfig,
                cachedBitmap);

        // Store the result in cache
        if (cacheResult && result != null
                && overWriteCache) {
            bmpCache.put(uri.toString(), result);
        }

        return result;
    }

    
    /**
     * Create the new Bitmap fitting in the requested size.
     * 
     * @param context
     *            The application context.
     * @param input
     *            An InputStream providing the picture data.
     * @param fpResult
     *            The calculations obtained in the first pass (final dimension
     *            and optimized sample size).
     * @param colorConfig
     *            The color {@link Config} we should use to deliver the final
     *            Bitmap.
     * @param cachedBitmap
     *            If the picture has been previously loaded and if we can reuse
     *            the previous result, the cached Bitmap should be provided
     *            here.
     * @return The final Bitmap.
     * @throws IOException
     */
    private static Bitmap secondPass(Context context, InputStream input,
            FirstPassResult fpResult, Bitmap.Config colorConfig,
            Bitmap cachedBitmap) throws IOException {
        Bitmap result = null;
        if (input != null) {

            fpResult.options.inJustDecodeBounds = false;
            fpResult.options.inPreferredConfig = colorConfig;
            if (!colorConfig.equals(Config.ARGB_8888)) {
                // Other Configs reduce the color depth. Enforcing dithering
                // could help get nicer pictures.
                fpResult.options.inDither = true;
            }
            // Log.d(LOG_TAG, "fpResult =" + fpResult);

            Bitmap source = cachedBitmap;
            if (source == null) {
                Log.d(LOG_TAG, "No cached bitmap to use, loading from stream");
                // Log.d(LOG_TAG, "Decoding picture..." + fpResult);
                source = BitmapFactory.decodeStream(new FlushedInputStream(input), null,
                        fpResult.options);
                // Log.d(LOG_TAG, "Picture decoded.");
                input.close();
            }

            if (source != null) {
                // Log.d(LOG_TAG, "Loaded picture with dimension "
                // + source.getWidth() + " x " + source.getHeight());

                if (fpResult.finalWidth < source.getWidth()
                        || fpResult.finalHeight < source.getHeight()) {
                    // Resize the picture to the caller specs.
                    result = Bitmap.createScaledBitmap(source,
                            fpResult.finalWidth, fpResult.finalHeight, true);
                } else {
                    result = source;
                }

                // Log.d(LOG_TAG, "Resized picture to dimension "
                // + fpResult.finalWidth + " x " + fpResult.finalHeight);
            } else {
                result = null;
            }
        }
        return result;
    }
}
