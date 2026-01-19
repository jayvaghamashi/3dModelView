package com.example.a3dviewapp.utils;

import android.content.Context;
import android.widget.ImageView;

import com.example.a3dviewapp.R;
import com.squareup.picasso.Picasso;

public class ImageLoader {

    private static ImageLoader instance;
    private Context context;

    private ImageLoader(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized ImageLoader getInstance(Context context) {
        if (instance == null) {
            instance = new ImageLoader(context);
        }
        return instance;
    }

    /**
     * Load image with Picasso
     */
    public void loadImage(String url, ImageView imageView) {
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(R.drawable.placeholder);
            return;
        }

        try {
            Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.ic_error)
                    .fit()
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.placeholder);
        }
    }

    /**
     * Load image with custom placeholder
     */
    public void loadImage(String url, ImageView imageView, int placeholderResId) {
        try {
            Picasso.get()
                    .load(url)
                    .placeholder(placeholderResId)
                    .error(R.drawable.ic_error)
                    .fit()
                    .centerCrop()
                    .into(imageView);
        } catch (Exception e) {
            imageView.setImageResource(placeholderResId);
        }
    }

    /**
     * Cancel image request
     */
    public void cancelRequest(ImageView imageView) {
        try {
            Picasso.get().cancelRequest(imageView);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        try {
            Picasso.get().invalidate("");
        } catch (Exception e) {
            // Ignore
        }
    }
}