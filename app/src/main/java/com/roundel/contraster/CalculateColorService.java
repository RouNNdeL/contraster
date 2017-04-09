package com.roundel.contraster;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.apps.muzei.api.MuzeiContract;

import java.io.FileNotFoundException;

/**
 * Created by Krzysiek on 2017-04-07.
 */

public class CalculateColorService extends IntentService
{
    private final int COLOR_VIBRANT = 0;
    private final int COLOR_DARK_VIBRANT = 1;
    private final int COLOR_LIGHT_VIBRANT = 2;
    private final int COLOR_MUTED = 3;
    private final int COLOR_DARK_MUTED = 4;
    private final int COLOR_LIGHT_MUTED = 5;

    private final String ZOOPER_BG_COLOR = "C_BG_COLOR";
    private final String ZOOPER_TEXT_COLOR = "C_TEXT_COLOR";

    private final float CONTRAST_HUE_SATURATION_THRESHOLD = 15.0f;
    private float BG_LUMINESCENCE_THRESHOLD = 0.1f;
    private final float LUMINESCENCE_THRESHOLD_LIGHT = 0.715f;
    private final float LUMINESCENCE_THRESHOLD_DARK = 0.615f;

    private final float HUE_MULTIPLIER = 1 / 30f;
    private float CONTRAST_MULTIPLIER = 0.8f;
    private final float SATURATION_MULTIPLIER = 4f;

    private int[] priorities = {COLOR_VIBRANT, COLOR_DARK_VIBRANT, COLOR_LIGHT_VIBRANT, COLOR_MUTED, COLOR_DARK_MUTED, COLOR_LIGHT_MUTED,};

    private int lightTextColor = 0xFFFFFFFF;
    private int darkTextColor = 0x8A000000;

    /**
     * {@inheritDoc}
     */
    public CalculateColorService(String name)
    {
        super(name);
    }

    public CalculateColorService()
    {
        super("CalculateColorService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent)
    {
        try
        {
            Bitmap wallpaper = MuzeiContract.Artwork.getCurrentArtworkBitmap(this);
            if(wallpaper == null || wallpaper.isRecycled())
                return;
            Palette palette = Palette.from(wallpaper).maximumColorCount(48).generate();

            @ColorInt int backgroundColor = getBackgroundColor(wallpaper, 75, 675, 1525);
            @ColorInt int bestColor = getBestColor(palette, backgroundColor);
            @ColorInt int textColor = getTextColor(bestColor, backgroundColor);

            Log.d("CalculateColorService", "BestColor: " + String.format("#%06X", (0xFFFFFF & bestColor)) + " BackgroundColor: " + String.format("#%06X", (0xFFFFFF & backgroundColor)));

            //TODO: Calculate contrast and chose an appropriate text color
            updateZooperVariable(ZOOPER_BG_COLOR, bestColor);
            updateZooperVariable(ZOOPER_TEXT_COLOR, textColor);
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    @ColorInt
    private int getBackgroundColor(Bitmap bitmap, int radius, int targetX, int targetY)
    {
        return getBackgroundColor(bitmap, radius, radius, targetX, targetY);
    }

    @ColorInt
    private int getBackgroundColor(Bitmap bitmap, int radiusX, int radiusY, int targetX, int targetY)
    {
        WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = window.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return getBackgroundColor(bitmap, size.x, size.y, radiusX, radiusY, targetX, targetY);
    }

    @ColorInt
    private int getBackgroundColor(Bitmap bitmap, int deviceWidth, int deviceHeight, int radius, int targetX, int targetY)
    {
        return getBackgroundColor(bitmap, deviceWidth, deviceHeight, radius, radius, targetX, targetY);
    }

    @ColorInt
    private int getBackgroundColor(Bitmap bitmap, int targetWidth, int targetHeight, int radiusX, int radiusY, int targetX, int targetY)
    {
        final float scaleY = (float) bitmap.getHeight() / (float) targetHeight;
        final float scaleX = (float) bitmap.getWidth() / (float) targetWidth;
        final float scale = Math.min(scaleY, scaleX);

        final int offsetX = (int) Math.floor((Math.round(bitmap.getWidth() / scale) - targetWidth) / 2);
        final int offsetY = (int) Math.floor((Math.round(bitmap.getHeight() / scale) - targetHeight) / 2);

        final int x = offsetX + (targetX - radiusX);
        final int y = offsetY + (targetY - radiusY);

        Bitmap scaledWallpaper = Bitmap.createScaledBitmap(
                bitmap,
                (int) (bitmap.getWidth() / scale),
                (int) (bitmap.getHeight() / scale),
                false
        );
        bitmap.recycle();

        //Use this only for debugging purposes,
        // as it creates a copy of the original bitmap in the memory
        /*int[] test = new int[radiusX * radiusY * 4];
        Arrays.fill(test, Color.RED);
        Bitmap copy = scaledWallpaper.copy(scaledWallpaper.getConfig(), true);
        copy.setPixels(test, 0, radiusX * 2, x, y, radiusX * 2, radiusY * 2);*/

        final int dominantColor = Palette.from(scaledWallpaper).setRegion(x, y, x + radiusX * 2, y + radiusY * 2).generate().getDominantColor(-1);
        if(dominantColor == -1)
        {
            int[] pixels = new int[radiusX * radiusY * 4];
            scaledWallpaper.getPixels(pixels, 0, radiusX * 2, x, y, radiusX * 2, radiusY * 2);

            int r = 0, g = 0, b = 0, n = 0;
            for(int color : pixels)
            {
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);

                n++;
            }

            return Color.rgb(r / n, g / n, b / n);
        }
        return dominantColor;
    }

    @ColorInt
    private int getBestColor(Palette palette, @ColorInt int background)
    {
        int bestColor = -1;
        float bestScore = -1;
        for(int type : priorities)
        {
            int color = -1;
            switch(type)
            {
                case COLOR_VIBRANT:
                    color = palette.getVibrantColor(-1);
                    break;
                case COLOR_LIGHT_VIBRANT:
                    color = palette.getLightVibrantColor(-1);
                    break;
                case COLOR_DARK_VIBRANT:
                    color = palette.getDarkVibrantColor(-1);
                    break;
                case COLOR_MUTED:
                    color = palette.getMutedColor(-1);
                    break;
                case COLOR_LIGHT_MUTED:
                    color = palette.getLightMutedColor(-1);
                    break;
                case COLOR_DARK_MUTED:
                    color = palette.getDarkMutedColor(-1);
                    break;
            }
            if(color == -1)
                continue;

            float contrast = getContrast(color, background);
            float hue = getHueDifference(color, background);
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            float saturation = hsv[1];

            final float score = contrast * CONTRAST_MULTIPLIER + (saturation > 0.2f ? hue * HUE_MULTIPLIER : hue * HUE_MULTIPLIER * 0.1f) + saturation * SATURATION_MULTIPLIER;
            Log.d("CalculateColorService", type + ": " + String.format("#%06X", (0xFFFFFF & color)) + " -> " +
                    contrast * CONTRAST_MULTIPLIER + " + " + (saturation > 0.2f ? hue * HUE_MULTIPLIER : hue * HUE_MULTIPLIER * 0.1f) + " + " + saturation * SATURATION_MULTIPLIER +  " = " + score);
            if(score > CONTRAST_HUE_SATURATION_THRESHOLD && background != color)
                return color;
            if(score > bestScore && background != color)
            {
                bestScore = score;
                bestColor = color;
            }
        }
        return bestColor;
    }

    private float getContrast(@ColorInt int color1, @ColorInt int color2)
    {
        float l1 = getLuminescence(color1);
        float l2 = getLuminescence(color2);

        return (float) (l1 > l2 ? (l1 + 0.05) / (l2 + 0.05) : (l2 + 0.05) / (l1 + 0.05));
    }

    private float getLuminescence(@ColorInt int color)
    {
        return getLuminescence(Color.red(color), Color.green(color), Color.blue(color));
    }

    private float getLuminescence(int r, int g, int b)
    {
        double rg;
        double gg;
        double bg;
        if(r <= 10)
            rg = r / 3294;
        else
            rg = Math.pow((r / 269.0 + 0.0513), 2.4);

        if(g <= 10)
            gg = g / 3294;
        else
            gg = Math.pow((g / 269.0 + 0.0513), 2.4);

        if(b <= 10)
            bg = b / 3294;
        else
            bg = Math.pow((b / 269.0 + 0.0513), 2.4);
        return (float) (0.2126 * rg + 0.7152 * gg + 0.0722 * bg);
    }

    private int getHueDifference(@ColorInt int color1, @ColorInt int color2)
    {
        final int distance = Math.abs(getHue(color1) - getHue(color2));
        if(distance > 180)
            return 360 - distance;
        return distance;
    }

    private int getHue(@ColorInt int color)
    {
        return getHue(Color.red(color), Color.green(color), Color.blue(color));
    }

    private int getHue(int r, int g, int b)
    {

        float min = Math.min(Math.min(r, g), b);
        float max = Math.max(Math.max(r, g), b);

        float hue;
        if(max == r)
        {
            hue = (g - b) / (max - min);

        }
        else if(max == g)
        {
            hue = 2f + (b - r) / (max - min);

        }
        else
        {
            hue = 4f + (r - g) / (max - min);
        }

        hue = hue * 60;
        if(hue < 0) hue = hue + 360;

        return Math.round(hue);
    }

    @ColorInt
    private int getTextColor(@ColorInt int bestColor, @ColorInt int backgroundColor)
    {
        float luminescence = getLuminescence(bestColor);
        float bgLuminescence = getLuminescence(backgroundColor);
        Log.d("CalculateColorService", "luminescence: "+luminescence+" bg_lumi: "+bgLuminescence);
        if(bgLuminescence < BG_LUMINESCENCE_THRESHOLD)
        {
            if(luminescence > LUMINESCENCE_THRESHOLD_DARK)
                return darkTextColor;
            return lightTextColor;
        }
        else
        {
            if(luminescence > LUMINESCENCE_THRESHOLD_LIGHT)
                return darkTextColor;
            return lightTextColor;
        }
    }

    private void updateZooperVariable(String variableName, @ColorInt int color)
    {
        updateZooperVariable(variableName, "#" + Integer.toHexString(color));
    }

    private void updateZooperVariable(String variableName, String variableValue)
    {
        Intent intent = new Intent("org.zooper.zw.action.TASKERVAR");
        Bundle bundle = new Bundle();

        bundle.putInt("org.zooper.zw.tasker.var.extra.INT_VERSION_CODE", 1);
        bundle.putString("org.zooper.zw.tasker.var.extra.STRING_VAR", variableName);
        bundle.putString("org.zooper.zw.tasker.var.extra.STRING_TEXT", variableValue);
        intent.putExtra("org.zooper.zw.tasker.var.extra.BUNDLE", bundle);

        Log.d("CalculateColorService", "Sending " + variableName + ": " + variableValue);

        this.sendBroadcast(intent);
    }
}
