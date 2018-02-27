package com.frank.simplebutterknife;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import simplebutterknife.BindView;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.titleTextView)
    TextView mTitleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SimpleButterKnife.bind(this);
        mTitleTextView.setText("Hello World!");
    }
}
