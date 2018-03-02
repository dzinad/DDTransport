package com.dd.transport;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MenuActivity extends Activity {

	private String[] options = {"Назад к карте", "Обновить данные"};
	private String secStore;
	private ProgressDialog mProgressDialog;
	private boolean ifNeedUpdate;
	private double savedLat, savedLng, savedZoom;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		secStore = Environment.getExternalStorageDirectory().toString() + File.separator + getString(R.string.directory);

		mProgressDialog = new ProgressDialog(MenuActivity.this);
		mProgressDialog.setMessage(getString(R.string.loading));
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
				
		updateTextViewInfo();

	    ListView listView = (ListView) findViewById(R.id.listView);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
	        android.R.layout.simple_list_item_1, options);
	    listView.setAdapter(adapter);
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	switch (position) {
            	case 0: //назад
            		backToMap();
            		break;
            	case 1: //обновить
            		update();
            		break;
            	default:
            	}
            }
        });

		
		Intent intent = getIntent();
		savedLat = intent.getDoubleExtra("lat", -1);
		savedLng = intent.getDoubleExtra("lng", -1);
		savedZoom = intent.getDoubleExtra("zoom", -1);
		ifNeedUpdate = intent.getBooleanExtra("ifNeedUpdate", false);
		if (ifNeedUpdate) {
			update();
		}
				
	}
	
	private void backToMap() {
		Intent intent = new Intent(MenuActivity.this, MapActivity.class);
		intent.putExtra("lat", savedLat);
		intent.putExtra("lng", savedLng);
		intent.putExtra("zoom", savedZoom);
		startActivity(intent);				
	}
	
	private void updateTextViewInfo() {
		TextView tv = (TextView) findViewById(R.id.menuInfo);
		File file=new File(secStore, getString(R.string.timesFileName));
		Date date=new Date();
        if(file.exists()) {
            Long lastModified = file.lastModified();
            date = new Date(lastModified);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");            
    		tv.setText("Дата последнего обновления данных: " + formatter.format(date));
        }
        else {
        	tv.setText("Данные отсутствуют.");
        }		
	}
	
	public void update() {
		File folder = new File(secStore);
		boolean success = true;
		if (folder.exists()) {
			startDownload();
		}
		else {
			success = folder.mkdir();
		}
		if (success) {
			startDownload();
		} 
		else {
	        Toast.makeText(this.getApplicationContext(), "Ошибка: невозможно создать папку.", 
	        		   Toast.LENGTH_SHORT).show();
		}						
	}

	private void startDownload() {
		final DownloadTask downloadTask = new DownloadTask(MenuActivity.this);
		downloadTask.execute();
	}
	
	private class DownloadTask extends AsyncTask<String, Integer, String> {
		private Context context;
		private String res1 = "", res2 = "", res3 = "";
	    private PowerManager.WakeLock mWakeLock;

	    public DownloadTask(Context context) {
			this.context = context;
		}

	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	             getClass().getName());
	        mWakeLock.acquire();
	        mProgressDialog.show();
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setIndeterminate(false);
	        mProgressDialog.setMax(100);
	        mProgressDialog.setProgress(progress[0]);
	    }

	    @Override
	    protected void onPostExecute(String result) {
	        mWakeLock.release();
	        mProgressDialog.dismiss();
	        String resstr;
	        if (res1 != null || res2 != null || res3 != null)
	        	resstr = "Сервер временно недоступен.";
	        else {
	        	resstr = "Обновление завершено.";
	        }
	        Toast.makeText(this.context, resstr, Toast.LENGTH_SHORT).show();
	        updateTextViewInfo();
	        if (ifNeedUpdate) {
	        	backToMap();
	        }
	    }	    
	    
	    @Override
		protected String doInBackground(String... params) {
	    	res1 = downloadFile(getString(R.string.stopsURL), getString(R.string.stopsFileName));
	    	res2 = downloadFile(getString(R.string.routesURL), getString(R.string.routesFileName));
	    	res3 = downloadFile(getString(R.string.timesURL), getString(R.string.timesFileName));
	        return null;    
		}

	    private String downloadFile(String urlstr, String fileName) {
	        InputStream input = null;
	        OutputStream output = null;
	        HttpURLConnection connection = null;
	        try {
	            URL url = new URL(urlstr);
	            connection = (HttpURLConnection) url.openConnection();
	            connection.connect();
	            
	            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	                return "Ошибка: " + connection.getResponseCode() + " " + connection.getResponseMessage();
	            }

	            int fileLength = connection.getContentLength();

	            input = connection.getInputStream();
	            output = new FileOutputStream(secStore + File.separator + fileName);

	            byte data[] = new byte[4096];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	                total += count;
	                if (fileLength > 0) {
	                    publishProgress((int) (total * 100 / fileLength));
	                }
	                output.write(data, 0, count);
	            }
	        } 
	        catch (Exception e) {
	            return e.toString();
	        } 
	        finally {
	            try {
	                if (output != null) {
	                    output.close();
	                }
	                if (input != null) {
	                    input.close();
	                }
	            } 
	            catch (IOException ignored) {}

	            if (connection != null) {
	                connection.disconnect();
	            }
	        }
	        return null;
	    }	    
	}	

}
