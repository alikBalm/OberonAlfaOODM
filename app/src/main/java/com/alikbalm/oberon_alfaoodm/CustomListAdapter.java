package com.alikbalm.oberon_alfaoodm;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String> itemname;
    private final ArrayList<Integer> imgid;

    public CustomListAdapter(Activity context, ArrayList<String> itemname, ArrayList<Integer> imgid) {
        super(context, R.layout.oo_items_list_sample, itemname);
        this.context=context;
        this.itemname=itemname;
        this.imgid=imgid;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.oo_items_list_sample, null,true);


        ImageView imageView = (ImageView) rowView.findViewById(R.id.png);
        TextView extratxt = (TextView) rowView.findViewById(R.id.png_text);

        imageView.setImageResource(imgid.get(position));
        extratxt.setText(itemname.get(position));
        return rowView;

    }
}
