package com.holenet.cowinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;

public class RecodeActivity extends AppCompatActivity {
    final static int REQUEST_COW_INFO = 300;
    final static int REQUEST_COW_ADD = 301;
    final static int REQUEST_COW_MODIFY = 302;
    final static int REQUEST_RECODE_ADD = 404;
    final static int REQUEST_RECODE_MODIFY = 405;
    final static int REQUEST_BACKUP = 501;
    final static int MENU_INFO = 1000;
    final static int MENU_MODIFY = 1001;
    final static int MENU_DELETE = 1002;
    public final static int FRAG_COW = 2001;
    public final static int FRAG_DATE = 2002;

    RecodeCowFragment rcf;
    RecodeDateFragment rdf;

    private PagerAdapter adapter;
    private ViewPager vPrecode;

    View lLcontent;
    View pBloading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recode);

        getSupportActionBar().setTitle("전체 이력 목록");

        File f = getExternalFilesDir(null);
        if (!f.exists())
            f.mkdirs();

        rcf = new RecodeCowFragment();
        rdf = new RecodeDateFragment();

        adapter = new PagerAdapter(getSupportFragmentManager());

        vPrecode = (ViewPager) findViewById(R.id.vPrecode);
        vPrecode.setAdapter(adapter);

        lLcontent = findViewById(R.id.lLcontent);
        pBloading = findViewById(R.id.pBloading);

        serviceAlarm(false);

        if(getIntent().getBooleanExtra("notification", false)) {
            getIntent().putExtra("notification", false);
            vPrecode.setCurrentItem(1);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        Object fragIdTag = v.getTag(R.id.FRAG_ID);
        if(fragIdTag==null)
            return;
        int fragId = (int) fragIdTag;
        long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo)menuInfo).packedPosition;
        int type = ExpandableListView.getPackedPositionType(packedPosition);

        if(type==ExpandableListView.PACKED_POSITION_TYPE_NULL)
            return;
        if(fragId==FRAG_COW) {
            if(type==ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                RecodeCowAdapter.Parent item = (RecodeCowAdapter.Parent) rcf.adapter.getGroup(ExpandableListView.getPackedPositionGroup(packedPosition));
                Intent intent = new Intent(this, CowActivity.class);
                intent.putExtra("id", item.getId());
                intent.putExtra("requestCode", REQUEST_COW_INFO);
                startActivityForResult(intent, REQUEST_COW_INFO);
/*
                menu.add(FRAG_COW, MENU_INFO, Menu.NONE, "상세정보");
                menu.add(FRAG_COW, MENU_MODIFY, Menu.NONE, "수정");
                menu.add(FRAG_COW, MENU_DELETE, Menu.NONE, "삭제");
*/
            } else if(type==ExpandableListView.PACKED_POSITION_TYPE_CHILD && rcf.adapter.getChild(ExpandableListView.getPackedPositionGroup(packedPosition), ExpandableListView.getPackedPositionChild(packedPosition))!=null) {
                menu.add(FRAG_COW, MENU_MODIFY, Menu.NONE, "이력 수정");
                menu.add(FRAG_COW, MENU_DELETE, Menu.NONE, "이력 삭제");
            }
        } else if(fragId==FRAG_DATE) {
            if(type==ExpandableListView.PACKED_POSITION_TYPE_GROUP)
                return;
            if(type==ExpandableListView.PACKED_POSITION_TYPE_CHILD && rdf.adapter.getChild(ExpandableListView.getPackedPositionGroup(packedPosition), ExpandableListView.getPackedPositionChild(packedPosition))!=null) {
                menu.add(FRAG_DATE, MENU_INFO, Menu.NONE, "개체 상세정보");
                menu.add(FRAG_DATE, MENU_MODIFY, Menu.NONE, "이력 수정");
                menu.add(FRAG_DATE, MENU_DELETE, Menu.NONE, "이력 삭제");
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int groupId = item.getGroupId();
        int itemId = item.getItemId();
        long packedPosition = ((ExpandableListView.ExpandableListContextMenuInfo)item.getMenuInfo()).packedPosition;
        int type = ExpandableListView.getPackedPositionType(packedPosition);
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
        int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);
        if(groupId==FRAG_COW) {
            if(type==ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
//                rcf.performContextMenuParentClick(groupPosition, itemId);
            } else if(type==ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
               rcf.performContextMenuChildClick(groupPosition, childPosition, itemId);
            }
        } else if(groupId==FRAG_DATE) {
            if(type==ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
            } else if(type==ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                rdf.performContextMenuChildClick(groupPosition, childPosition, itemId);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_COW_ADD || requestCode==REQUEST_COW_INFO) {
            if(resultCode==RESULT_OK) {
                rcf.refreshList();
                rdf.refreshList();
                vPrecode.setCurrentItem(0);
            }
        } else if(requestCode==REQUEST_BACKUP) {
            rcf.refreshList();
            rdf.refreshList();
        }
    }

    private void serviceAlarm(boolean restart) {
        Intent intent = new Intent(this, AlarmService.class);

        if(getSharedPreferences("settings", 0).getBoolean("notice", true)) {
            boolean isRunning = false;
            if(restart) {
                stopService(intent);
            } else {
                ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if(AlarmService.class.getName().equals(service.service.getClassName())) {
//                    Toast.makeText(this, AlarmService.class.getName()+" : "+service.service.getClassName(), Toast.LENGTH_SHORT).show();
                        isRunning = true;
                    }
                }
            }
            if(!isRunning)
                startService(intent);
        } else {
            stopService(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        if (id == R.id.action_settings) {
            final SharedPreferences pref = getSharedPreferences("settings", 0);
            SharedPreferences.Editor editor = pref.edit();

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                Toast.makeText(RecodeActivity.this, "안드로이드 버전이 낮아 알림 기능이 지원되지 않습니다.", Toast.LENGTH_LONG).show();
                editor.putBoolean("notice", false);
                editor.apply();
            }

            final LinearLayout linear = (LinearLayout)(((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_settings, null));
            final Switch sTnotice = (Switch) linear.findViewById(R.id.sTnotice);
            final EditText eTtime = (EditText) linear.findViewById(R.id.eTtime);
            final Switch sTsound = (Switch) linear.findViewById(R.id.sTsound);
            sTnotice.setChecked(pref.getBoolean("notice", true));
            sTnotice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    eTtime.setEnabled(isChecked);
                    sTsound.setEnabled(isChecked);
                }
            });
            eTtime.setEnabled(sTnotice.isChecked());
            eTtime.setText(String.valueOf(pref.getInt("notice_time", 9)));
            sTsound.setEnabled(sTnotice.isChecked());
            sTsound.setChecked(pref.getBoolean("notice_sound", true));

            new AlertDialog.Builder(this)
                    .setTitle("설정")
                    .setView(linear)
                    .setPositiveButton("저장", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = pref.edit();
                            if(sTnotice.isChecked()) {
                                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                    Toast.makeText(RecodeActivity.this, "안드로이드 버전이 낮아 알림 기능이 지원되지 않습니다.", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                editor.putBoolean("notice", true);
                                int time;
                                try {
                                    time = Integer.valueOf(eTtime.getText().toString());
                                } catch (Exception e) {
                                    Toast.makeText(RecodeActivity.this, "0~23 의 수만 입력하세요.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if(time>=24) {
                                    Toast.makeText(RecodeActivity.this, "0~23 의 수만 입력하세요.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                editor.putInt("notice_time", time);
                                editor.putBoolean("notice_sound", sTsound.isChecked());
                                Toast.makeText(getApplicationContext(), "전날 "+time+"시에 알림이 "+(sTsound.isChecked()?"울립니다.":"뜹니다."), Toast.LENGTH_LONG).show();
                            } else {
                                editor.putBoolean("notice", false);
                                Toast.makeText(RecodeActivity.this, "알림이 뜨지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                            editor.apply();
                            serviceAlarm(true);
                        }
                    })
                    .setNegativeButton("취소", null)
                    .create().show();
        } else if(id==R.id.action_cow_add) {
            Intent intent = new Intent(getApplicationContext(), CowEditActivity.class);
            intent.putExtra("requestCode", REQUEST_COW_ADD);
            startActivityForResult(intent, REQUEST_COW_ADD);
        } else if(id==R.id.action_backup) {
            Intent intent = new Intent(RecodeActivity.this, BackupActivity.class);
            startActivityForResult(intent, REQUEST_BACKUP);
        }

        return super.onOptionsItemSelected(item);
    }

    public class PagerAdapter extends FragmentPagerAdapter {
        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: return rcf;
                case 1: return rdf;
                default: return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return "개체별";
                case 1: return "날짜별";
                default: return null;
            }
        }
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
        lLcontent.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                lLcontent.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
        pBloading.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                pBloading.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }
}
