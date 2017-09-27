package com.holenet.cowinfo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import static com.holenet.cowinfo.BackupActivity.Location.*;
import static java.net.HttpURLConnection.HTTP_OK;

public class BackupActivity extends AppCompatActivity {
    final static int REQUEST_LOGIN = 1234;

    ProgressBar pBloading;
    LinearLayout lLcontent;
    ConstraintLayout cLheader;
    ListView lVdb;
    DBAdapter adapter;

    DBListTask listTask;
    List<DBTransferTask> transferTasks;
    boolean loggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        getSupportActionBar().setTitle("백업/복원");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
        transferTasks = new ArrayList<>();

        cLheader = (ConstraintLayout) findViewById(R.id.cLheader);
        pBloading = (ProgressBar) findViewById(R.id.pBloading);
        lLcontent = (LinearLayout) findViewById(R.id.lLcontent);
        lVdb = (ListView) findViewById(R.id.lVdb);
        lVdb.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.showContextMenu();
            }
        });
        adapter = new DBAdapter(BackupActivity.this, R.layout.item_db, new ArrayList<DBItem>());
        lVdb.setAdapter(adapter);
        registerForContextMenu(lVdb);

        FloatingActionButton fABaddDB = (FloatingActionButton) findViewById(R.id.fABaddDB);
        fABaddDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String[] itemNames = new String[] {"기기에 백업", "서버에 백업"};
                final boolean[] items = new boolean[] {false, false};
                new AlertDialog.Builder(BackupActivity.this)
                        .setTitle("데이터베이스 백업")
                        .setMultiChoiceItems(itemNames, items, new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                                items[i] = b;
                            }
                        })
                        .setPositiveButton(R.string.dialog_button_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(!items[0] && !items[1]) {
                                    Toast.makeText(BackupActivity.this, "백업되지 않았습니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    String time = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Calendar.getInstance().getTime());
                                    if(items[0]) {
                                        transfer(DEVICE_IN, DEVICE_EX, time, null, 0);
//                                        tryBackupDevice(time);
                                    }
                                    if(items[1]) {
                                        transfer(DEVICE_IN, SERVER, time, null, 0);
//                                        tryBackupServer(time);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(R.string.dialog_button_cancel, null)
                        .create().show();
            }
        });

        new AlertDialog.Builder(this)
                .setMessage("서버에 접근하기 위하여 로그인하시겠습니까?")
                .setPositiveButton(R.string.dialog_button_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(BackupActivity.this, LoginActivity.class);
                        startActivityForResult(intent, REQUEST_LOGIN);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        refresh();
                    }
                })
                .create().show();
    }

    private void updateProgress(final boolean show) {
        if(!show && (listTask!=null || !transferTasks.isEmpty()))
            return;

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

    private void refresh() {
        if(!requestPermission())
            return;
        if(listTask!=null)
            return;
        updateProgress(true);
        listTask = new DBListTask();
        listTask.execute((Void) null);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        final int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        DBItem db = adapter.getItem(position);
        menu.add(0, 0, Menu.NONE, "복원");
        if(!db.isDevice()) {
            menu.add(0, 1, Menu.NONE, "기기에 복사");
        }
        if(!db.isServer()) {
            menu.add(0, 2, Menu.NONE, "서버에 복사");
        }
        if(db.isDevice() && db.isServer()) {
            menu.add(0, 3, Menu.NONE, "삭제");
        }
        if(db.isDevice()) {
            menu.add(0, 4, Menu.NONE, "기기에서 삭제");
        }
        if(db.isServer()) {
            menu.add(0, 5, Menu.NONE, "서버에서 삭제");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();
        final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
        final DBItem db = adapter.getItem(position);
        final String[] datetime = Parser.getDatetimeFromName(db.getName());

        if(id==0) { // restore
            new AlertDialog.Builder(this)
                    .setTitle(datetime[0]+" "+datetime[1])
                    .setMessage("선택한 데이터베이스로 복원하시겠습니까?\n현재 데이터베이스가 덮어씌워집니다.")
                    .setPositiveButton(R.string.dialog_button_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(db.isDevice()) {
                                transfer(DEVICE_EX, DEVICE_IN, null, db.getName(), 0);
                            } else {
                                transfer(SERVER, DEVICE_IN, null, db.getName(), db.getId());
                            }
                        }
                    }).setNegativeButton(R.string.dialog_button_cancel, null)
                    .create().show();
        } else if(id<3) { // copy
            if(id==1) {
                transfer(SERVER, DEVICE_EX, null, db.getName(), db.getId());
            } else {
                transfer(DEVICE_EX, SERVER, null, db.getName(), 0);
            }

        } else if(id<6) { // delete
            new AlertDialog.Builder(this)
                    .setTitle(datetime[0]+" "+datetime[1])
                    .setMessage("선택한 데이터베이스를 "+(id<=4?"기기":"")+(id==3?", ":"")+(id%2==1?"서버":"")+"에서 삭제하시겠습니까?")
                    .setPositiveButton(R.string.dialog_button_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(db.isDevice()) {
                                transfer(DEVICE_EX, null, null, db.getName(), 0);
                            } else {
                                transfer(SERVER, null, null, db.getName(), db.getId());
                            }
                        }
                    }).setNegativeButton(R.string.dialog_button_cancel, null)
                    .create().show();
        }
        return true;
    }

    Menu menu;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_backup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id==android.R.id.home) {
            onBackPressed();
        } else if(id==R.id.action_refresh) {
            refresh();
        } else if(id==R.id.action_login) {
            requestLogin();
        } else if(id==R.id.action_db_delete) {
            // TODO: implement multi select mode and delete multiple items
            Toast.makeText(this, "delete", Toast.LENGTH_SHORT).show();
        } else if(id==R.id.action_db_format) {
            new AlertDialog.Builder(this)
                    .setMessage("현재 데이터베이스를 초기화하시겠습니까? 초기화 전에 백업을 하는 것을 권장합니다.")
                    .setPositiveButton(R.string.dialog_button_confirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            transfer(DEVICE_IN, null, null, null, 0);
                        }
                    })
                    .setNegativeButton(R.string.dialog_button_cancel, null)
                    .create().show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length<=0 || grantResults[0]!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한을 허용하지 않으면 백업 및 복원 기능을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            refresh();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_LOGIN) {
            if(resultCode==1) {
                loggedIn = true;
                if(menu!=null) {
                    menu.removeItem(R.id.action_login);
                }
            }
            refresh();
        }
    }

    private boolean requestPermission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.error_permission, Toast.LENGTH_LONG).show();
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Log.e("request", "??");
            } else {
                Log.e("request", "permmm");
            }
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return false;
        }
        return true;
    }

    private boolean requestLogin() {
        if(!loggedIn) {
            Toast.makeText(this, "서버에 접근하려면 로그인 해야합니다.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(BackupActivity.this, LoginActivity.class);
            startActivityForResult(intent, REQUEST_LOGIN);
            return false;
        }
        return true;
    }

    public class DBListTask extends AsyncTask<Void, List<DBItem>, String> {
        @Override
        protected String doInBackground(Void... voids) {
            List<DBItem> items = new ArrayList<>();
            File backupDir = new File(Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database");
//            Log.e("backupDir", backupDir+"/"+backupDir.exists());
            if(backupDir.exists()) {
//                Log.e("backupDir", backupDir.list()+"");
                for(String path : backupDir.list()) {
                    if(path.split("\\.").length==2 && path.split("\\.")[0].split("_").length==6) {
                        DBItem db = new DBItem(path);
                        db.setDevice(true);
                        items.add(db);
                    }
                }
            }
            publishProgress(items);

            if(loggedIn) {
                return NetworkManager.get(BackupActivity.this, NetworkManager.DATABASE_URL);
            } else {
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(List<DBItem>... values) {
            adapter.clear();
            List<DBItem> items = values[0];
            for(DBItem item : items) {
                adapter.addItem(item);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            listTask = null;
            updateProgress(false);

            if(loggedIn) {
                if(result==null) {
                    Toast.makeText(BackupActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                }

                List<DBItem> items = Parser.getDBItemsJSON(result);
                for(DBItem item : items) {
                    adapter.addItem(item);
                }
            } else {
//                Toast.makeText(BackupActivity.this, "로그인을 하면 서버의 데이터베이스를 가져올 수 있습니다.", Toast.LENGTH_SHORT).show();
            }
            adapter.sort(new Comparator<DBItem>() {
                @Override
                public int compare(DBItem db1, DBItem db2) {
                    return -db1.getName().compareTo(db2.getName());
                }
            });
            adapter.notifyDataSetChanged();
        }
    }

    enum Location {
        DEVICE_IN, DEVICE_EX, SERVER,
    }

    private void transfer(Location from, Location to, String time, String name, int id) {
        if(from==DEVICE_EX || to==DEVICE_EX) {
            if(!requestPermission()) {
                return;
            }
        }
        if(from==SERVER || to==SERVER) {
            if(!requestLogin()) {
                return;
            }
        }

        updateProgress(true);
        DBTransferTask transferTask = new DBTransferTask(BackupActivity.this, from, to, time, name, id);
        transferTasks.add(transferTask);
        transferTask.execute((Void) null);
    }

    private class DBTransferTask extends AsyncTask<Void, Void, Boolean> {
        Context context;
        Location from, to;
        String time, name;
        int id;

        public DBTransferTask(Context context, Location from, Location to, String time, String name, int id) {
            this.context = context;
            this.from = from;
            this.to = to;
            this.time = time;
            this.name = name;
            this.id = id;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if(to==null) { // delete operation
                if(from==SERVER) { // delete Server db
                    String url = NetworkManager.DATABASE_URL+"delete/"+id+"/";
                    String output = NetworkManager.get(context, url);
                    if(output==null)
                        return false;
                    if(output.equals(NetworkManager.RESULT_STRING_LOGIN_FAILED))
                        return false;
                    return true;
                } else {
                    File file;
                    if(from==DEVICE_IN) { // format current db
                        file = new File(String.valueOf(context.getExternalFilesDir(null))+File.separator+"cows.db");
                    } else { // delete Device db
                        file = new File(Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database"+File.separator+name);
                    }
                    return file.delete();
                }
            }
            else if(from!=SERVER && to!=SERVER) { // from Device to Device
                String inPath = String.valueOf(context.getExternalFilesDir(null))+File.separator+"cows.db";
                String exPath = Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database";

                File exPathFile = new File(exPath);
                if(!exPathFile.exists()) {
                    if(!exPathFile.mkdirs()) {
                        Log.e("exPathFile.mkdirs Error", "");
                        return false;
                    }
                }
                exPath += File.separator+(time!=null?time+".db":name);

                File fromFile, toFile;
                if(from==DEVICE_IN) {
                    fromFile = new File(inPath);
                    toFile = new File(exPath);
                } else {
                    fromFile = new File(exPath);
                    toFile = new File(inPath);
                }

                try {
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(fromFile));
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(toFile));
                    int data;
                    while(true) {
                        data = in.read();
                        if(data==-1)
                            break;
                        out.write(data);
                    }
                    in.close();
                    out.close();

                } catch(Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            } else if(from==SERVER) { // from Server to Device
                String url = NetworkManager.DATABASE_URL+"download/"+id+"/";
                File file;
                if(to==DEVICE_IN) {
                    file = new File(String.valueOf(context.getExternalFilesDir(null))+File.separator+"cows.db");
                } else {
                    file = new File(Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database"+File.separator+name);
                }
                return NetworkManager.download(context, url, file)==HTTP_OK;
            } else { // from Device to Server
                String url = NetworkManager.DATABASE_URL+"upload/";
                File file;
                if(from==DEVICE_IN) {
                    file = new File(String.valueOf(context.getExternalFilesDir(null))+File.separator+"cows.db");
                    name = time+".db";
                } else {
                    file = new File(Environment.getExternalStorageDirectory()+File.separator+"cowinfo"+File.separator+"database"+File.separator+name);
                }
                return NetworkManager.upload(context, url, file, name)==HTTP_OK;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            transferTasks.remove(this);
            updateProgress(false);

            if(result) {
                if(to==null) {
                    if(from==DEVICE_IN) {
                        Toast.makeText(context, "데이터베이스 삭제에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "데이터베이스 초기화에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else if(to==DEVICE_IN) {
                    Toast.makeText(context, "복원에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, (to==DEVICE_EX?"기기":"서버")+"에 "+(from==DEVICE_IN?"백업":"복사")+" 성공하였습니다.", Toast.LENGTH_SHORT).show();
                }
                refresh();
            } else {
                Toast.makeText(context, "실패하였습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onCancelled() {
            transferTasks.remove(this);
            updateProgress(false);
        }
    }

    class DBAdapter extends ArrayAdapter<DBItem> {
        private List<DBItem> items;

        DBAdapter(Context context, int layout, List<DBItem> items) {
            super(context, layout, items);
            this.items = items;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View v = convertView;
            if(v==null) {
                v = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.item_db, null);
            }

            DBItem db = items.get(position);
            if(db!=null) {
                TextView tVname = (TextView) v.findViewById(R.id.tVname);
                if(tVname!=null) {
                    String[] datetime = Parser.getDatetimeFromName(db.getName());
                    tVname.setText(datetime[0]+" "+datetime[1]);
                }
                ImageView iVdevice = (ImageView) v.findViewById(R.id.iVdevice);
                if(iVdevice!=null)
                    iVdevice.setVisibility(db.isDevice() ? View.VISIBLE : View.INVISIBLE);
                ImageView iVserver = (ImageView) v.findViewById(R.id.iVserver);
                if(iVserver!=null)
                    iVserver.setVisibility(db.isServer() ? View.VISIBLE : View.INVISIBLE);
            }

            return v;
        }

        public void addItem(DBItem item) {
            for(int i = 0; i<items.size(); i++) {
                if(items.get(i).equals(item)) {
                    items.get(i).addProperty(item);
                    return;
                }
            }
            items.add(item);
        }

        public void setItems(List<DBItem> items) {
            this.items.clear();
            for(int i = 0; i<items.size(); i++) {
                this.items.add(items.get(i));
            }
        }
    }
}
