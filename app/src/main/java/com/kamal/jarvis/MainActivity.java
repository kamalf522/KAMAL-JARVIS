package com.kamal.jarvis;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    TextView output;
    EditText commandInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(30, 40, 30, 30);
        main.setBackgroundColor(Color.rgb(8, 12, 24));

        TextView title = new TextView(this);
        title.setText("KAMAL JARVIS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 20);

        TextView status = new TextView(this);
        status.setText("● JARVIS ONLINE");
        status.setTextColor(Color.rgb(100, 255, 150));
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 10, 0, 25);

        output = new TextView(this);
        output.setText("JARVIS:\nأنا جاهز. أعطني أمراً.");
        output.setTextColor(Color.WHITE);
        output.setTextSize(17);
        output.setPadding(20, 20, 20, 20);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);

        commandInput = new EditText(this);
        commandInput.setHint("اكتب أمرك هنا...");
        commandInput.setHintTextColor(Color.GRAY);
        commandInput.setTextColor(Color.WHITE);
        commandInput.setTextSize(16);
        commandInput.setSingleLine(true);

        Button sendButton = new Button(this);
        sendButton.setText("تنفيذ الأمر");

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeCommand();
            }
        });

        main.addView(title);
        main.addView(status);

        main.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        main.addView(commandInput);
        main.addView(sendButton);

        setContentView(main);
    }

    private void executeCommand() {

        String command = commandInput.getText().toString().trim();

        if (command.isEmpty()) {
            return;
        }

        output.setText(
                "أنت:\n" + command +
                "\n\nJARVIS:\nتم استقبال الأمر. النظام جاهز لتطوير هذا الأمر."
        );

        commandInput.setText("");
    }
}