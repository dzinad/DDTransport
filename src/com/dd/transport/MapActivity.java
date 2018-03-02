package com.dd.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.TreeMap;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class MapActivity extends Activity {
    private CheckBox mTrafficCheckbox, mMyLocationCheckbox, mBuildingsCheckbox, mMapViewCheckbox;
    private TreeMap<Integer, Stop> stops = new TreeMap<Integer, Stop>();
    private ArrayList<Route> routes = new ArrayList<Route>();
	private ArrayList<String> timeLines;
	private HashSet<Marker> markers = new HashSet<Marker>();
	private String secStore;
	private Route curRoute = null;
	private Stop curStop = null;
	private File fileStops, fileRoutes, fileTimes;
	private GoogleMap mMap;
	private HashMap<Route, Polyline> routePolylines = new HashMap<Route, Polyline>();
	private double previousZoomLevel = 1; 
	private final double zoomForMarkers = 13.5, zoomForRoutes = 11.5;
	private double savedZoomLevel = -1;
	private LatLng savedPosition = null;
	private LinearLayout routeOnMapInfoLayout;
	private HashMap<Integer, TableRow> routesOnMapLayouts = new HashMap<Integer, TableRow>();
	private final int[] routesColors = {Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN, Color.BLACK, Color.MAGENTA, Color.GRAY};
	int colorIndex = 0;
    //static boolean active = false;
    //static Bundle saved;
    private class Bounds {
    	public double EAST, WEST, NORTH, SOUTH;
    	public Bounds(double east, double west, double north, double south) {
    		EAST = east;
    		WEST = west;
    		NORTH = north;
    		SOUTH = south;
    	}
    }
    private final Bounds bounds = new Bounds(27.74, 27.37, 54.0, 53.81);
    private final LatLng centerLatLng = new LatLng(53.9, 27.5667);
    
    /*@Override
    public void onStart() {
       super.onStart();
       active = true;
    } */

    /*@Override
    public void onStop() {
       super.onStop();
       saved = new Bundle();
    }*/	
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
				
        try {
            super.onCreate(savedInstanceState);
        	setContentView(R.layout.activity_map);
        	
    		secStore = Environment.getExternalStorageDirectory().toString() + File.separator + getString(R.string.directory);
        	mMapViewCheckbox = (CheckBox) findViewById(R.id.satellite);
        	routeOnMapInfoLayout = (LinearLayout) findViewById(R.id.routeOnMapInfoLayout);
            		
            if (!filesReady()) {
        		Intent intent = new Intent(MapActivity.this, MenuActivity.class);
        		intent.putExtra("ifNeedUpdate", true);        		
        		startActivity(intent);						            	
            	return;
            }        
            
            Intent intent = getIntent();
            double lat = intent.getDoubleExtra("lat", -1);
            double lng = intent.getDoubleExtra("lng", -1);
            savedZoomLevel = intent.getDoubleExtra("zoom", -1);
            if (lat != -1 && lng != -1) {
            	savedPosition = new LatLng(lat, lng);
            }
            
        	previousZoomLevel = 1;

        	final ParseTask parseTask = new ParseTask();
        	try {
        		parseTask.execute();
        	}
        	catch (Exception e)	{}
        }
        catch (Exception e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.unknownError), Toast.LENGTH_LONG).show();
        }
	
	}

	private boolean filesReady() {
		
		File folder = new File(secStore);
		if (folder.exists()) {
			File fileStops = new File(secStore, getString(R.string.stopsFileName));
			File fileRoutes = new File(secStore, getString(R.string.routesFileName));
			File fileTimes = new File(secStore, getString(R.string.timesFileName));
			if (fileStops.exists() && fileRoutes.exists() && fileTimes.exists()) {
				return true;
			}
		}		
		return false;
	}
	
	private void parseStops() {
		fileStops = new File(secStore, getString(R.string.stopsFileName));
		int id;
		double lat, lon;
		String curName = "";
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(fileStops));
			br.readLine(); //в первой строчке нет нужных данных
			
			String line;
			while (true) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				
				String[] items=line.split(";");				

				try {
					if (!items[4].equals("")) {
						curName = items[4];
					}
					id = Integer.parseInt(items[0]);
					lon = Double.parseDouble(items[6]) / 100000;
					lat = Double.parseDouble(items[7]) / 100000;					
					if (id != 0 && lon != 0 && lat != 0) {
						stops.put(id, new Stop(id, curName, lon, lat));
					}
				}
				catch (Exception e) {} //ошибка парсинга или недопустимый индекс				
			}
			br.close();
		} 
		catch (FileNotFoundException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.fileNotFoundError), Toast.LENGTH_LONG).show();
		}
		catch (IOException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.unknownError), Toast.LENGTH_LONG).show();
		}				
	}

	private void parseRoutes() {
		String debug = "";
		boolean numberRepeated = false;
		try {
			fileRoutes = new File(secStore, getString(R.string.routesFileName));
			BufferedReader br = new BufferedReader(new FileReader(fileRoutes));
			br.readLine(); //первая строчка
			String line;
			String curName = "", curNumber = "", curType = "", curStopsList = "", curWorkingDays = "", curDepoline = "";
			int curID;
		    while (true) {
		    	line = br.readLine();
			    if (line == null) {
			    	break;
			    }
			      
			    String[] items = line.split(";");
			    String[] depos;
			      
			    try {
			    	if (!items[0].equals("")) {
			    		curNumber = items[0];
			    		numberRepeated = false;
			    	}
			    	else {
			    		numberRepeated = true;
			    	}
			    	if (!items[3].equals("")) {
				    	curType = items[3];			    		  
			    	}
			    	curName = items[10];
			    	curStopsList = items[14];		    		  
			    	curID = Integer.parseInt(items[12]);
			    	curWorkingDays = items[11];
					curDepoline = items[8];
					depos = curDepoline.split(">");
			    }
			    catch (Exception e) {
			    	continue;
			    }
			    
			    if (curType.equals("bus")) {
			    	curType = "А"; //кириллица
			    }
			    else if (curType.equals("trol")) {
			    	curType = "Т";
			    }
			    else if (curType.equals("tram")) {
			    	curType = "Трам";
			    }
			    else if (curType.equals("metro")) {
			    	curType = "М";
			    }
			    if (curType.equals("М")) {
			    	curNumber = getMetroNum(curNumber);
			    }
			    Route tmproute = new Route(curName, curID, curType, curNumber, curWorkingDays);
			    tmproute.setDepos(depos[0], depos[1]);
				if (numberRepeated) {
					tmproute.setMainRoute(getMainRoute(tmproute));
				}
				routes.add(tmproute);

			    //добавить остановки
				tmproute = routes.get(routes.size() - 1);
			    String[] stopIDs = curStopsList.split(",");
			    for (int i = 0; i < stopIDs.length; i++) {
			    	try {
			    		Stop tmpStop = stops.get(Integer.parseInt(stopIDs[i]));
			    		
			    		if (tmpStop != null) {
			    			tmproute.addStop(tmpStop);
				    		tmpStop.addRoute(tmproute);
			    		}
			    	}
			    	catch (Exception e) {}
			    }	  
			    tmproute.setTerminalStops();			    			    
		    }
		    br.close();
		}
		catch (FileNotFoundException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.fileNotFoundError), Toast.LENGTH_LONG).show();
		}
	    catch (IOException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.unknownError), Toast.LENGTH_LONG).show();
	    }
		writeFile(debug);
	}
		
	private void parseTimes() {
		timeLines = new ArrayList<String>();
		try {
			fileTimes = new File(secStore, getString(R.string.timesFileName));
			BufferedReader br = new BufferedReader(new FileReader(fileTimes));
			String line;
			while (true) {
				line = br.readLine();
				if (line == null) {
					break;
				}
				if ((int) line.charAt(0) == 65279) { //удалим первый символ (видимо, служебный какой-то, понятия не имею, откуда он там берется)
					line = line.substring(1);
				}
				timeLines.add(line);
			}
			br.close();
		}
		catch (FileNotFoundException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.fileNotFoundError), Toast.LENGTH_LONG).show();
		}
	    catch (IOException e) {
        	Toast.makeText(this.getApplicationContext(), getString(R.string.unknownError), Toast.LENGTH_LONG).show();
	    }
	}
	
	private Route getMainRoute(Route curroute) {
		ListIterator<Route> it = routes.listIterator(routes.size());
		Route route;
		while(it.hasPrevious()) {
			route = (Route) it.previous();
			if (!route.getNumber().equals(curroute.getNumber()) || 
					!route.getType().equals(curroute.getType())) 
			{
				break;
			}
			if (!route.equals(curroute) && route.getMainRoute() == null && 
					(curroute.getStartDepo().contains(route.getStartDepo()) || 
						curroute.getEndDepo().contains(route.getEndDepo()))) 
			{
				return route;
			}
		}
		return null;
	}
	
	public void preSetMap() {
        if (!checkReady()) {
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        }
        if (checkReady()) {
            Location loc = new Location(LocationManager.GPS_PROVIDER);
            loc.setLatitude(centerLatLng.latitude);
            loc.setLongitude(centerLatLng.longitude);                
            LatLng latlng = new LatLng(loc.getLatitude(),loc.getLongitude());
            previousZoomLevel = 13;
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) previousZoomLevel));
        }		
	}
	
	public void setMap() {
        if (!checkReady()) {
        	preSetMap();
        	setMap();
        }
        if (checkReady()) {
            mMap.setOnCameraChangeListener(getCameraChangeListener());
            mMap.setMyLocationEnabled(true);
            LatLng latlng = null;
            if (savedPosition != null) {
            	latlng = savedPosition;
            }
            else {
            	try {
                    LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
                    Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location == null) {
                    	location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    latlng = new LatLng(location.getLatitude(),location.getLongitude());               
                    
                    if (latlng.latitude < bounds.SOUTH || latlng.latitude > bounds.NORTH || latlng.longitude > bounds.EAST || latlng.longitude < bounds.WEST) {
            			latlng = centerLatLng;                	                	
                    }
            	}
            	catch (Exception e) {
        			latlng = centerLatLng;
            	}            	
            }
            if (savedZoomLevel != -1) {
            	previousZoomLevel = savedZoomLevel;
            }
            else {
            	previousZoomLevel = 14.6;        		
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, (float) previousZoomLevel));
            addMarkers(latlng);
        }
            
    }
	
	public void backToBriefTimetable(View v) {
		closeFullTimetable(v);
    	final TimetableTask timetableTask = new TimetableTask(MapActivity.this, curStop);
    	try {
    		timetableTask.execute();
    	}
    	catch (Exception e)	{}
	}
	
	public void closeBriefTimetable(View v) {
		LinearLayout ll = (LinearLayout) findViewById(R.id.briefTTLayout);
		ll.removeAllViews();
		ll = (LinearLayout) findViewById(R.id.mainBriefTTLayout);
		ll.setVisibility(View.INVISIBLE);
		ll = (LinearLayout) findViewById(R.id.menuLayout);
		ll.setVisibility(View.VISIBLE);
	}
	
	public void closeFullTimetable(View v) {
		LinearLayout ll = (LinearLayout) findViewById(R.id.mainFullTTLayout);
		ll.setVisibility(View.INVISIBLE);				
		ll = (LinearLayout) findViewById(R.id.menuLayout);
		ll.setVisibility(View.VISIBLE);
		TextView tv = (TextView) findViewById(R.id.fullTTLayout);
		tv.setText("");
		tv = (TextView) findViewById(R.id.routeTitleTextView);
		tv.setText("");		
	}

	public void showFullTimetable(Stop curStop, Route route) {
		curRoute = route;
		LinearLayout ll = (LinearLayout) findViewById(R.id.mainBriefTTLayout);
		ll.setVisibility(View.INVISIBLE);
		ll = (LinearLayout) findViewById(R.id.mainFullTTLayout);
		ll.setVisibility(View.VISIBLE);
		TextView stopTitleTv = (TextView) findViewById(R.id.stopTitleFullTextView);
		TextView routeTitleTv = (TextView) findViewById(R.id.routeTitleTextView);
		TextView timesTv = (TextView) findViewById(R.id.fullTTLayout);
		String stopName = curStop.getName();
		String routeInfo = route.toString() + " (" + route.getLastStopName() + ")";		
		String timesInfo = curStop.getFullTimetable(route.getID());		
		stopTitleTv.setText(stopName);
		routeTitleTv.setText(routeInfo);
		timesTv.setText(Html.fromHtml(timesInfo));
	}
	
	public void showMenu(View v) {
		Intent intent = new Intent(MapActivity.this, MenuActivity.class);
		CameraPosition position = mMap.getCameraPosition();
		intent.putExtra("lat", position.target.latitude);
		intent.putExtra("lng", position.target.longitude);
		intent.putExtra("zoom", position.zoom);
		startActivity(intent);				
	}
	
	public void showRouteOnMap(View v) {
		savedZoomLevel = mMap.getCameraPosition().zoom;
		savedPosition = mMap.getCameraPosition().target;
		closeFullTimetable(v);
		previousZoomLevel = zoomForRoutes;
		setMarkersVisibility(false);

		routeOnMapInfoLayout = (LinearLayout) findViewById(R.id.routeOnMapInfoLayout);
		routeOnMapInfoLayout.setVisibility(View.VISIBLE);
		
		addRouteOnMapLine();
		
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerLatLng, (float) previousZoomLevel));
		PolylineOptions rectOptions = new PolylineOptions();
		rectOptions.color(routesColors[colorIndex]);
		for (Stop stop: curRoute.getStops()) {
			LatLng latlng = new LatLng(stop.getLat(), stop.getLon());
			rectOptions.add(latlng);
			addOneMarker(stop);
		}
		routePolylines.put(curRoute, mMap.addPolyline(rectOptions));
		addRoutesOnMapMarkers();
		colorIndex++;
		if (colorIndex >= routesColors.length) {
			colorIndex = 0;
		}
	}
	
	public void eraseRouteFromMap(Route route) {
		TableRow tr = routesOnMapLayouts.get(route.getID());
		if (tr == null) {
			return;
		}
		routeOnMapInfoLayout.removeView(tr);
		routesOnMapLayouts.remove(route.getID());
		routePolylines.get(route).remove();
		routePolylines.remove(route);
		setMarkersVisibility(false);
		addRoutesOnMapMarkers();
		
		if (routesOnMapLayouts.size() == 0) {
			colorIndex = 0;
			routeOnMapInfoLayout.setVisibility(View.INVISIBLE);
	        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(savedPosition, (float) savedZoomLevel));
	        handleMapChange(mMap.getCameraPosition());			
		}
	}
	
	private void addRouteOnMapLine() {
		TableRow tr = new TableRow(routeOnMapInfoLayout.getContext());
		tr.setGravity(Gravity.CENTER_VERTICAL);
		tr.setLayoutParams(new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
	            LayoutParams.WRAP_CONTENT));
		
		TextView tv = new TextView(tr.getContext());
		TableRow.LayoutParams params = new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
	            LayoutParams.WRAP_CONTENT);		
		params.setMargins(10,  0,  20, 0);
		tv.setLayoutParams(params);
		tv.setTextSize(20);
		tv.setTypeface(null, Typeface.BOLD);
		tv.setText(curRoute.toString());
		
		View v = new View(tr.getContext());
		v.setLayoutParams(new TableRow.LayoutParams(75, 3));
		v.setBackgroundColor(routesColors[colorIndex]);
		
		final Route routeToPass = curRoute;
		ImageView iv = new ImageView(tr.getContext());
		iv.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				eraseRouteFromMap(routeToPass);				
			}
			
		});
		iv.setImageResource(R.drawable.close);
		iv.setAlpha(0.75f);
		iv.setLayoutParams(new TableRow.LayoutParams(LayoutParams.WRAP_CONTENT,
	            LayoutParams.WRAP_CONTENT));
		 
		tr.addView(tv);
		tr.addView(v);
		tr.addView(iv);

		routeOnMapInfoLayout.addView(tr);
		
		routesOnMapLayouts.put(curRoute.getID(), tr);
	}
	
	private String getMetroNum(String s) {
		if (s.startsWith("M")) {
			return s.substring(1);
		}
		return s;
	}
	
	private boolean checkReady() {
        if (mMap == null) {
            return false;
        }
        return true;
    }

    //Called when the Traffic checkbox is clicked.
    public void onTrafficToggled(View view) {
        updateTraffic();
    }

    private void updateTraffic() {
        if (!checkReady()) {
            return;
        }
        mMap.setTrafficEnabled(mTrafficCheckbox.isChecked());
    }

    
    //Called when the MyLocation checkbox is clicked.
    public void onMyLocationToggled(View view) {
        updateMyLocation();
    }

    private void updateMyLocation() {
        if (!checkReady()) {
            return;
        }
        mMap.setMyLocationEnabled(mMyLocationCheckbox.isChecked());
    }

    //Called when the Buildings checkbox is clicked.
    public void onBuildingsToggled(View view) {
        updateBuildings();
    }

    private void updateBuildings() {
        if (!checkReady()) {
            return;
        }
        mMap.setBuildingsEnabled(mBuildingsCheckbox.isChecked());
    }
    
    public void changeMapView(View v) {
    	if (!checkReady()) {
    		return;
    	}
    	if (mMapViewCheckbox.isChecked()) {
    		mMap.setMapType(mMap.MAP_TYPE_SATELLITE);
    	}
    	else {
    		mMap.setMapType(mMap.MAP_TYPE_NORMAL);
    	}
    }
    
    public OnCameraChangeListener getCameraChangeListener() {
        return new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
            	handleMapChange(position);
            }
        };
    }
    
    private void handleMapChange(CameraPosition position) {
        if (position.zoom >= zoomForMarkers) {
    		addMarkers(position.target);                	
        }
        if(previousZoomLevel != position.zoom) {
        	if (previousZoomLevel < zoomForMarkers && position.zoom >= zoomForMarkers) {
        		setMarkersVisibility(true);
        		addMarkers(position.target);
        		previousZoomLevel = position.zoom;
        	}	
        	else if (previousZoomLevel >= zoomForMarkers && position.zoom < zoomForMarkers) {
        		setMarkersVisibility(false);
        		addRoutesOnMapMarkers();
        		previousZoomLevel = position.zoom;                		
        	}	
        }    	
    }
    
    private void setMarkersVisibility(boolean b) {
    	for (Marker marker: markers) {
    		marker.setVisible(b);
    		/*try {
    			stops.get(Integer.parseInt(marker.getSnippet())).setIsAdded(false);
    		}
    		catch (Exception e) {}*/
    	}
    	if (b == false) {
    		markers.clear();
    	}
    }
    
    private void addRoutesOnMapMarkers() {
		for (Route route: routePolylines.keySet()) {
			for (Stop stop: route.getStops()) {
				addOneMarker(stop);
			}
		}
    }
    
    private void addMarkers(LatLng pos) {
        double south = 0, west = 0, north = 0, east = 0;
        try {
        	south = mMap.getProjection().getVisibleRegion().nearRight.latitude;
            north = mMap.getProjection().getVisibleRegion().farLeft.latitude;
            east = mMap.getProjection().getVisibleRegion().nearRight.longitude;
            west = mMap.getProjection().getVisibleRegion().farLeft.longitude;
        }
        catch (Exception e) {}
        
        for (Stop stop: stops.values()) {	
            //if (!stop.closeEnough(south, west, north, east) || stop.getIsAdded() || stop.getType().equals("-1")) {
            if (!stop.closeEnough(south, west, north, east) || stop.getType().equals("-1")) {
            	continue;
            }
            addOneMarker(stop);
        }
    }

    private void addOneMarker(Stop stop) {
    	/*if (stop.getIsAdded()) {
    		return;
    	}*/
        Marker marker = mMap.addMarker(new MarkerOptions()
               	.position(new LatLng(stop.getLat(), stop.getLon()))
               	.title(stop.getName())
               	.snippet(stop.getID().toString())
               	);

        
        if (stop.getType().equals("А") || stop.getType().equals("Т")) {
        	marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        }
        else if (stop.getType().equals("Трам")) {
        	marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        }
        else if (stop.getType().equals("М")) {
        	marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        }

        //stop.setIsAdded(true);
        markers.add(marker);
        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
        	@Override
        	public boolean onMarkerClick(Marker marker) {            		
        		curStop = stops.get(Integer.parseInt(marker.getSnippet()));
            	final TimetableTask timetableTask = new TimetableTask(MapActivity.this, curStop);
            	try {
            		timetableTask.execute();
            	}
            	catch (Exception e)	{}
        		return true;
        	}
        });    	
    }
    
	private class ParseTask extends AsyncTask<String, Integer, String> {
        private LinearLayout ll; 
        
	    public ParseTask() {}
	    
	    @Override
	    protected void onPreExecute() {
	    	preSetMap();
	        super.onPreExecute();
	        ll = (LinearLayout) findViewById(R.id.spinner);
	        ll.setVisibility(View.VISIBLE);
	    }


	    @Override
	    protected void onPostExecute(String result) {
	    	super.onPostExecute("");
	    	try {
	    		setMap();
	    	}
	    	catch (Exception e) {}
	    	ll.setVisibility(View.INVISIBLE);
	    }	    
	    
	    @Override
	    protected String doInBackground(String... arg0) {
	        parseStops();
	        parseRoutes();
	        parseTimes();	    	
	    	return null;
	    }
	}

	private class TimetableTask extends AsyncTask<String, Integer, String>{
        private LinearLayout contentll; 
        private Context context;
        private TextView stopTitleTv, routesOverviewTv;
        private Stop curStop = null;
        private int curStopID = 0;
        private ArrayList<TextView> timesTvs = new ArrayList<TextView>();
        
	    public TimetableTask(Context context, Stop curStop) {
	    	this.context = context;
	    	this.curStop = curStop;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        LinearLayout ll = (LinearLayout) findViewById(R.id.mainBriefTTLayout);
	        ll.setVisibility(View.VISIBLE);
	        ll = (LinearLayout) findViewById(R.id.menuLayout);
	        ll.setVisibility(View.INVISIBLE);
	    	contentll = (LinearLayout) findViewById(R.id.briefTTLayout);
	    	contentll.removeAllViews();
	    	stopTitleTv = (TextView) findViewById(R.id.stopTitleTextView);
	    	routesOverviewTv = (TextView) findViewById(R.id.routesOverviewTextView);
	    	stopTitleTv.setText(curStop.getName());
	    	routesOverviewTv.setText(getString(R.string.loading));
	    	curStopID = curStop.getID();
	    }


	    @Override
	    protected void onPostExecute(String result) {
	    	super.onPostExecute("");
	    	if (curStop == null) {
	    		return;
	    	}
	    	for (TextView timeTv: timesTvs) {
	    		contentll.addView(timeTv);
	    	}
	    	try {
	    		routesOverviewTv.setText(Html.fromHtml(curStop.getRoutesString(
	    				getString(R.string.color_bus),
	    				getString(R.string.color_trol),
	    				getString(R.string.color_tram),
	    				getString(R.string.color_metro))));
	    	}
	    	catch (Exception e) {
	        	Toast.makeText(context, getString(R.string.unknownError), Toast.LENGTH_LONG).show();
	    	}
	    }	    
	    
	    @Override
	    protected String doInBackground(String... arg0) {
	    		
	    	curStop = stops.get(curStopID);
	    	if (curStop == null) {
	    		return "exception";
	    	}
	    	
	    	curStop.setTimetable(timeLines);
	    	ArrayList<String> lines = curStop.getBriefTimetableStrings(
	    			getString(R.string.color_bus),
    				getString(R.string.color_trol),
    				getString(R.string.color_tram),
    				getString(R.string.color_metro));
	    	int index = 0;
	        for (String line: lines) {
	        	TextView textView = new TextView(contentll.getContext());
	        	textView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	        	textView.setTextSize(17);
	        	textView.setText(Html.fromHtml(line));
	        	timesTvs.add(textView);
	        	final Route curroute = curStop.getRouteByIndex(index);
	        	textView.setOnClickListener(new TextView.OnClickListener() {	        		    
	        		public void onClick(View v) {
	        		    showFullTimetable(curStop, curroute);
	        		}
	        	});
	        	index++;
	        }	        
	    	return null;
	    }
	}

	//для отладки
	private void writeFile(String content) {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			return;
		}
		File sdPath = Environment.getExternalStorageDirectory();
		sdPath = new File(sdPath.getAbsolutePath() + "/com.dd.transport");
		sdPath.mkdirs();
		File sdFile = new File(sdPath, "debug.txt");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(sdFile));
			bw.write(content);
			bw.close();
		}
		catch (IOException e) {}
	}
	
}


