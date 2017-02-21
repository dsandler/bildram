package org.dsandler.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;


public class Instagram {
    public String URI_BASE = "https://www.instagram.com";
    public String URI_PATH_USER = "/%s";
    public String MAGIC = "/?__a=1";

    private final Context context;
    private final RequestQueue rq;
    private final ImageLoader imageloader;

    private static Instagram instance;
    private int mSpinnerResId;

    public static synchronized Instagram getInstance(Context ctx) {
        if (instance != null) return instance;
        return (instance = new Instagram(ctx.getApplicationContext()));
    }

    private Instagram(Context ctx) {
        context = ctx;
        rq = Volley.newRequestQueue(ctx);
        imageloader = new ImageLoader(rq, new BasicImageCache());
    }

    public void fetchUserPictureList(String user, int max, Response.Listener<List<String>> handler) {
        String uri = URI_BASE
            + String.format(URI_PATH_USER, user)
            + MAGIC;
        Log.v("Bildram", "Requesting: " + uri.toString());
        rq.add(new JsonObjectRequest(
                Request.Method.GET,
                uri,
                null,
                response -> {
                    try {
                        JSONArray media = response.getJSONObject("user")
                                .getJSONObject("media")
                                .getJSONArray("nodes");
                        final int N = media.length();
                        ArrayList<String> results = new ArrayList<>(N);
                        for (int i=0; i<N; i++) {
                            final String imageUri = media.getJSONObject(i).getString("display_src");
                            Log.v("Bildram", imageUri);
                            results.add(i, imageUri);
                        }
                        handler.onResponse(results);
                    } catch (JSONException e) {
                        Log.e("Bildram", "JSON error: ", e);
                    }
                },
                null));
    }

    public void fetchPicture(String url, ImageView imageView, int fadeDuration) {
        if (mSpinnerResId == 0) {
            TypedArray arr = context.getTheme().obtainStyledAttributes(
                    new int[]{android.R.attr.progressBarStyleLarge});
            final int style = arr.getResourceId(0, 0);
            arr.recycle();
            arr = context.getTheme().obtainStyledAttributes(style, new int[]{android.R.attr.indeterminateDrawable});
            mSpinnerResId = arr.getResourceId(0, 0);
            arr.recycle();
        }
        Log.v("Bildram", "fetching: " + url);
        imageloader.get(url, getFancyImageListener(imageView, mSpinnerResId,
                android.R.drawable.stat_notify_error, fadeDuration));
    }

    private static class BasicImageCache implements ImageLoader.ImageCache {
        private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

        @Override
        public Bitmap getBitmap(String url) {
            return cache.get(url);
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            cache.put(url, bitmap);
        }
    }

    public static ImageLoader.ImageListener getFancyImageListener(
                ImageView view, int defaultImageResId, int errorImageResId, int fadeDuration) {
        return new ImageLoader.ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (errorImageResId != 0) {
                    view.setImageResource(errorImageResId);
                }
            }

            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                view.animate().alpha(0f).setDuration(fadeDuration).withEndAction(
                        () -> {
                            if (response.getBitmap() != null) {
                                view.setImageBitmap(response.getBitmap());
                            } else if (defaultImageResId != 0) {
                                view.setImageResource(defaultImageResId);
                            }
                            view.animate().alpha(1f).setDuration(fadeDuration);
                        });
            }
        };
    }
}
