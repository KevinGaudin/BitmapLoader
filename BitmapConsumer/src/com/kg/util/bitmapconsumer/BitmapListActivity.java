package com.kg.util.bitmapconsumer;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class BitmapListActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_bitmap_list, menu);
        return true;
    }
}
