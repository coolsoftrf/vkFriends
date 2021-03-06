package ru.coolsoft.vkfriends.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import ru.coolsoft.vkfriends.loaders.sources.ILoaderSource;

/**
 * Loads an image by the URL passed in arguments, saves it into a local file
 * and provides the resulting file name
 */
public class ImageLoader extends AsyncTaskLoader<String> {
    private final static String TAG = ImageLoader.class.getSimpleName();
    private ILoaderSource mSource;
    private OnDownloadStartedListener mListener;

    //// constructor ////
    public ImageLoader(Context context) {
        super(context);
    }

    //// Overrides of base methods ////
    @Override
    public String loadInBackground() {
        try {
            String imageUrl = mSource.value();

            URL u = new URL(imageUrl);

            File targetFile = targetFile(imageUrl);
            if (!targetFile.exists()) {
                URLConnection connection;
                DataInputStream stream = null;
                ReadableByteChannel in = null;
                FileOutputStream fos = null;
                FileChannel out = null;

                try {
                    File targetPath = targetFile.getParentFile();
                    if (!targetPath.exists() && !targetPath.mkdirs()) {
                        return null;
                    }
                    //ToDo: implement LRU cache

                    connection = u.openConnection();
                    connection.connect();
                    int size = connection.getContentLength();

                    stream = new DataInputStream(connection.getInputStream());
                    in = Channels.newChannel(stream);

                    fos = new FileOutputStream(targetFile);
                    out = fos.getChannel();

                    int count = 0;
                    Log.i(TAG, "downloading '" + imageUrl + "' to '" + targetFile + "'");
                    while ((count /*+= out.transferFrom(in, count, size - count)*/) < size) {
                        count += out.transferFrom(in, count, size - count);
                        //Log.d(TAG, "bytes transferred " + count + " out of " + size);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "error occurred while downloading '" + imageUrl + "' to '" + targetFile + "'", e);
                    return null;
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (fos != null) {
                        fos.flush();
                        fos.close();
                    }
                    if (stream != null) {
                        stream.close();
                    }
                }
            }
            return targetFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onStartLoading() {
        final String imageUrl = mSource == null ? null : mSource.value();
        if (imageUrl == null || imageUrl.equals("")){
            deliverResult(null);
            return;
        }

        final File targetFile = targetFile(imageUrl);
        if (targetFile.exists()){
            /*=== DEBUG DELAY ===*
            if (mListener != null){
                mListener.onDownloadStarted(getId());
            }
            new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                    /*=== PERMANENT CODE START ===*/
                        deliverResult(targetFile.getAbsolutePath());
                    /*=== PERMANENT CODE END ===*
                    }
                }
                , 3000
            );
            /*=== END of DELAY ===*/
            return;
        }

        if (mListener != null){
            mListener.onDownloadStarted(getId());
        }
        forceLoad();
    }

    //// Callback setters////
    public void setOnDownloadStartedListener (OnDownloadStartedListener listener){
        mListener = listener;
    }
    public void setLoaderSource (ILoaderSource source){
        mSource = source;
    }

    //// supplementary methods ////
    private File targetFile(@NonNull String imageUrl){
        final String[] pathParts =  imageUrl.split(File.separator);
        final String fileName = pathParts[pathParts.length - 1];

        File dir = getContext().getExternalCacheDir();
        if (dir == null){
            dir = getContext().getCacheDir();
        }
        return new File(dir, fileName);
    }

    // interface declarations
    public interface OnDownloadStartedListener{
        /**
         * Called in UI thread to indicate that the image loading task will be started soon
         * @param loaderId the ID of the loader which image loading task is going to start
         */
        void onDownloadStarted(int loaderId);
    }
}
