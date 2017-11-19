package com.example.nathanmorgenstern.googlelocationtracking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;


public class LocationInfoAdapter extends ArrayAdapter<LocationInfo>
{
    private int layoutResourceId;
    private ArrayList<LocationInfo> data;

    public LocationInfoAdapter(Context context, int layoutId, ArrayList<LocationInfo> list) {
        super(context, layoutId, list);
        layoutResourceId = layoutId;
        data = list;
    }

    @Override
    public View getView(int index, View row, ViewGroup parent) {

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        row = layoutInflater.inflate(layoutResourceId, null);

        TextView lat = (TextView) row.findViewById(R.id.latitude_text);
        String format1 = String.format("%.4f", Double.parseDouble(data.get(index).getLatitude()));
        lat.setText(format1);

        TextView lon = (TextView) row.findViewById(R.id.longitude_text);
        String format2 = String.format("%.4f", Double.parseDouble(data.get(index).getLongitude()));
        lon.setText(format2);

        TextView time = (TextView) row.findViewById(R.id.time_text);
        time.setText(data.get(index).getTime());

        TextView address = (TextView) row.findViewById(R.id.address_text);
        address.setText(data.get(index).getAddress());

        TextView checkin = (TextView) row.findViewById(R.id.check_in_text);
        checkin.setText(data.get(index).getCheckInName());

        return row;
    }
}
