package com.kg.oifilemanager.filemanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.kg.oifilemanager.filemanager.util.MimeTypeParser;
import com.kg.oifilemanager.filemanager.util.MimeTypes;
import com.kg.util.bitmapconsumer.R;

public class FileManagerProvider extends ContentProvider {

    private static final String LOG_TAG = "FileManagerProvider";
    public static final String AUTHORITY = "com.kg.bitmapconsumer.share";
    public static final String CONTENT_URI_STRING = ContentResolver.SCHEME_CONTENT
            + "://" + AUTHORITY;
    public static final Uri CONTENT_URI = Uri.parse(CONTENT_URI_STRING);
    private MimeTypes mMimeTypes;

    @Override
    public boolean onCreate() {
        getMimeTypes();
        return true;
    }

    /**
	 * 
	 */
    private void getMimeTypes() {
        MimeTypeParser mtp = new MimeTypeParser();

        XmlResourceParser in = getContext().getResources().getXml(
                R.xml.mimetypes);

        try {
            mMimeTypes = mtp.fromXmlResource(in);
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "PreselectedChannelsActivity: XmlPullParserException", e);
            throw new RuntimeException(
                    "PreselectedChannelsActivity: XmlPullParserException");
        } catch (IOException e) {
            Log.e(LOG_TAG, "PreselectedChannelsActivity: IOException", e);
            throw new RuntimeException(
                    "PreselectedChannelsActivity: IOException");
        }
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        // not supported
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // return file extension (uri.lastIndexOf("."))
        return mMimeTypes.getMimeType(uri.toString());
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        // not supported
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        if (uri.toString().startsWith(CONTENT_URI_STRING)) {
            MatrixCursor c = new MatrixCursor(new String[] { Images.Media.DATA,
                    Images.Media.MIME_TYPE });
            String data = uri.toString().substring(CONTENT_URI_STRING.length());
            String mimeType = mMimeTypes.getMimeType(data);
            c.addRow(new String[] { data, mimeType });
            return c;
        } else {
            throw new RuntimeException("Unsupported uri");
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        if (uri.toString().startsWith(CONTENT_URI_STRING)) {
            int m = ParcelFileDescriptor.MODE_READ_ONLY;
            if (mode.equalsIgnoreCase("rw"))
                m = ParcelFileDescriptor.MODE_READ_WRITE;

            Log.d(LOG_TAG, "Trying to open file : " + uri.toString().substring(
                    CONTENT_URI_STRING.length()));
            File f = new File(uri.toString().substring(
                    CONTENT_URI_STRING.length()));
            try {
                ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, m);
                return pfd;
            } catch(IOException e) {
                return null;
            }
        } else {
            throw new RuntimeException("Unsupported uri");
        }
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
            String[] as) {
        // not supported
        return 0;
    }

    public static Uri getContentUri(File file) {
        return FileManagerProvider.CONTENT_URI.buildUpon()
                .encodedPath(file.getAbsolutePath()).build();
    }

    public static Uri getContentUri(String archive, String entry) {
        return FileManagerProvider.CONTENT_URI.buildUpon().encodedPath(archive)
                .encodedFragment(entry).build();
    }
}
