package com.holenet.cowinfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class RecodeDateAdapter extends BaseExpandableListAdapter {
    private Context context;
    private ArrayList<Parent> items;
    private LayoutInflater inflater = null;

    public RecodeDateAdapter(Context context, ArrayList<Parent> items) {
        this.items = items;
        this.context = context;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setItems(ArrayList<Parent> items) {
        this.items = items;
    }

    @Override
    public View getGroupView(int position, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_recode_date_parent, parent, false);
        }

        TextView tVdate = (TextView) convertView.findViewById(R.id.tVdate);
        TextView tVcount = (TextView) convertView.findViewById(R.id.tVcount);
        int[] date = items.get(position).getDate();
        tVdate.setText(date[0]+"년 "+date[1]+"월 "+date[2]+"일");
        tVcount.setText(String.valueOf(items.get(position).getCount()));

        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_recode_date_child, parent, false);
        }

        TextView tVshortNumber = (TextView) convertView.findViewById(R.id.tVshortNumber);
        TextView tVcontent = (TextView) convertView.findViewById(R.id.tVcontent);
        TextView tVetc = (TextView) convertView.findViewById(R.id.tVetc);
        Child childItem = items.get(groupPosition).getChildAt(childPosition);
        tVshortNumber.setText(childItem.getNumber().split("-")[2]);
        tVcontent.setText(childItem.getContent());
        tVetc.setText(childItem.getEtc());

        return convertView;
    }

    @Override
    public int getGroupCount() {
        return items.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return items.get(groupPosition).getCount();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return items.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return items.get(groupPosition).getChildAt(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public static class Parent {
        private int[] date;
        private ArrayList<Child> children;

        public Parent(int[] date) {
            this.date = date;
            children = new ArrayList<>();
        }

        public int[] getDate() {
            return date;
        }

        public void setDate(int[] date) {
            this.date = date;
        }

        public Child getChildAt(int index) {
            return children.get(index);
        }

        public void addChild(Child child) {
            children.add(child);
        }

        public int getCount() {
            return children.size();
        }

        public boolean isSameDate(Parent p) {
            int[] pDate = p.getDate();
            return date[0]==pDate[0] && date[1]==pDate[1] && date[2]==pDate[2];
        }
    }

    public static class Child {
        private int prop_id;
        private int id;
        private boolean female;
        private String number;
        private String content;
        private String etc;
        private int[] date;

        public Child(int prop_id, int id, boolean female, String number, String content, String etc, int[] date) {
            this.prop_id = prop_id;
            this.id = id;
            this.female = female;
            this.number = number;
            this.content = content;
            this.etc = etc;
            this.date = date;
        }

        public int getProp_id() {
            return prop_id;
        }

        public void setProp_id(int prop_id) {
            this.prop_id = prop_id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public boolean isFemale() {
            return female;
        }

        public void setFemale(boolean female) {
            this.female = female;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
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
    }
}