package com.roundel.contraster;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;

import com.google.android.apps.muzei.api.MuzeiContract;

import java.io.FileNotFoundException;

/**
 * Created by Krzysiek on 2017-04-07.
 */

public class CalculateColorService extends IntentService
{
    private String ZOOPER_VARIABLE_NAME = "C_WIDGET_COLOR";

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
            Palette p = Palette.from(wallpaper).generate();
        }
        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private void updateZooperVariable(String variableValue)
    {
        updateZooperVariable(ZOOPER_VARIABLE_NAME, variableValue);
    }

    private void updateZooperVariable(String variableName, String variableValue)
    {
        Intent intent = new Intent("org.zooper.zw.action.TASKERVAR");
        Bundle bundle = new Bundle();

        bundle.putInt("org.zooper.zw.tasker.var.extra.INT_VERSION_CODE", 1);
        bundle.putString("org.zooper.zw.tasker.var.extra.STRING_VAR", variableName);
        bundle.putString("org.zooper.zw.tasker.var.extra.STRING_TEXT", variableValue);
        intent.putExtra("org.zooper.zw.tasker.var.extra.BUNDLE", bundle);

        this.sendBroadcast(intent);
    }
}
