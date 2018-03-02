package com.dd.transport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import android.os.Environment;

public class Route {
	private ArrayList<Stop> stops = new ArrayList<Stop>();
	private ArrayList<Time> times = new ArrayList<Time>();
	private String num, name, type, workingDays;
	private int id;
	private String[] depos = new String[2];
	private Route mainRoute = null;
	private String firstStopName = "", lastStopName = "";
	
	public Route() {
		num = "0";
		name = "";
		type = "";
		id = 0;
	}
	
	public Route(String name, int id, String type, String num, String wd) {
		this.name = name;
		this.type = type;
		this.num = num;
		this.id = id;
		this.workingDays = wd;
	}

	public void setDepos(String start, String end) {
		depos[0] = start;
		depos[1] = end;
	}
	
	
	public void setMainRoute(Route mr) {
		mainRoute = mr;
	}
	
	public void addStop(Stop s) {
		stops.add(s);
	}
	
	public String getWorkingDays() {
		return workingDays;
	}
	
	public int getID() {
		return id;
	}
	
	public String getType() {
		return type;
	}
	
	public String getNumber() {
		return num;
	}
	
	public String getStartDepo() {
		return depos[0];
	}

	public String getEndDepo() {
		return depos[1];
	}

	public Route getMainRoute() {
		return mainRoute;
	}
	
	public ArrayList<Stop> getStops() {
		return stops;
	}
	
	public int getStopNumByID(int stopID) {
		int index = 0;
		for (Stop stop: stops) {
			if (stopID == stop.getID()) {
				return index;
			}
			index++;
		}	
		return -1;
	}
	
	public Stop getStop(int stopID) {
		for (Stop stop: stops) {
			if (stop.getID() == stopID) {
				return stop;
			}
		}	
		return null;
	}
	
	public String getFirstStopName() {
		return firstStopName;
	} 
	
	public String getLastStopName() {
		return lastStopName;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return type + num;
	}
	
	public void addTime(Time t) {
		if (!times.contains(t)) {
			times.add(t);			
		}
	}
	
	public void setTimetable(String s, int stopID) {
		//String debug = this.toString() + ": \n";
		//writeFile(debug);
		int stopNum = getStopNumByID(stopID);
		if (stopNum == -1) {
			return;
		}

		String timesstr = s.substring(s.indexOf(',') + 1, s.indexOf(",,"));
		//debug += timesstr + "\n";
		String[] absMins = timesstr.split(",");

		String[] elements = s.split(",,");
		String[] ifWork = elements[3].split(",");
		int changeDays = 0;
		int routeOffset = -1;
		try {
			routeOffset = Integer.parseInt(ifWork[1]);
		}
		catch (Exception e) {}
		
		int prevTime = 0;
		
		for (int i = 0; i < absMins.length; i++) {
			try {
				int oneMin = Integer.parseInt(absMins[i]);
				if (i == routeOffset) {
					changeDays += 2;
					try {
						routeOffset += Integer.parseInt(ifWork[changeDays+1]);
					}
					catch (Exception e) {
						routeOffset = -1;
					}
				}
				if (oneMin == -1) {
					continue;
				}
				prevTime += oneMin;
				Time tmp = new Time(stopID, prevTime, ifWork[changeDays]);
				addTime(tmp);
				//debug += tmp.getTime() + " (" + tmp.getDays() + "), ";
			}
			catch (Exception e) {}
		}
		
		int index = 0;
		for (Time time: times) {
			//writeFile(time.getDays() + " (" + time.getTime() + "): ");
			if (time.getStopID() == stopID) {
				time.increaseTime(Time.getTotalWayTime(index, s, stopNum));				
				index++;
			}
		}		
		Collections.sort(times);
		//writeFile(debug);
	}

	public void setTerminalStops() {
		String[] terminals;
		if (name.contains(" - ")) {
			terminals = name.split(" - ");
		}
		else if (name.contains(" – ")) {
			terminals = name.split(" – ");
		}
		else if (name.contains("-")) {
			terminals = name.split("-");
		}
		else {
			terminals = name.split("–");
		}
		if (terminals.length > 1) {
			firstStopName = terminals[0].trim();
			lastStopName = terminals[1].trim();
		}
	}
	
	public boolean ifRouteStarts(int stopID) {
		if (stops.get(0) == getStop(stopID)) {
			return true;
		}
		return false;
	}
	
	public boolean ifRouteEnds(int stopID) {
		if (stops.get(stops.size() - 1) == getStop(stopID)) {
			return true;
		}
		return false;
	}	
	
	private int get17Day(int day) {
		if (day == 0) {
			day = 7;
		}
		return day;
	}
	
	public ArrayList<Time> getFullTimes(int stopID) {
		ArrayList<Time> r = new ArrayList<Time>();
		for (Time time: times) {
			if (stopID == time.getStopID()) {
				r.add(time);
			}	
		}
		return r;
	}
	
	public ArrayList<Integer> getCurrentTimes(int stopID) {
		ArrayList<Integer> r = new ArrayList<Integer>();
		Date curDate=new Date();
		//curDate.setHours(1);
		//curDate.setMinutes(0);
		//curDate.setDate(23);
		int curTime = curDate.getHours() * 60 + curDate.getMinutes();
		int day = get17Day(curDate.getDay());
		if (curTime < 240) {
			if (day == 1) {
				day = 7;
			}
			else {
				day--;
			}
			curTime += (24 * 60);
		}
	
		int timesNum = 0;
		int index = 0;
		for (Time time: times) {
			if (stopID == time.getStopID() && time.getDays().contains(((Integer)day).toString()) && curTime <= time.getTime()) {
				if (r.size() == 0 && index > 0) {
					int tmpTime = times.get(index - 1).getTime() - curTime;
					if (tmpTime > -6) {
						r.add(tmpTime);
					}
				}
				r.add(time.getTime() - curTime);
				timesNum++;
			}	
			index++;
			if (timesNum == 5) {
				break;
			}
		}		
		
		if (timesNum < 2) {
			for (Time time: times) {
				if (stopID == time.getStopID() && time.getDays().contains(((Integer) get17Day(day + 1)).toString())) {
					r.add(time.getTime() + 1440);
					break;
				}
			}
			//TODO ближайший рейс утром check
		}
		return r;
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

}


