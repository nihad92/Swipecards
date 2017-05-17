package com.lorentzos.swipecards;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import java.util.List;

public class CustomAdapter extends BaseAdapter {
  List<String> items = new ArrayList<>();

  @Override public int getCount() {
    return items.size();
  }

  @Override public Object getItem(int i) {
    return items.get(i);
  }

  @Override public long getItemId(int i) {
    return i;
  }

  @Override public View getView(int i, View view, ViewGroup viewGroup) {
    CustomHolder customHolder;
    if(view == null) {
      if(getItemViewType(i) == 0) {
        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item, viewGroup, false);
      } else {
        view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item1, viewGroup, false);
      }
      customHolder = new CustomHolder(view);
      view.setTag(customHolder);
    } else {
      customHolder = (CustomHolder)view.getTag();
    }

    customHolder.bind((String) getItem(i));
    return view;
  }

  @Override public int getItemViewType(int position) {
    return Integer.valueOf((String)getItem(position)).intValue() % 2;
  }

  public void add(String text) {
    items.add(text);
    notifyDataSetChanged();
  }

  public void add(List<String> text) {
    items.addAll(text);
    notifyDataSetChanged();
  }

  public void remove(int position) {
    items.remove(position);
    notifyDataSetChanged();
  }
}
