package com.dd.transport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

import android.os.Environment;

public class Stop {
	private double lat, lon;
	private String name;
	private int id;
	private boolean isAdded = false, ttset = false;
	private ArrayList<Route> routes = new ArrayList<Route>();
	
	public Stop(int id, String name, double lon, double lat) {
		this.id = id;
		if (name.contains("~")) {
			name = name.substring(0, name.indexOf('~'));
			while (name.charAt(name.length() - 1) == '(' || name.charAt(name.length() - 1) == ' ') {
				name = name.substring(0, name.length() - 1);				
			}
		}
		this.name = name;
		this.lon = lon;
		this.lat = lat;
	}
	
	public Stop() {
		id = 0;
		name = "";
		lat = 0;
		lon = 0;
	}
	
	public void addRoute(Route r) {
		if (routes.contains(r)) {
			return;
		}
		routes.add(r);
	}
	
	public Route getRouteByIndex(int index) {
		int i = 0;
		for (Route route: routes){
			if (route.getMainRoute() != null) {
				continue;
			}
			else if (i == index) {
				return route;
			}
			else {
				i++;
			}
		}
		return null;
	}
	
	public String getRoutesString(String colorBus, String colorTrol, String colorTram, String colorMetro) {
		StringBuffer sb = new StringBuffer();
		String prevType = "";
		String prevNumber = "";
		for (Route route: routes) {
			if (route.getNumber().equals(prevNumber)) {
				continue;
			}
			prevNumber = route.getNumber();
			if (!prevType.equals("") && !prevType.equals(route.getType())) {
				sb.append('\n');
			}
			
			String color = "#FFFFFF";
			String type = route.getType();
			if (type != null) {
				if (type.equals("А")) {
					color = colorBus;
				}				
				else if (type.equals("Т")) {
					color = colorTrol;
				}
				else if (type.equals("Трам")) {
					color = colorTram;
				}
				else if (type.equals("М")) {
					color = colorMetro;
				}
			}
			
			sb.append("<font color = " + color + " >" + type + route.getNumber() + "</font> ");
			prevType = route.getType();
		}	
		return sb.toString();
	}
	
	public void setTimetable(ArrayList<String> lines) {
		if (ttset) {
			return;
		}
		for (String line: lines) {
			int routeID = -1;
			try {
				routeID=Integer.parseInt(line.substring(0, line.indexOf(',')));
			}	
			catch (Exception e) {}
			for (Route route: routes) {
				if (routeID == route.getID()) {
					route.setTimetable(line, id);
				}
			}
		}
		ttset = true;		
	}
	
	
	public ArrayList<String> getBriefTimetableStrings(String colorBus, String colorTrol, String colorTram, String colorMetro) {
		ArrayList<String> timesStrings = new ArrayList<String>();
		int mainID =0 ;
		String lastStopName = "";
		
		ArrayList<Integer> routeTimes = new ArrayList<Integer>();
		StringBuffer sb = null;

		String color, type;
		
		//TODO проверить, включаются ли идущие в парк в brief
		for (Route route: routes) {
			//TODO подумать, как быть с дублированием на конечных (например, Городской Вал)
			//debug += route.toString() + ", " + route.getName() + ", main: ";
			//if (route.getMainRoute().equals(null)) debug += "null\n";
			//else debug += route.getMainRoute().getName() + "\n";

			color = "#FFFFFF";
			type = route.getType();
			if (type != null) {
				if (type.equals("А")) {
					color = colorBus;
				}				
				else if (type.equals("Т")) {
					color = colorTrol;
				}
				else if (type.equals("Трам")) {
					color = colorTram;
				}
				else if (type.equals("М")) {
					color = colorMetro;
				}
			}
			
			if (route.getMainRoute() == null){
				if (sb != null) {
					timesToString(sb, routeTimes, lastStopName, route.getType());
					timesStrings.add(sb.toString());
				}
				sb = new StringBuffer();
				mainID = route.getID();
				lastStopName = route.getLastStopName();
				sb.append("<font color = " + color + ">" + route.toString() + "</font>");
				if (route.getType().equals("М")) {
					sb.append(" (" + route.getName() + ")");
				}
				sb.append(": ");
				routeTimes.clear();
			}
			if (route.getID() == mainID || route.getMainRoute().getID() == mainID) {
				routeTimes.addAll(route.getCurrentTimes(id));
			}
			
			if (routes.get(routes.size() - 1).equals(route)) {
				timesToString(sb, routeTimes, lastStopName, route.getType());
				timesStrings.add(sb.toString());
			}			
		}
		return timesStrings;
	}

	private void timesToString(StringBuffer sb, ArrayList<Integer> routeTimes, String lastStopName, String routeType) {
		int hour = 0, min = 0;
		Collections.sort(routeTimes);
		int timesNum = 0;
		for (Integer routeTime: routeTimes) {
			if (routeTime > 0 && routeTime < 1440) {
				sb.append("+");
			}
			if (routeTime < 60) {
				sb.append(routeTime + " мин ");							
			}
			else {
				if (routeTime > 1440) {
					routeTime -= 1440;
					sb.append(getStringTime(routeTime) + " (утром)");
				}
				else {
					hour = routeTime / 60;
					min = routeTime - hour * 60;
					sb.append(hour + "ч " + min + " мин ");									
				}
			}
			timesNum++;
			if (timesNum == 5) {
				break;
			}
		}
		if (timesNum == 0) {
			sb.append("До утра ходить не будет.");
		}
		if (!routeType.equals("М")) {
			sb.append(" (" + lastStopName + ")");
		}
	}
	
	public String getFullTimetable(int routeID) {
		StringBuffer sb = new StringBuffer();
		ArrayList<Time> times = new ArrayList<Time>();
		for (Route route: routes) {
			if (route.getID() == routeID || (route.getMainRoute() != null && route.getMainRoute().getID() == routeID)) {
				times.addAll(route.getFullTimes(id));
			}
		}
		Collections.sort(times);
		
		String prevDays = "";
		int prevHour = -1;
		for (Time time: times) {
			if (prevDays.equals("") || !time.getDays().equals(prevDays)) {
				if (sb.length() > 0) {
					sb.append("<br><br>");
				}
				sb.append("<u><i><b>" + getStringDays(time.getDays().charAt(0)));
				if (time.getDays().length() > 1) {
					sb.append("-" + getStringDays(time.getDays().charAt(time.getDays().length() - 1)));
				}
				sb.append("</b></i></u>");
				prevDays = time.getDays();
			}
			if ((int) time.getTime() / 60 != prevHour) {
				sb.append("<br>");
				prevHour = time.getTime() / 60;
			}
			sb.append(getStringTime(time.getTime()) + " ");
		}		
		
		return sb.toString();
	}
	
	public String getType() {
		if (routes.size() == 0) {
			return "-1";
		}
		return routes.get(routes.size()-1).getType();
	}
	
	public Integer getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public double getLat() {
		return lat;
	}
	
	public double getLon() {
		return lon;
	}

	public boolean closeEnough(double south, double west, double north, double east) {
		if (lat >= south && lat <= north && lon >= west && lon <= east) {
			return true;
		}
		return false;
	}
	
	public boolean getIsAdded() {
		return isAdded;
	}
	
	public void setIsAdded(boolean state) {
		isAdded = state;
	}

	//для отладки
	public String toString() {
		return "name: " + name + ", id: " + id +", lat: " + lat + ", lon: " + lon + ", routes: " + routes.size() + "\n";
	}

	private String getStringTime(int n) {
		Integer h = n / 60;
		Integer m = n % 60;
		if (h >= 24) {
			h -= 24;
		}
		String min;
		if (m < 10) {
			min="0" + m;
		}
		else {
			min = m.toString();
		}
		return h + ":" + min;
	}


	private String getStringDays(char day) {
		if (day == '1') {
			return "пн";
		}
		else if (day == '2') {
			return "вт";
		}
		else if (day == '3') {
			return "ср";
		}
		else if (day == '4') {
			return "чт";
		}
		else if (day == '5') {
			return "пт";
		}
		else if (day == '6') {
			return "сб";
		}
		else if (day == '7') {
			return "вс";
		}
		return "";
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

