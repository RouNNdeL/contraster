package com.roundel.contraster;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.ColorInt;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Krzysiek on 2017-04-07.
 */

public class BitmapScaleUtil
{
    private Bitmap bitmap;
    private int targetHeight;
    private int targetWidth;
    private float scale;
    private int offsetX;
    private int offsetY;
    private int scaleDirection; // 0 - > Vertical (y axis), 1 -> Horizontal (x axis)

    private Context context;

    public BitmapScaleUtil(Bitmap bitmap, int targetHeight, int targetWidth)
    {
        this.bitmap = bitmap;
        this.targetHeight = targetHeight;
        this.targetWidth = targetWidth;

        final float scaleY = (float) bitmap.getHeight() / (float) targetHeight;
        final float scaleX = (float) bitmap.getWidth() / (float) targetWidth;
        this.scale = Math.min(scaleY, scaleX);
        this.scaleDirection = scaleY < scaleX ? 0 : 1;

        this.offsetX = (int) Math.floor((Math.round(bitmap.getWidth() / scale) - targetWidth) / 2);
        this.offsetY = (int) Math.floor((Math.round(bitmap.getHeight() / scale) - targetHeight) / 2);

        this.context = context;
    }

    @ColorInt
    public int getPixel(int x, int y)
    {
        return bitmap.getPixel(Math.round(x * scale) + offsetX, Math.round(y * scale) + offsetY);
    }

    @ColorInt
    public int getAverageColor(int centerX, int centerY, int radius)
    {
        return getAverageColor(centerX, centerY, radius, 2);
    }

    @ColorInt
    public int getAverageColor(int centerX, int centerY, int radius, int spacing)
    {
        if(spacing < 1)
            throw new IllegalArgumentException("spacing must not br smaller then 1");

        int x = Math.round((centerX - radius/2) * scale) + offsetX;
        int y = Math.round((centerY - radius/2) * scale) + offsetY;

        radius*=scale;
        int[] pixels = new int[radius * radius];
        int[] test = new int[radius*radius];
        Arrays.fill(test, Color.RED);

        bitmap.getPixels(pixels, 0, radius, x, y, radius, radius);

        Bitmap copy = bitmap.copy(bitmap.getConfig(), true);

        copy.setPixels(test, 0, radius, x, y, radius, radius);

        int r = 0, g = 0, b = 0, n = 0;

        for(int i = 0; i < pixels.length; i += spacing)
        {
            int color = pixels[i];

            r += Color.red(color);
            g += Color.green(color);
            b += Color.blue(color);

            n++;
        }

        return Color.rgb(r / n, g / n, b / n);
    }
}
