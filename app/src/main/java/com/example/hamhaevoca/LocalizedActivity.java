package com.example.hamhaevoca;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import java.util.Locale;
/** Locale shared by all screens. Does not alter saved wordbook content. */
public class LocalizedActivity extends Activity {
    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(Lang.wrap(base));
    }
}
