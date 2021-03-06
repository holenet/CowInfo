package com.holenet.cowinfo;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.holenet.cowinfo.RecodeActivity.FRAG_DATE;
import static com.holenet.cowinfo.RecodeActivity.MENU_DELETE;
import static com.holenet.cowinfo.RecodeActivity.MENU_INFO;
import static com.holenet.cowinfo.RecodeActivity.MENU_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_ADD;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_INFO;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_MODIFY;

public class RecodeDateFragment extends Fragment {
    protected DatabaseHelper dbHelper;
    boolean created = false;

    ExpandableListView eLVrecode;
    RecodeDateAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DatabaseHelper(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recode, container, false);

        eLVrecode = (ExpandableListView) v.findViewById(R.id.eLVrecode);
        eLVrecode.setEmptyView(v.findViewById(R.id.tVempty));
        eLVrecode.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
//                eLVrecode.showContextMenuForChild(v);
                performContextMenuChildClick(groupPosition, childPosition, MENU_MODIFY);
                return false;
            }
        });
        eLVrecode.setTag(R.id.FRAG_ID, FRAG_DATE);
        registerForContextMenu(eLVrecode);

        created = true;
        refresh();

        return v;
    }

    private void refresh() {
        refreshList();
        ((RecodeActivity)getActivity()).rcf.refreshList();
    }
    protected void refreshList() {
        if(!created)
            return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("select _id, prop_id, content, etc, year, month, day from " + DatabaseHelper.detailTable, null);
        int recodeCount = c.getCount();

        ArrayList<RecodeDateAdapter.Parent> parents = new ArrayList<>();
        for (int i = 0; i < recodeCount; i++) {
            c.moveToNext();
            int id = c.getInt(0);
            int prop_id = c.getInt(1);
            Cursor cc = db.rawQuery("select number, female from " + DatabaseHelper.propTable + " where _id = " + prop_id, null);
            cc.moveToNext();
            String number = cc.getString(0);
            boolean female = Boolean.valueOf(cc.getString(1));
            cc.close();
            String content = c.getString(2);
            String etc = c.getString(3);
            int year = c.getInt(4);
            int month = c.getInt(5);
            int day = c.getInt(6);
            int[] date = new int[]{year, month, day};
            RecodeDateAdapter.Parent parent = new RecodeDateAdapter.Parent(date);
            RecodeDateAdapter.Child child = new RecodeDateAdapter.Child(prop_id, id, female, number, content, etc, date);
            int j;
            for (j = 0; j < parents.size(); j++) {
                if (parents.get(j).isSameDate(parent)) {
                    parents.get(j).addChild(child);
                    break;
                }
            }
            if (j == parents.size()) {
                parent.addChild(child);
                parents.add(parent);
            }
        }
        c.close();
        db.close();

        Collections.sort(parents, new Comparator<RecodeDateAdapter.Parent>() {
            @Override
            public int compare(RecodeDateAdapter.Parent lhs, RecodeDateAdapter.Parent rhs) {
                int[] lBirth = lhs.getDate();
                int[] rBirth = rhs.getDate();
                return ((lBirth[0] * 10000 + lBirth[1] * 100 + lBirth[2]) - (rBirth[0] * 10000 + rBirth[1] * 100 + rBirth[2]));
            }
        });

        Calendar now = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();
        int indexTomorrow = -1;
        for (int i = 0; i < parents.size(); i++) {
            int[] date = parents.get(i).getDate();
            calendar.set(date[0], date[1] - 1, date[2]);
            if (calendar.after(now)) {
                indexTomorrow = i;
                break;
            }
        }

        adapter = new RecodeDateAdapter(getContext(), parents);
        eLVrecode.setAdapter(adapter);
        if (indexTomorrow != -1) {
            eLVrecode.expandGroup(indexTomorrow);
            eLVrecode.setSelection(indexTomorrow > 0 ? indexTomorrow - 1 : indexTomorrow);
        }
    }

    public void performContextMenuChildClick(int groupPosition, int childPosition, final int MENU_ID) {
        final RecodeDateAdapter.Child child = (RecodeDateAdapter.Child) adapter.getChild(groupPosition, childPosition);
        if(child==null)
            return;
        if(MENU_ID==MENU_INFO) {
            Intent intent = new Intent(getContext(), CowActivity.class);
            intent.putExtra("id", child.getProp_id());
            intent.putExtra("requestCode", REQUEST_COW_INFO);
            startActivityForResult(intent, REQUEST_COW_INFO);
        } else if(MENU_ID==MENU_MODIFY) {
            Intent intent = new Intent(getContext(), RecodeEditActivity.class);
            intent.putExtra("id", child.getId());
            intent.putExtra("prop_id", child.getProp_id());
            intent.putExtra("requestCode", REQUEST_RECODE_MODIFY);
            startActivityForResult(intent, REQUEST_RECODE_MODIFY);
        } else if(MENU_ID==MENU_DELETE) {
            new AlertDialog.Builder(getContext())
                    .setTitle(child.getContent()+" "+child.getDate()[0]+"년 "+child.getDate()[1]+"월 "+child.getDate()[2]+"일")
                    .setMessage("삭제하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete(DatabaseHelper.detailTable, "_id="+child.getId(), null);
                            db.close();
                            refresh();
                        }})
                    .setNegativeButton("아니오", null).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_RECODE_MODIFY) {
            if(resultCode==RESULT_OK) {
                refresh();
            }
        } else if(requestCode==REQUEST_COW_INFO) {
            if(resultCode==RESULT_OK) {
                refresh();
            }
        }
    }
}