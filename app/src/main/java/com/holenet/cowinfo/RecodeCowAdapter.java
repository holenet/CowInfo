package com.holenet.cowinfo;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.holenet.cowinfo.R;
import com.holenet.cowinfo.RecodeActivity;

import java.util.ArrayList;

public class RecodeCowAdapter extends BaseExpandableListAdapter {
    private Context context;
    private ArrayList<Parent> items;
    private LayoutInflater inflater = null;

    public RecodeCowAdapter(Context context, ArrayList<Parent> items) {
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
            convertView = inflater.inflate(R.layout.item_recode_cow_parent, parent, false);
        }

        Parent item = items.get(position);
        TextView tVshortNumber = (TextView) convertView.findViewById(R.id.tVshortNumber);
        TextView tVnumber = (TextView) convertView.findViewById(R.id.tVnumber);
        TextView tVBirthday = (TextView) convertView.findViewById(R.id.tVbirthday);
        TextView tVcount = (TextView) convertView.findViewById(R.id.tVcount);
        tVshortNumber.setText(item.getNumber().split("-")[2]+" "+(item.isFemale() ? "♀" : "♂"));
        tVnumber.setText(item.getNumber());
        int[] date = item.getBirthday();
        tVBirthday.setText(date[0]+"년 "+date[1]+"월 "+date[2]+"일");
        tVcount.setText(String.valueOf(getChild(position, 0)==null ? "0" : items.get(position).getCount()));

        convertView.setTag(R.id.FRAG_ID, RecodeActivity.FRAG_COW);

        return convertView;
    }


    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        Child childItem = items.get(groupPosition).getChildAt(childPosition);
        if(childItem==null) {
            convertView = inflater.inflate(R.layout.view_empty, parent, false);
            convertView.setTag("null");
            return convertView;
        }
        if(convertView==null || convertView.getTag()==null || convertView.getTag().equals("null")) {
            convertView = inflater.inflate(R.layout.item_recode_cow_child, parent, false);
        }

        TextView tVcontent = (TextView) convertView.findViewById(R.id.tVcontent);
        TextView tVetc = (TextView) convertView.findViewById(R.id.tVetc);
        TextView tVdate = (TextView) convertView.findViewById(R.id.tVdate);

        tVcontent.setText(childItem.getContent());
        tVetc.setText(childItem.getEtc());
        int[] date = childItem.getDate();
        tVdate.setText(date[0]+"년 "+date[1]+"월 "+date[2]+"일");

        convertView.setTag(R.id.FRAG_ID, RecodeActivity.FRAG_COW);

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

    public int getCount() {
        return items.size();
    }

    public static class Parent {
        private int id;
        private String number;
        private boolean female;
        private int[] birthday;
        private String mnumber;
        private ArrayList<Child> children;

        public Parent(int id, String number, boolean female, int[] birthday, String mnumber, ArrayList<Child> children) {
            this.id = id;
            this.number = number;
            this.female = female;
            this.birthday = birthday;
            this.mnumber = mnumber;
            this.children = children;
        }

        public void addChild(Child child) {
            children.add(child);
        }

        public Child getChildAt(int index) {
            return children.get(index);
        }

        public int getCount() {
            return children.size();
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public boolean isFemale() {
            return female;
        }

        public void setFemale(boolean female) {
            this.female = female;
        }

        public int[] getBirthday() {
            return birthday;
        }

        public void setBirthday(int[] birthday) {
            this.birthday = birthday;
        }

        public String getMotherNumber() {
            return mnumber;
        }

        public void setMotherNumber(String mnumber) {
            this.mnumber = mnumber;
        }

        public ArrayList<Child> getChildren() {
            return children;
        }

        public void setChildren(ArrayList<Child> children) {
            this.children = children;
        }
    }

    public static class Child {
        private int prop_id;
        private int id;
        private String content;
        private String etc;
        private int[] date;

        public Child(int prop_id, int id, String content, String etc, int[] date) {
            this.prop_id = prop_id;
            this.id = id;
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