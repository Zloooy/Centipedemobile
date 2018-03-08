package com.zloooy.centipedemobile;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.CharBuffer;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CentDict {
    private CharOrder[] chartbl;
    private PassOrder po;
    private int brutepos=-1;
    private boolean bruteStarted = false;
    private static class CharOrder{
        private CharRate.CharRow c;
        private CharRate cr;
        private static class CharRate{
            private ArrayList<CharRow> table;
            private static class CharRow{
                private final char ch;
                private int weight;
                CharRow(char c)
                {
                    this(c,0);
                }
                CharRow(char c, int w){
                    ch=c;
                    weight=w;
                    Log.d("CharRow","created "+c+" "+w);
                }
                char getChar()
                {return ch;}
                int getWeight()
                {return weight;}
                void IncWeight(){weight++;}
                void DecWeight(){weight--;}
                public String toString(){
                    return ch+" "+weight;
                }
            }
            CharRate(char[] chars){
                table=new ArrayList<>();
                for(char c:chars){
                    add(c,0);
                }
            }
            CharRate(){
                table=new ArrayList<>();
            }
            void add(char c,int weight){
                Log.d("CharRate","adding "+c+" "+weight);
                if(table.isEmpty())
                {
                    table.add(new CharRow(c,weight));
                }
                else{
                    int res=table.size();
                    for (int i=0;i<res;i++) {
                        CharRow cr=table.get(i);
                        if(cr.getWeight()<weight)
                        {
                            table.add(i,new CharRow(c,weight));
                            return;
                        }
                        else if(cr.getWeight()==weight&&cr.getChar()>c)
                        {      Log.d("CharRate","OrderedByChar "+cr.getChar()+">"+c);
                               table.add(i,new CharRow(c,weight));
                               return;
                        }
                    }
                    Log.d("CharRate","added to start");
                    table.add(new CharRow(c,weight));
                }
            }
            Character getNext(char ch){
                for (int i = 0; i < table.size(); i++) {
                    if(table.get(i).getChar()==ch){
                        if(i+1==table.size()) return null;
                        return table.get(i+1).getChar();
                    }
                }
                return null;
            }
            void IncWeight(char c){
                int s=table.size();
                for (int i = 0; i < s; i++) {
                    if(table.get(i).getChar()==c){
                        Log.d("CRa","found "+c);
                        CharRow cr=table.get(i);
                        table.remove(i);
                        cr.IncWeight();
                        int w=cr.getWeight();
                        for(int j=i-1;j>0;j--){
                            if(table.get(j).getWeight()>w){
                                table.add(j+1,cr);
                                return;
                            }
                            if(table.get(j).getWeight()==w&&table.get(j).getChar()<w){
                                table.add(j+1,cr);
                                return;
                            }
                        }
                        Log.d("CRa","adding to start "+cr);
                        table.add(0,cr);
                        return;
                    }
                }
            }
            Character getFirst(){
            return table.get(0).getChar();
            }
            public boolean containsChar(char c){
                for (CharRow cr: table) {
                    if(cr.getChar()==c) return true;
                }
                return false;
            }
            public int getWeight(char c){
                for(CharRow cr:table){
                    if(cr.getChar()==c){
                        return cr.getWeight();
                    }
                }
                return 0;
            }
            public String toString(){
                StringBuilder sb=new StringBuilder("");
                for (CharRow cr:table){
                    sb.append(cr).append("\n");
                }
                return sb.toString();
            }
        }
        CharOrder(char ch, int weight){
            c=new CharRate.CharRow(ch,weight);
            cr=new CharRate();
        }
        CharOrder(char ch,int weight, char[] chars){
            c=new CharRate.CharRow(ch,weight);
            cr=new CharRate(chars);
        }
        char getChar(){return c.getChar();}
        int getWeight(){return c.getWeight();};
        void add(char ch, int i){
            Log.d("CharOrder","adding "+ch+" "+i);
            cr.add(ch,i);
        }
        public void IncWeight(char c){
            cr.IncWeight(c);
        }
        Character getFirst(){
            return cr.getFirst();
        }
        public Character getNext(char c){
            return cr.getNext(c);
        }
        public String toString(){
            return "\n"+c+"\n"+cr;
        }
        public boolean containsChar(char ch){
            return cr.containsChar(ch);
        }
        public int getWeight(char c){
            return cr.getWeight(c);
        }
        public void IncWeight(){
            c.IncWeight();
        }
    }
    private class AnalyzedPass{
        private String pass;
        private int weight;
        AnalyzedPass(String passs){
            pass=passs;
            weight=getPassWeight(pass);
        }
        AnalyzedPass(Character[] passs){
            StringBuilder sb=new StringBuilder();
            for (Character c: passs) {
                sb.append(c);
            }
            pass=sb.toString();
            weight=getPassWeight(pass);
        }
        public int getWeight() {
            return weight;
        }
        public String getPass(){
            return pass;
        }
        public Character[] getCharacterArray(){
            int ln=pass.length();
            Character[] carr=new Character[ln];
            for(int i=0;i<ln;i++){
                carr[i]=pass.charAt(i);
            }
            return carr;
        }
        public String toString(){
            return pass+" "+weight;
        }
    }
    private class PassOrder{
        private BlockingQueue<AnalyzedPass> passquenue;
        private ArrayList<AnalyzedPass> po;
        private Puter pu;
        private class Puter extends Thread{
            @Override
            public void run() {
                super.run();
                while (true){
                    try {
                        AnalyzedPass ap=passquenue.take();
                        boolean foundpass=false;
                        int end=po.size();
                        int pweight=ap.getWeight();
                        for (int j=0;j<end;j++){
                            AnalyzedPass cur=po.get(j);
                            if(pweight>cur.getWeight()){
                                po.add(j,ap);
                                Log.d("CDPOPU",ap.getPass()+" "+j);
                                foundpass=true;
                                break;
                            }
                            else if (pweight==cur.getWeight() && ap.getPass().charAt(0)<cur.getPass().charAt(0)){
                                po.add(j,ap);
                                Log.d("CDPOPU",ap.getPass()+" "+j);
                                foundpass=true;
                                break;
                            }
                        }
                        if(!foundpass){
                            po.add(ap);
                            Log.d("CDPOPU",ap.getPass()+" "+(po.size()-1));
                        }
                        Log.d("CDPOPU","]");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        PassOrder(){
            passquenue=new LinkedBlockingQueue<AnalyzedPass>();
            po=new ArrayList<>();
            pu=new Puter();
            pu.start();
        }
        public void putPass(AnalyzedPass ap){
            Log.d("CDPO",ap.getPass());
            if(po.isEmpty()){
                po.add(ap);
            }
            else{
                passquenue.add(ap);
            }
        }
        public AnalyzedPass get(int i){
            return po.get(i);
        }
        public void clear(){
            po.clear();
        }
        public int size(){
            return po.size();
        }
        public boolean isReady(){
            return passquenue.isEmpty();
        }
    }
    CentDict(char[] chars){
        chartbl=new CharOrder[chars.length];
        for(int i=0;i<chars.length;i++){
            chartbl[i]=new CharOrder(chars[i],0);
            for (char c:chars) {
                chartbl[i].add(c,0);
            }
        }
        po=new PassOrder();
    }
    private CentDict(ArrayList<CharOrder> chartbltemp){
        chartbl=chartbltemp.toArray(new CharOrder[chartbltemp.size()]);
        po=new PassOrder();
    }
    void add(char c1, char c2, int i)
    {
        for(CharOrder co:chartbl){if(co.getChar()==c1) co.add(c2,i);}
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CharOrder co : chartbl) {
            sb.append(co);
        }
        return sb.toString();
    }
    public void toFile(File f) throws IOException {
        if(!f.exists()){
            f.createNewFile();
        }
        FileWriter fw=new FileWriter(f);
        fw.write(this.toString());
        fw.flush();
        fw.close();
    }
    public static CentDict fromFile(File f) throws IOException {
        FileReader fr=new FileReader(f);
        BufferedReader br=new BufferedReader(fr);
        String s=br.readLine();
        ArrayList<CharOrder> chartbltemp=new ArrayList<>();
        boolean newOrder=false;
        while (s!=null){
        if(s.length()==0){
            newOrder=true;
        }
        else {
            String[] res=s.split(" ");
            if(newOrder){
                chartbltemp.add(new CharOrder(res[0].charAt(0),Integer.parseInt(res[1])));
                newOrder=false;
            }
            else {
                chartbltemp.get(chartbltemp.size() - 1).add(res[0].charAt(0), Integer.parseInt(res[1]));
            }
        }
        s=br.readLine();
        }
        return new CentDict(chartbltemp);
    }
    public static CentDict fromFileWithCharset(File f, char[] charset) throws IOException {
        FileReader fr=new FileReader(f);
        BufferedReader br=new BufferedReader(fr);
        String s=br.readLine();
        ArrayList<CharOrder> chartbltemp=new ArrayList<>();
        while (s!=null){
            if(s.length()==0){
                char tit=s.charAt(0);
                for (char c : charset) {
                    if (tit == c) {
                        chartbltemp.add(new CharOrder(tit,0));
                        break;
                    }
                }
            }
            else {
                String[] res=s.split(" ");
                if(!chartbltemp.get(chartbltemp.size()-1).containsChar(res[0].charAt(0))){
                chartbltemp.get(chartbltemp.size()-1).add(res[0].charAt(0),Integer.parseInt(res[1]));}
            }
            s=br.readLine();
        }
        return new CentDict(chartbltemp);
    }
    private CharOrder get(char c){
        for (CharOrder co:chartbl){
            if(co.getChar()==c) return co;
        }
        return null;
    }
    protected Character[] getNextPass(Character[] pass,int pos,boolean forward){
            //Log.d("CD", Arrays.toString(pass)+"\n"+pos+" "+forward);
        if(pass[pos]==null){
            if(forward){
                //Log.d("CD", String.valueOf(pass[pos-1]==null));
                pass[pos]=getFirst(pass[pos-1]);
                if(pos<pass.length-1){
                    pos++;
                    return getNextPass(pass,pos,forward);
                }
                else{
                    return pass;
                }
            }
            else {
                pos--;
                return getNextPass(pass,pos,forward);
            }
    }
    else {
            if (pos == 0) {
                pass[0]=null;
                return pass;
            }
            else {
                pass[pos] = getNext(pass[pos - 1], pass[pos]);
                if (pos == pass.length - 1) {
                    if (pass[pos] == null) {
                        forward=false;
                        return getNextPass(pass, pos, forward);
                    }
                    return pass;
                }
                if(pass[pos]!=null){
                forward = true;
                pos++;}
                return getNextPass(pass, pos, forward);
            }
        }
    }
    private void IncWeight(char c1, char c2) throws NullPointerException{
        Log.d("CINCWeight",c1+" "+c2);
        get(c1).IncWeight(c2);
    }
    private void IncStarterWeight(char c) throws NullPointerException
    {
        get(c).IncWeight();
    }
    public Character getNext(char c1, char c2) throws NullPointerException
    {
        return get(c1).getNext(c2);
    }
    public Character getFirst(char c){
        Log.d("CD","GOT: "+c);
        return get(c).getFirst();
    }
    public char getFirstStartChar(){
        return chartbl[0].getChar();
    }
    public String getFirstPassStartsWith(char c,int length){
        Log.d("CD","requested length: "+length);
        StringBuilder sb=new StringBuilder();
        sb.append(c);
        Log.d("CD","First char: "+sb.charAt(0));
        for (int i=1;i<length;i++){
            Character cc=getFirst(sb.charAt(i-1));
            Log.d("CD",cc.toString());
            sb.append(cc);
            Log.d("CD",Integer.toString(i)+" "+cc.toString());
        }
        Log.d("CD",sb.toString());
        return sb.toString();
    }
    public void wordsFromFile(File f) throws IOException {
        BufferedReader bf=new BufferedReader(new FileReader(f));
        String s=bf.readLine();
        while (s!=null){
            this.addPass(s);
            s=bf.readLine();
        }
    }
    public void addPass(String pass){
        Log.d("CD","Adding pass "+s);
        if(pass.length()>0){
            try{
        IncStarterWeight(pass.charAt(0));
        for(int i=0;i<pass.length()-1;i++){
            IncWeight(pass.charAt(i),pass.charAt(i+1));
        }
            }
            catch (NullPointerException e){
                e.printStackTrace();}
        }
    }
    public int getPassWeight(String pass){
        int w=get(pass.charAt(0)).getWeight();
        for(int i=0;i<pass.length()-1;i++){
            w+=get(pass.charAt(i)).getWeight(pass.charAt(i+1));
        }
        return w;
    }
    public String getNextPass(){
        if(bruteStarted) {
            brutepos+=1;
            if(brutepos<po.size()) {
                Log.d("CD","Password order size is "+po.size()+" brutepos="+brutepos+" returning "+po.get(brutepos).getPass());
                return po.get(brutepos).getPass();
            }
            Log.d("CD","returning null");
                return null;
    }
    else return null;
    }
    public void StartBrute(int length){
        Log.d("CD",Arrays.toString(chartbl));
        for(CharOrder co:chartbl) {
            AnalyzedPass ap = new AnalyzedPass(getFirstPassStartsWith(co.getChar(), length));
            while (true) {
                po.putPass(ap);
                Character[] res = getNextPass(ap.getCharacterArray(), length - 1, false);
                if (res[0] == null) break;
                ap=new AnalyzedPass(res);
            }
        }
        while(!po.isReady()) try {
            TimeUnit.MILLISECONDS.sleep(100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        brutepos=-1;
        bruteStarted=true;
        Log.d("CD","brute started");
    }
    public void StopBrute(){
        bruteStarted=false;
    brutepos=-1;
    po.clear();
    }
    }
