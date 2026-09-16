package com.kamal.jarvis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(
            Context context,
            Intent intent
    ) {

        String title =
                intent.getStringExtra(
                        "reminder_title"
                );

        if (title == null ||
                title.trim().isEmpty()) {

            title = "عندك تذكير من JARVIS";
        }

        Toast.makeText(
                context,
                "JARVIS 🔔\n" + title,
                Toast.LENGTH_LONG
        ).show();
    }
}