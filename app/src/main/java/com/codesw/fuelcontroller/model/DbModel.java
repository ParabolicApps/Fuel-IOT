package com.codesw.fuelcontroller.model;

/**
 * this Class Will be used to create the data model appropietly with the SQL System
 *
 */
public class DbModel {
    public String data;
    public String date;
    public String time;
    public String type;
    public DbModel(String time, String date, String data, String type){
        this.date = date;
        this.data = data;
        this.time = time;
        this.type = type;
    }
    public String showData(){
        return "{"+"\"date\":"+date+","+"\"data\":"+data+","+"\"time\":"+time+","+"\"type\":"+type+"}";
    }
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
