package com.roundel.contraster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Objects;

/**
 * Created by Krzysiek on 2017-04-07.
 */

public class MuzeiChangeReceiver extends BroadcastReceiver
{
    private final String ARTWORK_CHANGED = "com.google.android.apps.muzei.ACTION_ARTWORK_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if(Objects.equals(ARTWORK_CHANGED, intent.getAction()))
        {
            context.startService(new Intent(context, CalculateColorService.class));
        }
    }
}
