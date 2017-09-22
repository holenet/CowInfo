package com.holenet.cowinfo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RecodeAdapter extends ArrayAdapter<RecodeAdapter.Item> {
    private ArrayList<Item> arrayItem;
    private Context context;

    public RecodeAdapter(Context context, int resource, ArrayList<Item> objects) {
        super(context, resource, objects);
        this.arrayItem = objects;
        this.context = context;
    }

    public void addItem(Item item) {
        arrayItem.add(item);
    }

    public void setArrayItem(ArrayList<Item> arrayItem) {
        this.arrayItem.clear();
        for(int i=0; i<arrayItem.size(); i++) {
            this.arrayItem.add(arrayItem.get(i));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if(v==null) {
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = li.inflate(R.layout.item_rec, null);
        }

        Item item = arrayItem.get(position);
        if(item!=null) {
            TextView tVcontent = (TextView) v.findViewById(R.id.tVcontent);
            TextView tVetc = (TextView) v.findViewById(R.id.tVetc);
            TextView tVdate = (TextView) v.findViewById(R.id.tVdate);

            if(item.getContent()!="기타")
                tVcontent.setText(item.getContent());
            tVetc.setText(item.getEtc());
            int[] date = item.getDate();
            tVdate.setText(date[0]+"년 "+date[1]+"월 "+date[2]+"일");
        }

        return v;
    }

    public static class Item {
        private int id;
        private String content, etc;
        private int[] date;

        public Item(int id, String content, String etc, int year, int month, int day) {
            this.id = id;
            this.content = content;
            this.etc = etc;
            date = new int[] {year, month, day};
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getEtc() {
            return etc;
        }

        public void setEtc(String etc) {
            this.etc = etc;
        }

        public int[] getDate() {
            return date;
        }

        public void setDate(int[] date) {
            this.date = date;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }
}
