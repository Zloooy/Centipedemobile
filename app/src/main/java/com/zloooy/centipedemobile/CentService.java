package com.zloooy.centipedemobile;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by zloooy on 27.08.17.
 */

public class CentService extends Service {
    private Messenger mes;
    private HandlerThread mt;
    private ListenHandler lh;
    private CentDict cd;
    public static final int OUTPUT=0,ADD_PASS=1,TRY_PASS=2,ADD_DICT=3,SAVE_DICT=4,PAGE=5,TOAST=6;
    private class ListenHandler extends Handler{
        ListenHandler(Looper looper)
        {super(looper);}

        @Override
        public void handleMessage(Message msg) {
            mes=msg.replyTo;
            switch (msg.what){
                case ADD_PASS:
                    cd.StopBrute();
                    cd.addPass(msg.getData().getString("PASS"));
                    try {
                        sendOutput(msg.getData().getString("PASS")+" added");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    try {
                        sendToast("Password added");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case TRY_PASS:
                    try {
                        StringBuilder sb=new StringBuilder(" ");
                        for (int i=0;i<4;i++){
                        //tryPass(msg.getData().getString("PASS"));
                            sb.append(" ");
                            tryPass(sb.toString());
                            cd.StopBrute();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case ADD_DICT:
                    cd.StopBrute();
                    try {
                        BufferedReader bf=new BufferedReader(new FileReader(new File(msg.getData().getString("DICT"))));
                        String s=bf.readLine();
                        while (s!=null){
                            sendOutput(s);
                            cd.addPass(s);
                            s=bf.readLine();}
                    sendToast("Words got");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case SAVE_DICT:
                    try {
                        cd.toFile(new File(msg.getData().getString("DICT")));
                        sendToast("dictionary saved");
                        Log.d("CS","Dictionary saved");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    }

    private void tryPass(String pass) throws RemoteException, InterruptedException, IOException, XmlPullParserException {
        cd.StartBrute(pass.length());
        String s=cd.getNextPass();
        Log.d("CS",s+" got");
        String[] domains=new String[]{"ru","com","net"};
        while (s!=null){
            Log.d("CS","s not null!!!");
            for(String dom:domains){
                String url="http://"+s+"."+dom;
                Log.d("CS","testing "+url);
                String title=getPageTitle(url);
                if(title!=null){
                    sendPage(url,title);
                    cd.addPass(s);
                }
                else {
                    sendToast(url+" not found");
                }
            }
            /*if(s==null) {
                sendOutput("haven't found");
                break;
            }
            if(s.equals(pass)) break;*/
            //java.util.concurrent.TimeUnit.MILLISECONDS.sleep(1);
            //sendOutput(s);
            Log.d("CS","requesting new pass");
            s=cd.getNextPass();
            Log.d("CS",s+" got");
        }
        Log.d("CS","s=null");
        //sendOutput(i+" iterations");
    }
    @Override
    public IBinder onBind(Intent intent) {
        if(mes==null){
            mt=new HandlerThread("HT");
            mt.start();
            lh=new ListenHandler(mt.getLooper());
            mes=new Messenger(lh);
        }
        return mes.getBinder();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.d("CS","started");
        String letters="abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
              //  " .-_'+/$!#@*`,";
        cd=new CentDict(letters.toCharArray());
    }
    private String getPageTitle(String urpath) throws RemoteException, IOException{
        try {
            URL url=new URL(urpath);
            HttpURLConnection con=(HttpURLConnection)url.openConnection();
            con.setReadTimeout(1000);
            con.setRequestMethod("GET");
            InputStreamReader rd=new InputStreamReader(con.getInputStream());
            BufferedReader br=new BufferedReader(rd);
            String s= br.readLine();
            while(s!=null){
                int startindex=s.indexOf("<title>");
                if(startindex!=-1){
                    startindex+=7;
                    int endindex=s.indexOf("</title>");
                    if(endindex==0) endindex=s.length()-1;
                    if(!(startindex<endindex)) { startindex=0;
                        endindex=s.length()-1;}
                    return s.substring(startindex,endindex);
                }
                s=br.readLine();
            }
            return  urpath;
        } catch (UnknownHostException ue) {
            Log.d("CS","UHE");
            return null;
    }
    catch (IOException ie){
        ie.printStackTrace();
        return urpath;
    }
    }
    private void sendPage(String url,String title) throws RemoteException {
        if(mes!=null){
            Message m=new Message();
            m.what=PAGE;
            Bundle b=new Bundle();
            b.putString("URL",url);
            b.putString("TITLE",title);
            m.setData(b);
            mes.send(m);
            Log.d("CS","Page sent");
        }
    }
    private void sendOutput(String output) throws RemoteException {
        if(mes!=null){
        Message m=new Message();
        m.what=OUTPUT;
        Bundle b=new Bundle();
        b.putString("OUTPUT",output);
        m.setData(b);
        mes.send(m);}
    }
    private void sendToast(String toast) throws RemoteException {
        if(mes!=null){
            Message m=new Message();
            m.what=TOAST;
            Bundle b=new Bundle();
            b.putString("TOAST",toast);
            m.setData(b);
            mes.send(m);}
    }
}
