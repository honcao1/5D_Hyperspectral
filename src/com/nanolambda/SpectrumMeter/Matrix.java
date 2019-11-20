package com.nanolambda.SpectrumMeter;

import java.util.Scanner;

public class Matrix {
    /*
    Tao ma tran 5 x 121 tu matrix lon roi tinh trung binh
    start: vi tri bat dau (hang - row)
    m:  Matrix dau vao
    m1: Matrix dau ra
     */
    public static void MeanCreate(int start,  double[][] m, double[][] m1){
        double[][] create = new double[5][121];
        for (int i = 5*start-5; i < 5*start; i++) {
            for (int j = 0; j < 121; j++) {
                create[i%5][j] = m[i][j];
            }
        }
        double[][] mean_create = snv(mean(create));
        for(int i=0; i<mean_create.length;i++){
            for(int j=0;j<mean_create[0].length;j++){
                m1[start-1][j] = mean_create[i][j];
            }
        }
    }
    /*
    Tao matrix theo colum tu ma tran lon
    start: diem bat dau
    end:   diem ket thuc
    m1: matrix dau vao
    m2; matrix dau ra
     */
    public static void CreateColum(int start, int end, double[][] m1, double[][] m2){
        for (int i = 0; i < m1.length; i++) {
            for (int j = 0; j < end-start+1; j++) {
                m2[i][j] = m1[i][j+start-1];
            }
        }
    }
    /*
    Tao matrix 1 x n(colum) gia tri la 1
     */
    public static int[][] ones(int col){
        //tao ma tran
        int[][] m = new int[1][col];
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < col; j++) {
                m[i][j] = 1;
            }
        }
        return m;
    }
    /*
    Matrix chyen vi hang thanh cot, cot thanh hang
     */
    public static double[][] Transpose(double[][] m){
        double[][] temp = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }
    /*
    Do lenh chuan
     */
    public static double[][] std(double[][] m){
        double[][] mean = mean(m);

        int row_M = m.length;
        int col_M = m[0].length;
        //System.out.println("size M: " + row_M + "\t" + col_M);

        double[][] phep_tru = new double[col_M][row_M];
        double[] count=new double[col_M];
        double[] variance=new double[col_M];
        double[][] std=new double[1][col_M];

        for(int j=0; j<m[0].length;j++){
            //phep tru = gtri goc - gtri tb
            //System.out.println("mean: " + mean[0][j]);
            for(int i=0; i<m.length; i++){
                phep_tru[j][i] = (double)m[i][j] - mean[0][j];
            }
            //in phep tru
            //System.out.print("Phep tru: ");
            //for(int i=0; i<phep_tru[0].length;i++){
            //    System.out.print(phep_tru[j][i] + "\t");
            //}
            //System.out.println();
            //binh phuong = pow(phep_tru, 2)
            for(int i=0; i<phep_tru[0].length;i++){
                phep_tru[j][i] = Math.pow(Math.abs(phep_tru[j][i]), 2);
            }
            //in phep tru binh phuong
            //System.out.print("pow 2:    ");
            //for(int i=0; i<phep_tru[0].length;i++){
            //   System.out.print(phep_tru[j][i] + "\t");
            //}
            //System.out.println();
            //tinh tong phep tru binh phuong
            for(int i=0; i<phep_tru[0].length;i++){
                count[j] = count[j] + phep_tru[j][i];
            }
            //phuon sai = count / m.length -1;
            variance[j] = count[j] / ((double)row_M-1);
            //System.out.print("variance:  " + variance[j]);
            //System.out.println();
            //do lenh chuan = can bac 2 phuong sai
            std[0][j] = Math.sqrt(variance[j]);
            //System.out.print("std:  " + std[j]);
            //System.out.println();
            //System.out.println("----------------------------------->");
        }
        return std;
    }
    /*
    Tinh trung binh
     */
    public static double[][] mean(double[][] m) {
        double[][] temp = new double[1][m[0].length];
        double mean = 0;
        for(int j=0; j<m[0].length; j++){
            for(int i=0; i<m.length; i++){
                mean = mean + m[i][j];
            }
            temp[0][j] = (double)mean / (double)m.length;
            mean=0;
        }
        return temp;
    }
    /*
    Nhan 2 matrix
     */
    public static double[][] Mulitiplication(double[][] m1, int[][] m2){
        int row_m1 = m1.length;
        int col_m1 = m1[0].length;
        int row_m2 = m2.length;
        int col_m2 = m2[0].length;
        // System.out.println("size M1: " + row_m1 + "\t" + col_m1);
        // System.out.println("size M2: " + row_m2 + "\t" + col_m2);
        // System.out.println("----------------------------------->");
        //khai bao ma tran tich
        double[][] tich = new double[row_m1][col_m2];
        // kiem tra dong m1 = cot m2
        if(col_m1 == row_m2){
            for(int i=0;i<row_m1;i++){
                for(int j=0;j<col_m2;j++){
                    for(int k=0;k<col_m1;k++){
                        tich[i][j] = m1[i][k] * m2[0][j];
                    }
                }
            }
        } else {
            System.out.println("dong m1 khong bang cot m2");
        }
        return tich;
    }
    /*
    Tru 2 matrix
     */
    public static double[][] Subtraction(double[][] m1, double[][] m2){
        double[][] matrix_Tru = new double[m1.length][m1[0].length];

        if(m1.length==m2.length && m1[0].length==m2[0].length){
            for(int i=0; i<m1.length; i++){
                for(int j=0; j<m1[0].length; j++){
                    matrix_Tru[i][j] = m1[i][j] - m2[i][j];
                }
            }
        } else {
            System.out.println("Size m1 # size m2.");
        }
        return matrix_Tru;
    }
    /*
    chia 2 matrix
     */
    public static double[][] Division(double[][] m1,double[][] m2){
        double[][] matrix_Chia = new double[m1.length][m1[0].length];

        if(m1.length==m2.length && m1[0].length==m2[0].length){
            for(int i=0; i<m1.length; i++){
                for(int j=0; j<m1[0].length; j++){
                    matrix_Chia[i][j] = m1[i][j] / m2[i][j];
                }
            }
        } else {
            System.out.println("Size m1 # size m2.");
        }
        return matrix_Chia;
    }
    /*
    Tinh snv ma tran
     */
    public static double[][] snv(double[][] m){
        double[][] matrix_Nhan = Mulitiplication(
                Transpose(mean(Transpose(m))),
                ones(m[0].length));

        double[][] tru = Subtraction(m, matrix_Nhan);

        double[][] matrix_std = Mulitiplication(
                Transpose(std(Transpose(m))),
                ones(m[0].length));
        double[][] snv = Division(tru, matrix_std);

        return snv;
    }
    /*
    Tinh rms root-mean-sqrt
     */
    public static double[][] rms(double[][] m1, double[][] m2){
        double[][] tru = new double[m1.length][m1[0].length];
        for(int i=0; i<m1.length;i++){
            for(int j=0; j<m1[0].length;j++){
                tru[i][j] = Math.pow((m1[i][j] - m2[0][j]), 2);
            }
        }
        // printMatrix(tru);
        double[][] mean_tru = mean(Transpose(tru));
        // printMatrix(mean_tru);
        double[][] rmes = new double[mean_tru.length][mean_tru[0].length];
        for(int i=0; i<mean_tru.length;i++){
            for(int j=0;j<mean_tru[0].length;j++){
                rmes[i][j] = Math.sqrt(mean_tru[i][j]);
            }
        }
        return rmes;
    }
    /*
    tim gia tri nho nhat trong matrix
     */
    public static int[] min(double[][] m1){
        double temp = m1[0][0];
        int[] vt = new int[2];
        for(int i=0; i<m1.length;i++){
            for(int j=0; j<m1[0].length;j++){
                if(temp > m1[i][j]){
                    temp = m1[i][j];
                    vt[0] = i;
                    vt[1] = j;
                }
            }
        }
        return vt;
    }
    /*
    in Matrix
     */
    public static void printMatrix(int[][] m){
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                System.out.print(m[i][j] + "\t");
            }
            System.out.println("\n");
        }
        System.out.println("----------------------------------->");
    }
    /*
    in Matrix
     */
    public static void printMatrix(double[][] m){
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                System.out.printf(m[i][j] + "\t", m[i][j]);
            }
            System.out.println("\n");
        }
        System.out.println("----------------------------------->");
    }
}
