package com.example.pharmamapapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.pharmamapapp.appwrite.AppwriteManager;

import java.util.LinkedHashMap;
import java.util.Map;

public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final int MAX_CACHE = 50;

    private static final Map<String, Bitmap> cache = new LinkedHashMap<String, Bitmap>(MAX_CACHE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
            return size() > MAX_CACHE;
        }
    };

    public static void load(String fileId, ImageView imageView, @Nullable View placeholder) {
        if (fileId == null || fileId.isEmpty()) {
            imageView.setVisibility(View.GONE);
            if (placeholder != null) placeholder.setVisibility(View.VISIBLE);
            return;
        }

        Log.d(TAG, "Loading image: " + fileId);

        Bitmap cached = cache.get(fileId);
        if (cached != null) {
            Log.d(TAG, "Cache hit: " + fileId);
            imageView.setImageBitmap(cached);
            imageView.setVisibility(View.VISIBLE);
            if (placeholder != null) placeholder.setVisibility(View.GONE);
            return;
        }

        final String tag = fileId;
        imageView.setTag(tag);

        AppwriteManager.INSTANCE.downloadFile(fileId,
                new AppwriteManager.FileDownloadCallback() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Log.d(TAG, "Downloaded " + bytes.length + " bytes for " + fileId);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            cache.put(fileId, bitmap);
                            imageView.post(() -> {
                                if (tag.equals(imageView.getTag())) {
                                    imageView.setImageBitmap(bitmap);
                                    imageView.setVisibility(View.VISIBLE);
                                    if (placeholder != null) placeholder.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            Log.e(TAG, "Failed to decode bitmap for " + fileId);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Download error for " + fileId + ": " + message);
                    }
                });
    }
}
