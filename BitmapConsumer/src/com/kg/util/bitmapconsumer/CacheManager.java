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
package com.kg.util.bitmapconsumer;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * Provider for the application cache directories. If an external storage is
 * available, use it, otherwise use the application context cache directory/
 * 
 * @author Kevin Gaudin
 * 
 */
public class CacheManager {
    private static final String LOG_TAG = CacheManager.class.getSimpleName();

    private Context mContext = null;

    public CacheManager(Context ctx) {
        mContext = ctx;
    }

    /**
     * Get the application cache root.
     * 
     * @return The application cache root.
     */
    public File getCacheDir() {
        return getCacheDir(null);
    }

    /**
     * Get a cache directory for a specific task.
     * 
     * @param subdir
     *            The name of the task, will result as a subdirectory of the
     *            cache root.
     * @return The cache directory for this task.
     */
    public File getCacheDir(String subdir) {
        if (subdir == null) {
            subdir = "";
        }

        // Default root is the application context cache.
        File result = new File(mContext.getCacheDir(), subdir);

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            // If an external storage is available, use it as it will prevent
            // from overloading the internal memory.
            result = new File(Environment.getExternalStorageDirectory(),
                    "data/EmailAlbum/.cache/" + subdir);
        }

        // Physically create the directory (and its parents) if it does not
        // exist.
        if (!result.exists()) {
            result.mkdirs();
        }
        
        File noMedia = new File(result, ".nomedia");
        try {
            noMedia.createNewFile();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(LOG_TAG, "Error : ", e);
        }

        // Log.i(LOG_TAG, "Using dir " + result + " for cache");
        return result;
    }

    // Clear the whole application cache.
    public void clearCache() {
        clearCache(null);
    }

    // Clear only the cache of a specific task.
    public void clearCache(String subdir) {
        File cacheDir = getCacheDir(subdir);
        if(cacheDir != null) {
            String[] files = cacheDir.list();
            if(files != null && files.length > 0) {
                for (String cachedFile : getCacheDir(subdir).list()) {
                    deleteDirectory(new File(getCacheDir(subdir), cachedFile));
                }
            }
        }
    }

    /**
     * Delete a directory and all its content.
     * 
     * @param path
     *            The directory to delete.
     * @return
     */
    static public int deleteDirectory(File path) {
        int nbDeleted = 0;
        if (path.exists() && path.isDirectory()) {
            File[] files = path.listFiles();

            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    nbDeleted += deleteDirectory(files[i]);
                } else {
                    if(files[i].delete()) {
                        nbDeleted++;
                    }
                    
                }
            }
        } else if (path.exists()) {
            if(path.delete()) {
                nbDeleted++;
            }
        }
        return nbDeleted;
    }
    
    public File getInboxDir() {
        String subdir = "received";
        // Default root is the application context internal files dir.
        File result = new File(mContext.getFilesDir(), subdir);

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            // If an external storage is available, use it as it will prevent
            // from overloading the internal memory.
            result = new File(Environment.getExternalStorageDirectory(),
                    "data/EmailAlbum/" + subdir);
        }

        // Physically create the directory (and its parents) if it does not
        // exist.
        if (!result.exists()) {
            result.mkdirs();
        }

        // Log.i(LOG_TAG, "Using dir " + result + " for cache");
        return result;
    }

    public File getOutboxDir() {
        String subdir = "created";
        // Default root is the application context internal files dir.
        File result = new File(mContext.getFilesDir(), subdir);

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            // If an external storage is available, use it as it will prevent
            // from overloading the internal memory.
            result = new File(Environment.getExternalStorageDirectory(),
                    "data/EmailAlbum/" + subdir);
        }

        // Physically create the directory (and its parents) if it does not
        // exist.
        if (!result.exists()) {
            result.mkdirs();
        }

        // Log.i(LOG_TAG, "Using dir " + result + " for cache");
        return result;
    }

}
