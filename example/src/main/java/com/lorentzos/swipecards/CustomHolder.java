package com.lorentzos.swipecards;

import android.view.View;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

public class CustomHolder {
  @InjectView(R.id.helloText) TextView textView;
  public CustomHolder(View view) {
    ButterKnife.inject(this, view);
  }

  public void bind(String text) {
    textView.setText(text);
  }
}
