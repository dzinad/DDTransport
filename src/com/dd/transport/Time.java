package com.dd.transport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Environment;

public class Time implements Comparable<Time> {
	private int time, stopID;
	private String days;
	
	public Time() {
		time = 0;
		stopID = 0;
		days = "";
	}

	public Time(int sid, int t, String d) {
		time = t;
		stopID = sid;
		days = d;
	}
	
	public String getDays() {
		return days;
	}
	
	public int getStopID() {
		return stopID;
	}

	public void increaseTime(int toAdd) {
		time += toAdd;
	}

	public int getTime() {
		return time;
	}
	
	static int getTotalWayTime(int routeNo, String s, int lastStopNum) {
		//String debug = "";
		if (lastStopNum == 0) {
			return 0;
		}
		int totalWayTime = 0;
		int stopNum = 0;
		String[] items=s.split(",,"); 
		//с четвертого элемента и до конца строки начинается то, что нам нужно
		for (int i = 4; i < items.length; i++) {
			int oneWayTime = 0;
			String[] elements = items[i].split(",");
			if (elements.length == 1) {	
				try {
					oneWayTime = Integer.parseInt(elements[0]);
				}
				catch (Exception e) {}
			}	
			else {
				try {
					int offSet = 0;
					int prevRouteNo = 0;
					if (routeNo < Integer.parseInt(elements[1])) {
						oneWayTime=Integer.parseInt(elements[0]);
					}
					else {
						for (int j = 1; j < elements.length - 1; j += 2) {
							offSet += Integer.parseInt(elements[j + 1]) - 5;
							if (routeNo >= Integer.parseInt(elements[j]) + prevRouteNo) {
								if (j == elements.length - 2) {	
									oneWayTime = Integer.parseInt(elements[0]) + offSet;
									prevRouteNo += Integer.parseInt(elements[j]);
									break;
								}
								else {
									if (routeNo >= Integer.parseInt(elements[j]) + Integer.parseInt(elements[j + 2]) + prevRouteNo) {	
										prevRouteNo+=Integer.parseInt(elements[j]);
										continue;
									}	
									oneWayTime = Integer.parseInt(elements[0]) + offSet;
									prevRouteNo += Integer.parseInt(elements[j]);
									break;
								}	
							}	
							prevRouteNo += Integer.parseInt(elements[j]);
						}
					}
				}
				catch (Exception e) {}			
			}	
		
			//debug += oneWayTime + ", ";
			totalWayTime += oneWayTime;
			stopNum++;
			if (stopNum == lastStopNum) {
				break;
			}
		}	
		//writeFile(debug);
		//writeFile(debug + ", total: " + totalWayTime);
		return totalWayTime;
	}

	
	//TODO убрать
	private static void writeFile(String content) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}
		File sdPath = Environment.getExternalStorageDirectory();
		sdPath = new File(sdPath.getAbsolutePath() + "/com.dd.transport");
		sdPath.mkdirs();
		File sdFile = new File(sdPath, "debug.txt");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile, true));
			bw.write(content);
			bw.close();
		}
		catch (IOException e) {}
	}

	@Override
	public int compareTo(Time another) {
		if (this.stopID != another.stopID) {
			return this.stopID - another.stopID;
		}
		
		if (this.days.compareTo(another.days) != 0) {
			return this.days.compareTo(another.days);
		}
		return this.time - another.time;
	}


}


