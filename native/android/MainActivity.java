package com.bogdan0805.learningapp;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(BitReminderPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
