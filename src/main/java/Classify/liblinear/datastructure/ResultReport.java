package Classify.liblinear.datastructure;

import Classify.TagConstant;

/**
 * Created by mhjang on 10/17/15.
 */
public class ResultReport {
    public double[] tableAccuracy = new double[2];
    public double[] codeAccuracy = new double[2];
    public double[] equAccuracy = new double[2];
    public double[] miscAccuracy = new double[2];

    public void setAccuracy(int componentID, double prec, double recall) {
        if(componentID == TagConstant.BEGINTABLE) {
            tableAccuracy[0] = prec;
            tableAccuracy[1] = recall;
        }

        if(componentID == TagConstant.BEGINEQU) {
            equAccuracy[0] = prec;
            equAccuracy[1] = recall;
        }

        if(componentID == TagConstant.BEGINCODE) {
            codeAccuracy[0] = prec;
            codeAccuracy[1] = recall;
        }

        if(componentID == TagConstant.BEGINMISC) {
            miscAccuracy[0] = prec;
            miscAccuracy[1] = recall;
        }
    }
    public void print() {
        System.out.println("TABLE \t" + tableAccuracy[0] + "\t" + tableAccuracy[1]);
        System.out.println("CODE \t" + codeAccuracy[0] + "\t" + codeAccuracy[1]);
        System.out.println("EQUATION \t" + equAccuracy[0] + "\t" + equAccuracy[1]);
        System.out.println("MISC \t" + miscAccuracy[0] + "\t" + miscAccuracy[1]);
    }

}
