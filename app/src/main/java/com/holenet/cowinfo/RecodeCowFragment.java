package com.holenet.cowinfo;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import static android.app.Activity.RESULT_OK;
import static com.holenet.cowinfo.RecodeActivity.MENU_DELETE;
import static com.holenet.cowinfo.RecodeActivity.MENU_INFO;
import static com.holenet.cowinfo.RecodeActivity.MENU_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_ADD;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_INFO;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_COW_MODIFY;
import static com.holenet.cowinfo.RecodeActivity.REQUEST_RECODE_MODIFY;

public class RecodeCowFragment extends Fragment {
    protected DatabaseHelper dbHelper;
    boolean created = false;

    ExpandableListView eLVrecode;
    RecodeCowAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        dbHelper = new DatabaseHelper(getContext());
        super.onCreate(savedInstanceState);
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
        eLVrecode.setTag(R.id.FRAG_ID, RecodeActivity.FRAG_COW);
        registerForContextMenu(eLVrecode);

        created = true;
        refresh();

        return v;
    }

    private void refresh() {
        refreshList();
        ((RecodeActivity)getActivity()).rdf.refreshList();
        ((RecodeActivity)getActivity()).getSupportActionBar().setTitle("전체 이력 목록 : "+eLVrecode.getCount()+" 마리");
    }
    protected void refreshList() {
        if(!created)
            return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("select _id, number, female, year, month, day, mnumber from "+DatabaseHelper.propTable, null);
        int recodeCount = c.getCount();

        ArrayList<RecodeCowAdapter.Parent> parents = new ArrayList<>();
        for(int i=0; i<recodeCount; i++) {
            c.moveToNext();
            int id = c.getInt(0);
            String number = c.getString(1);
            boolean female = Boolean.valueOf(c.getString(2));
            int bYear = c.getInt(3);
            int bMonth = c.getInt(4);
            int bDay = c.getInt(5);
            String mnumber = c.getString(6);

            ArrayList<RecodeCowAdapter.Child> children = new ArrayList<>();
            Cursor cc = db.rawQuery("select content, etc, year, month, day, _id from "+DatabaseHelper.detailTable+" where prop_id = "+id, null);
            int recodeCount2 = cc.getCount();
            for(int j=0; j<recodeCount2; j++) {
                cc.moveToNext();
                String content = cc.getString(0);
                String etc = cc.getString(1);
                int year = cc.getInt(2);
                int month = cc.getInt(3);
                int day = cc.getInt(4);
                int iid = cc.getInt(5);
                children.add(new RecodeCowAdapter.Child(id, iid, content, etc, new int[] {year, month, day}));
            }
            Collections.sort(children, new Comparator<RecodeCowAdapter.Child>() {
                @Override
                public int compare(RecodeCowAdapter.Child lhs, RecodeCowAdapter.Child rhs) {
                    int[] lBirth = lhs.getDate();
                    int[] rBirth = rhs.getDate();
                    return ((lBirth[0]*10000+lBirth[1]*100+lBirth[2]) - (rBirth[0]*10000+rBirth[1]*100+rBirth[2]));
                }
            });
            RecodeCowAdapter.Parent parent = new RecodeCowAdapter.Parent(id, number, female, new int[] {bYear, bMonth, bDay}, mnumber, children);
            if(parent.getCount()==0) {
                parent.addChild(null);
            }
            cc.close();
            parents.add(parent);
        }
        c.close();
        db.close();

        Collections.sort(parents, new Comparator<RecodeCowAdapter.Parent>() {
            @Override
            public int compare(RecodeCowAdapter.Parent lhs, RecodeCowAdapter.Parent rhs) {
                int[] lBirth = lhs.getBirthday();
                int[] rBirth = rhs.getBirthday();
                return ((lBirth[0] * 10000 + lBirth[1] * 100 + lBirth[2]) - (rBirth[0] * 10000 + rBirth[1] * 100 + rBirth[2]));
            }
        });

        adapter = new RecodeCowAdapter(getContext(), parents);
        eLVrecode.setAdapter(adapter);
    }

    public void performContextMenuParentClick(int position, final int MENU_ID) {
        final RecodeCowAdapter.Parent parent = (RecodeCowAdapter.Parent) adapter.getGroup(position);
        if(MENU_ID==MENU_INFO || MENU_ID==MENU_MODIFY) {
            Intent intent = new Intent(getContext(), MENU_ID==MENU_INFO ? CowActivity.class : CowEditActivity.class);
            intent.putExtra("id", parent.getId());
            intent.putExtra("requestCode", MENU_ID==MENU_INFO ? REQUEST_COW_INFO : REQUEST_COW_MODIFY);
            startActivityForResult(intent, MENU_ID==MENU_INFO ? REQUEST_COW_INFO : REQUEST_COW_MODIFY);
        } else if(MENU_ID==MENU_DELETE){
            new AlertDialog.Builder(getContext())
                    .setTitle(parent.getNumber())
                    .setMessage("삭제하시겠습니까?")
                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.delete(DatabaseHelper.propTable, "_id="+parent.getId(), null);
                            db.delete(DatabaseHelper.detailTable, "prop_id="+parent.getId(), null);
                            db.close();
                            refresh();
                        }})
                    .setNegativeButton("아니오", null).show();
        }
    }

    public void performContextMenuChildClick(int groupPosition, int childPosition, final int MENU_ID) {
        final RecodeCowAdapter.Child child = (RecodeCowAdapter.Child) adapter.getChild(groupPosition, childPosition);
        if(child==null)
            return;
        if(MENU_ID==MENU_MODIFY) {
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
        } else if(requestCode==REQUEST_COW_ADD || requestCode==REQUEST_COW_MODIFY) {
            if(resultCode==RESULT_OK) {
                refresh();
            }
        }
    }
}
