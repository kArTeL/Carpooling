/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package carpooling;

import java.util.Calendar;

/**
 *
 * @author andressolano
 */
public class Ride {
    String origin, destiny;
    Calendar departTime, arrivalTime;
    int freeSeats, price;
    
    public Ride(String o, String d, String dt, String at, int fs, int p) {
        origin = o;
        destiny = d;
        departTime = setTime(dt);
        arrivalTime = setTime(at);
        freeSeats = fs;
        price = p;
    }
    
    private Calendar setTime(String dt) {
        String[] time = dt.split(":");
        Calendar cal = Calendar.getInstance();
	cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
        cal.set(Calendar.MINUTE, Integer.parseInt(time[1]));
        return cal;
    }
    //Gets and sets:
    
    public String getDepartTime() {
        return departTime.get(Calendar.HOUR_OF_DAY) +":"+ departTime.get(Calendar.MINUTE);
    }
    
    public String getArrivalTime() {
        return arrivalTime.get(Calendar.HOUR_OF_DAY) +":"+ arrivalTime.get(Calendar.MINUTE);
    }
    
}
