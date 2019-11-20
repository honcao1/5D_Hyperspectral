package com.nanolambda.SpectrumMeter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class StorageFile {

    public static void readData(InputStream is, double[][] m){
        try {
            BufferedReader buff = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line = null;
            int i=0;
            while ((line = buff.readLine()) != null){
                String[] dataSlip = null;
                dataSlip = line.split(",");
                for(int j=0; j<dataSlip.length; j++){
                    m[i][j] = Double.parseDouble(dataSlip[j]);
                }
                i++;
            }
        } catch (Exception e){}
    }

    public static void readData(InputStream is, ArrayList<ArrayList<Double>> m){
        try {
            m.clear();
            BufferedReader buff = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String line = null;
            int i=0;
            while ((line = buff.readLine()) != null){
                String[] dataSlip = null;
                dataSlip = line.split(",");

                ArrayList<Double> list = new ArrayList<>();

                for(int j=0; j<dataSlip.length; j++){
                    list.add(Double.parseDouble(dataSlip[j]));
                }
                m.add(list);
            }
        } catch (Exception e){}
    }

}
