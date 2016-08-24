package com.trekken;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileArrayAdapter extends ArrayAdapter<Item> {

    private Context _c;
    private int _id;
    private List<Item> _items;

    public FileArrayAdapter(Context context, int textViewResourceId, List<Item> objects) {
        super(context, textViewResourceId, objects);
        _c = context;
        _id = textViewResourceId;
        _items = objects;
    }

    public Item getItem(int i) {
        return _items.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) _c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(_id, null);
        }

               /* create a new view of my layout and inflate it in the row */
        //convertView = ( RelativeLayout ) inflater.inflate( resource, null );

        final Item o = _items.get(position);

        if (o != null) {
            TextView t1 = (TextView) v.findViewById(R.id.TextView01);
            TextView t2 = (TextView) v.findViewById(R.id.TextView02);
            TextView t3 = (TextView) v.findViewById(R.id.TextViewDate);
                       /* Take the ImageView from layout and set the city's image */
            ImageView imageCity = (ImageView) v.findViewById(R.id.fd_Icon1);

            String uri = "drawable/" + o.getImage();
            int imageResource = _c.getResources().getIdentifier(uri, null, _c.getPackageName());
            Drawable image = _c.getResources().getDrawable(imageResource);

            imageCity.setImageDrawable(image);

            if (t1 != null)
                t1.setText(o.getName());
            if (t2 != null)
                t2.setText(o.getData());
            if (t3 != null)
                t3.setText(o.getDate());
        }

        return v;
    }
}
