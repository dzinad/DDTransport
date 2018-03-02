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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private ProgressDialog mProgressDialog;
	private TextView hintsInfoView, hintsTextView;
	private String secStore;
	private File fileStops, fileRoutes, fileTimes;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		
        hintsTextView = (TextView) findViewById(R.id.hints);
		hintsInfoView = (TextView) findViewById(R.id.info);
		secStore = Environment.getExternalStorageDirectory().toString() + File.separator + getString(R.string.directory);
		
		mProgressDialog = new ProgressDialog(MainActivity.this);
		mProgressDialog.setMessage(getString(R.string.loading));
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setCancelable(false);
	}
		
	public void showHints(View v) {
		String hintsstr = "1. Нажмите на кнопку \"Карта\".\n2. Остановки обозначены маркерами (синие - автобус/троллейбус, зеленые - трамвай, красные - метро).\n3. Нажмите на маркер, чтобы увидеть ожидамое время прибытия транспорта на выбранную остановку.\n4. Чтобы просмотреть полное расписание маршрута на этой остановке, нажмите на соответствующую строчку в появившемся окошке.\n5. Вся информация взята с официального сайта предприятия \"Минсктранс\".\n6. Нажмите на кнопку \"Обновить\", если хотите получить самые свежие данные с сайта minsktrans.by.\n";

		File file=new File(secStore, getString(R.string.timesFileName));
		Date date=new Date();
        if(file.exists()) {
            Long lastModified = file.lastModified();
            date = new Date(lastModified);
            SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");            
    		hintsTextView.setText(hintsstr+"7. Дата последнего обновления данных: "+formatter.format(date)+"\n");
        }
        else {
        	hintsTextView.setText(hintsstr);
        }
        
		hintsInfoView.setText("Спрятать подсказки");
	    hintsInfoView.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            hideHints(v);
	        }
	    });	
	}
	
	public void hideHints(View v) {
		hintsTextView.setText("");
		hintsInfoView.setText("Показать подсказки");
	    hintsInfoView.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            showHints(v);
	        }
	    });	
	}
	
	public void getMapOnClick(View v) {
		getMap();		
	}
	
	private void getMap() {		
		fileStops = new File(secStore, getString(R.string.stopsFileName));
		fileRoutes = new File(secStore, getString(R.string.routesFileName));
		fileTimes = new File(secStore, getString(R.string.timesFileName));

		if (fileStops.exists() && fileRoutes.exists() && fileTimes.exists()) {	
			Intent intent = new Intent(MainActivity.this, MapActivity.class);
			intent.putExtra("secStore", secStore);
			startActivity(intent);				
		}
		else {
			download(true);
		}	
		
	}
	
	public void update(View v) {
		download(false);
	}
	
	private void download(boolean ifSetMap) {
		File folder = new File(secStore);
		boolean success = true;
		if (folder.exists()) {
			startDownload(ifSetMap);
		}
		else {
			success = folder.mkdir();
		}
		if (success) {
			startDownload(ifSetMap);
		} 
		else {
	        Toast.makeText(this.getApplicationContext(), "Ошибка: невозможно создать папку.", 
	        		   Toast.LENGTH_SHORT).show();
		}						
	}
	
	private void startDownload(boolean ifSetMap) {
		final DownloadTask downloadTask = new DownloadTask(MainActivity.this, ifSetMap);
		downloadTask.execute();
	}
	
	private class DownloadTask extends AsyncTask<String, Integer, String> {
		private Context context;
		private String res1 = "", res2 = "", res3 = "";
	    private PowerManager.WakeLock mWakeLock;
	    private boolean ifSetMap;

	    public DownloadTask(Context context, boolean ifSetMap) {
			this.context = context;
			this.ifSetMap = ifSetMap;
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
	        if (ifSetMap) {
	        	getMap();
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
