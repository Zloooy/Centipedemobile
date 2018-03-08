package com.zloooy.centipedemobile;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends Activity {
    private static final int GET_PASS_FILE =0,SAVE_DICTIONARY=1 ;
    private Messenger mes;
    private Messenger reciever=new Messenger(new MyHandler());
    MyConnection mc;
    ListView output;
    OutputAdapter ad;
    PageAdapter pa;

    private class MyConnection implements ServiceConnection{
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mes=new Messenger(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mes=null;
        }
    }
    private class MyHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case CentService.OUTPUT:
                    Log.d("C","OUTPUT "+msg.getData().getString("OUTPUT"));
                    addOutput(msg.getData().getString("OUTPUT"));
                    break;
                case CentService.PAGE:
                    Log.d("C","PAGE recieved");
                    addPage(msg.getData().getString("TITLE"),msg.getData().getString("URL"));
                    break;
                case CentService.TOAST:
                    Toast.makeText(getApplicationContext(),msg.getData().getString("TOAST"),Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
    private class OutputAdapter extends ArrayAdapter<String>{
        Context con;
        public OutputAdapter(Context context, int resource) {
            super(context, resource);
            con=context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView line=new TextView(con);
            line.setText(this.getItem(position));
            line.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return line;
        }

        @Override
        public void add(String object) {
            super.add(object);
        }
    }
    private class PageAdapter extends ArrayAdapter<String[]>{
        private Context con;
        public PageAdapter(Context context, int resource) {
            super(context, resource);
            con=context;
        }

        public void add(String title,String url) {
            super.add(new String[]{title,url});
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView line=new TextView(con);
            line.setText(this.getItem(position)[0]);
            line.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return line;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText passfield= (EditText) findViewById(R.id.passfield);
        mc=new MyConnection();
        Button brute=(Button) findViewById(R.id.brute);
        Button add=(Button) findViewById(R.id.add);
        final Button options=(Button) findViewById(R.id.options);
        output=(ListView) findViewById(R.id.output);
        ad=new OutputAdapter(this,0);
        pa=new PageAdapter(this,0);
        //output.setAdapter(ad);
        output.setAdapter(pa);
        output.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent in=new Intent(Intent.ACTION_VIEW, Uri.parse(pa.getItem(i)[1]));
                startActivity(in);
            }
        });
        startService(new Intent(MainActivity.this,CentService.class));
         bindService(new Intent(MainActivity.this,CentService.class),mc,BIND_AUTO_CREATE);
        brute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Log.d("MA","sending pass "+passfield.getText());
                    sendPass(CentService.TRY_PASS,passfield.getText().toString());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(passfield.getText()!=null){
                    try {
                        sendPass(CentService.ADD_PASS,passfield.getText().toString());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        registerForContextMenu(options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                options.showContextMenu();
            }
        });
//        Log.d("C",cd.getNext(new char[]{'a','d'}).toString());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0,GET_PASS_FILE,0,"Get passwords from file");
        menu.add(0,SAVE_DICTIONARY,0,"Save current dictionary");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case GET_PASS_FILE:
                Intent i=new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("file/");
                startActivityForResult(i,GET_PASS_FILE);
                break;
            case SAVE_DICTIONARY:
                Message m=new Message();
                m.what=CentService.SAVE_DICT;
                Bundle b=new Bundle();
                String dictpath=getFilesDir()+"/dicti.txt";
                b.putString("DICT", dictpath);
                Log.d("C","dictpath "+dictpath);
                m.setData(b);
                try {
                    mes.send(m);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode==RESULT_OK) {
            switch (requestCode) {
                case GET_PASS_FILE:
                    try {
                        sendDict(data.getData().getPath());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        unbindService(mc);
        super.onDestroy();
    }

    private void addOutput(String out){
        ad.add(out);
    }
    private void addPage(String title, String url){
        Log.d("C",title+" "+url);
        pa.add(title,url);
    }
    private void sendPass(int what,String pass) throws RemoteException {
    Message m=new Message();
    m.what=what;
        m.replyTo=reciever;
        Bundle b=new Bundle();
    b.putString("PASS",pass);
    m.setData(b);
        mes.send(m);
    }
    private void sendDict(String path) throws RemoteException {
        Message m=new Message();
        m.what=CentService.ADD_DICT;
        m.replyTo=reciever;
        Bundle b=new Bundle();
        b.putString("DICT",path);
        m.setData(b);
        mes.send(m);
    }
}
