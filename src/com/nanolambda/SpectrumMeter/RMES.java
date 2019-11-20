package com.nanolambda.SpectrumMeter;

import java.io.InputStream;
import java.util.ArrayList;

public class RMES {
    public static int result(InputStream name1, InputStream name2){
        double[][] datasets_alcoho = new double[55][121];
        double[][] unknown_data = new double[1][121];

        StorageFile.readData(name1, datasets_alcoho);
        double[][] snv_alcoho = new double[11][121];
        for(int i=1; i<12; i++){
            Matrix.MeanCreate(i, datasets_alcoho, snv_alcoho);
        }

        StorageFile.readData(name2, unknown_data);
        double[][] snvUnknown = Matrix.snv(unknown_data);

        double[][] snvUnknownRange = new double[1][7];
        Matrix.CreateColum(110, 116, snvUnknown, snvUnknownRange);

        double[][] snv_alcohoRange = new double[11][121];
        snv_alcohoRange = snv_alcoho;

        double[][] snv_alcohoRangeCell = new double[11][7];
        Matrix.CreateColum(110, 116, snv_alcohoRange, snv_alcohoRangeCell);

        double[][] RMES1 = Matrix.rms(snv_alcohoRangeCell, snvUnknownRange);
        // Matrix.printMatrix(RMES1);
        int[] C = {100,90,80,70,60,50,40,30,20,10,0};
        int[] min_RMES1 = Matrix.min(RMES1);

        return C[min_RMES1[1]];
    }
    /*
    tinh ra gia tri ruou
    name1: inputStream lay tu getResources().openRawResource;
    matrix: matrix dau vao tinh ra reslut
    row: so dong cua matrix
     */
    public static int result(InputStream name1, double[][] matrix, int row){
        double[][] datasets_alcoho = new double[55][121];

        StorageFile.readData(name1, datasets_alcoho);
        double[][] snv_alcoho = new double[11][121];
        for(int i=1; i<12; i++){
            Matrix.MeanCreate(i, datasets_alcoho, snv_alcoho);
        }

        double[][] unknown_data = new double[row][121];
        unknown_data = matrix;
        double[][] average = Matrix.mean(unknown_data);
        double[][] snvUnknown = Matrix.snv(average);

        double[][] snvUnknownRange = new double[1][7];
        Matrix.CreateColum(110, 116, snvUnknown, snvUnknownRange);

        double[][] snv_alcohoRange = new double[11][121];
        snv_alcohoRange = snv_alcoho;

        double[][] snv_alcohoRangeCell = new double[11][7];
        Matrix.CreateColum(110, 116, snv_alcohoRange, snv_alcohoRangeCell);

        double[][] RMES1 = Matrix.rms(snv_alcohoRangeCell, snvUnknownRange);
        // Matrix.printMatrix(RMES1);
        int[] C = {0,10,20,30,40,50,60,70,80,90,100};
        int[] min_RMES1 = Matrix.min(RMES1);

        return C[min_RMES1[1]];
    }

    public static int result_1(InputStream name1, double[][] matrix, int row){
        //doc file dataset thanh array
        ArrayList<ArrayList<Double>> datasets = new ArrayList<ArrayList<Double>>();
        StorageFile.readData(name1, datasets);
        int row_dataset = datasets.size();
        int col_dataset = datasets.get(0).size();

        //tao matrix datasets_alcoho tu array
        double[][] datasets_alcoho = new double[row_dataset][col_dataset];
        for(int i=0;i<row_dataset;i++){
            for(int j=0;j<col_dataset;j++){
                datasets_alcoho[i][j] = datasets.get(i).get(j);
            }
        }
        Matrix.printMatrix(datasets_alcoho);

        //cach 5 dong tu matrix datasets_alcoho tinh mean
        int step=5;
        int row_snv_alcoho = datasets_alcoho.length / step;
        double[][] snv_alcoho = new double[row_snv_alcoho][121];
        for(int i=1; i<12; i++){
            Matrix.MeanCreate(i, datasets_alcoho, snv_alcoho);
        }

        //tinh trung binh matrix unknow
        double[][] unknown_data = new double[row][121];
        unknown_data = matrix;
        double[][] average = Matrix.mean(unknown_data);
        double[][] snvUnknown = Matrix.snv(average);

        // tao matrix tu so cot
        double[][] snvUnknownRange = new double[1][7];
        Matrix.CreateColum(110, 116, snvUnknown, snvUnknownRange);

        double[][] snv_alcohoRange = new double[11][121];
        snv_alcohoRange = snv_alcoho;

        double[][] snv_alcohoRangeCell = new double[11][7];
        Matrix.CreateColum(110, 116, snv_alcohoRange, snv_alcohoRangeCell);

        double[][] RMES1 = Matrix.rms(snv_alcohoRangeCell, snvUnknownRange);

        // Matrix.printMatrix(RMES1);
        int[] C = {0,10,20,30,40,50,60,70,80,90,100};
        int[] min_RMES1 = Matrix.min(RMES1);

        return C[min_RMES1[1]];
    }
}
