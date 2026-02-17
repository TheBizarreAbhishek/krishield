package com.krishield.activities;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.krishield.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
