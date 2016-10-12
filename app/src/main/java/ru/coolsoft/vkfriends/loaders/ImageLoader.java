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

/**
 * Loads an image by the URL passed in arguments, saves it into a local file
 * and provides the resulting file name
 */
public class ImageLoader extends AsyncTaskLoader<String> {
    private final static String TAG = ImageLoader.class.getSimpleName();
    private ILoaderSource mSource;

    public interface OnDownloadStartedListener{
        void onDownloadStarted();
    }
    OnDownloadStartedListener mListener;

    //// constructor ////
    public ImageLoader(Context context, ILoaderSource source) {
        super(context);
        mSource = source;
    }

    //// Overrides of base methods ////
    @Override
    public String loadInBackground() {
        if (mListener != null){
            mListener.onDownloadStarted();
        }
        try {
            String imageUrl = mSource.value();

            URL u = new URL(imageUrl);
            URLConnection connection = u.openConnection();
            connection.connect();
            int size = connection.getContentLength();

            DataInputStream stream = new DataInputStream(connection.getInputStream());
            ReadableByteChannel in = Channels.newChannel(stream);

            File targetFile = targetFile(imageUrl);
            File targetPath = targetFile.getParentFile();
            if (!targetPath.exists() && !targetPath.mkdirs()){
                return null;
            }
            FileOutputStream fos = new FileOutputStream(targetFile);
            FileChannel out = fos.getChannel();

            try {
                int count = 0;
                Log.i(TAG, "downloading '" + imageUrl + "' to '" + targetFile + "'");
                while ((count += out.transferFrom(in, count, size - count)) < size){
                    Log.d(TAG, "bytes transferred " + count + " out of " + size);
                }
            } catch (IOException e) {
                Log.e(TAG, "error occurred while downloading '" + imageUrl + "' to '" + targetFile + "'",  e);
                return null;
            } finally {
                in.close();
                out.close();
                fos.flush();
                fos.close();
            }
            return targetFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onStartLoading() {
        final String imageUrl = mSource.value();
        if (imageUrl == null || imageUrl.equals("")){
            deliverResult(null);
            return;
        }

        File targetFile = targetFile(imageUrl);
        if (targetFile.exists()){
            deliverResult(targetFile.getAbsolutePath());
            return;
        }

        forceLoad();
    }

    //// Specific methods ////
    public void setOnDownloadStartedListener (OnDownloadStartedListener listener){
        mListener = listener;
    }


    //// supplementary methods ////
    private File targetFile(@NonNull String imageUrl){
        final String[] pathParts =  imageUrl.split(File.separator);
        final String fileName = pathParts[pathParts.length - 1];

        return new File(getContext().getFilesDir(), fileName);
    }
}
