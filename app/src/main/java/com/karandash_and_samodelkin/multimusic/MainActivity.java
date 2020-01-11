package com.karandash_and_samodelkin.multimusic;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity<pablic> extends AppCompatActivity {
    ServerSocket ServerSocketObject;
    public ProgressBar progressBar;

    Uri chosenAudioUri;

    boolean main_server = true;
    String filePath = "";
    public int width = 0;
    public int height = 0;

    Boolean play = false;

    String name = "";
    String ip = "";

    Boolean client_input_system = false;

    Boolean all_ok_playing = false;

    int[] clients_progress_download = new int[10];
    String[] clients_ip = new String[10];
    int clients_connect = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController); // оно отвечает за кнопки интерфейса внизу - НЕ ТРОГАТЬ!

        Log.d("CREATION", "INIT PROGRAMM");
        for(int i = 0; i <clients_progress_download.length; i++) { clients_progress_download[i] = 0; }

        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { requestPermissions(permissions, 1); }

        try { ServerSocketObject = new ServerSocket(2048, 10); } catch (IOException e) { e.printStackTrace(); } // Init объекта сокета

        Display display = getWindowManager().getDefaultDisplay(); Point size = new Point(); display.getSize(size);
        width = size.x; height = size.y;

    }

    public void delete_start_button() {
        Button server_button = findViewById(R.id.Server); ((ViewGroup) server_button.getParent()).removeView(server_button);
        Button client_button = findViewById(R.id.Client); ((ViewGroup) client_button.getParent()).removeView(client_button);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void OnClick_Server (View view) { delete_start_button(); new Thread(new Server()).start(); Log.d("CREATION", "SERVER START"); }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void OnClick_Client (View view) { delete_start_button(); new Thread(new Client()).start(); Log.d("CREATION", "CLIENT START"); }

    public void OnClick_Start(View view) { play = true; }
    public void OnClick_Connect(View view) {
        Button button_conn = (Button) findViewById(R.id.Connect);
        final EditText ip_input = (EditText) findViewById(R.id.IP);
        final EditText name_input = (EditText) findViewById(R.id.NAME);
        ip = ip_input.getText().toString();
        name = name_input.getText().toString();
        client_input_system = true; }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void SetProgressBar(final int progress) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView textView = findViewById(R.id.loadingTextView);
                ProgressBar progressBar = findViewById(R.id.loadingProgressBar);
                if(progress == -1) { textView.setText(""); progressBar.setVisibility(ProgressBar.INVISIBLE); return; }

                textView.setText("Загрузка "+String.valueOf(progress)+"%");
                progressBar.setVisibility(ProgressBar.VISIBLE);
                progressBar.setProgress(progress);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case 1: {
                if (resultCode == RESULT_OK) { chosenAudioUri = data.getData(); }
                break;
            }
        }
    }
    public void show_TOAST(final String in) {
        MainActivity.this.runOnUiThread(new Runnable() { public void run() { (Toast.makeText(getApplicationContext(), in, Toast.LENGTH_SHORT)).show(); } });
    }

    public String ChoosePlayFile() {
        show_TOAST("Выберите файл для воспроизведения");

        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        photoPickerIntent.setDataAndType(Uri.parse(Environment.getExternalStorageDirectory().getPath()), "audio/*");
        startActivityForResult(Intent.createChooser(photoPickerIntent, "Open folder"), 1);

        while(chosenAudioUri == null) { }
        Cursor cursor = getContentResolver().query(chosenAudioUri, new String[] {android.provider.MediaStore.Audio.AudioColumns.DATA }, null, null, null);
        cursor.moveToFirst();
        final String filePath = cursor.getString(0);

        Log.d("CREATION", "file path - " + filePath);

        return filePath;
    }

    public void show_ip_server(final int status) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                TextView textView = findViewById(R.id.ShowIpServer);
                if(status == 1) { textView.setText("Сервер ожидает подключения\nВаш IP Адресс - "+ip); }
                if(status == 0) { textView.setText(""); }
            }
        });
    }

    public void StartButtonVisible(final int status) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Button button = findViewById(R.id.Start);
                if(status == 1) { button.setVisibility(ProgressBar.VISIBLE); }
                if(status == 0) { button.setVisibility(ProgressBar.INVISIBLE); }
            }
        });
    }

    public void show_edit_text(final int status) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                EditText NAME = findViewById(R.id.NAME);
                EditText IP = findViewById(R.id.IP);
                Button button = findViewById(R.id.Connect);
                if(status == 1) { NAME.setVisibility(View.VISIBLE); IP.setVisibility(View.VISIBLE); button.setVisibility(View.VISIBLE); }
                if(status == 0) { NAME.setVisibility(View.INVISIBLE); IP.setVisibility(View.INVISIBLE); button.setVisibility(View.INVISIBLE); }
            }
        });
    }

    public void show_clients(final int status) {
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                ListView lw = findViewById(R.id.USERS);
                if(status == 1) { lw.setVisibility(View.VISIBLE); }
                if(status == 0) { lw.setVisibility(View.INVISIBLE); }
                String[] clients_ip_ok = new String[clients_connect];
                int ok = 0;
                for(int i = 0; i<clients_ip.length; i++) { if(clients_ip[i]!= null && clients_ip[i] != "") {clients_ip_ok[ok] = clients_ip[i]; ok++;} }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, clients_ip_ok);
                lw.setAdapter(adapter);
            }
        });
    }

    public class Server implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            try {
                if(main_server) { filePath = ChoosePlayFile(); main_server = false; }

                show_ip_server(1);

                Socket server = ServerSocketObject.accept();

                int my_client = clients_connect;
                clients_connect+=1;
                Log.d("CREATION", "NEW CLIENT");
                new Thread(new Server()).start();

                server.setSoTimeout(10000000);
                Log.d("CREATION", "NEW CLIENT init");

                File file = new File(filePath);
                BufferedReader input = new BufferedReader(new InputStreamReader(server.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(server.getOutputStream())), true);
                BufferedOutputStream file_send = new BufferedOutputStream(server.getOutputStream());
                BufferedInputStream file_input = new BufferedInputStream(new FileInputStream(file));

                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(file.getAbsolutePath());
                String artist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Log.d("CREATION", title + ":" + artist + ":" + duration);


                out.println(String.valueOf(file.length()));
                clients_ip[my_client] = input.readLine();
                show_clients(1);

                int i = 0; int len;
                byte[] buffer = new byte[1024];
                Integer fileSize = (int) file.length();
                while(i<fileSize){
                    len = file_input.read(buffer);
                    i+=len;
                    file_send.write(buffer, 0, len);
                    file_send.flush();
                    Log.d("CREATION", String.valueOf(((float)i/(float)fileSize)*100));
                }

                while (true) { if(input.readLine() != null) { break; } }
                StartButtonVisible(1);
                while (play == false) {}
                show_ip_server(0);
                show_clients(0);
                StartButtonVisible(0);

                out.println("ok");

                MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(file.getAbsolutePath()));
                mPlayer.start();

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public class Client implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            try {
                show_edit_text(1);
                while(client_input_system == false) {}
                Log.d("CREATION", "CONNECT SERVER - "+ip);
                Socket client = new Socket(InetAddress.getByName(ip), 2048);
                show_edit_text(0);
                Log.d("CREATION", "CONNECT SERVER");

                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
                BufferedInputStream file_in = new BufferedInputStream(client.getInputStream());
                File mp3 = new File(getExternalStorageDirectory().getAbsolutePath().toString() + "/2.mp3");
                if (!mp3.exists()) { mp3.createNewFile(); }
                BufferedOutputStream file_out = new BufferedOutputStream(new FileOutputStream(mp3));
                Log.d("CREATION", "Ok INIT client");

                int fileSize = Integer.parseInt(input.readLine());

                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String my_ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                out.println(name+"("+my_ip+")");
                byte[] buffer = new byte[1024];

                int i = 0; int len = 0;
                while(i < fileSize) {
                    len = file_in.read(buffer, 0, (fileSize - i < buffer.length) ? fileSize - i : buffer.length);
                    i += len;
                    file_out.write(buffer, 0, len);
                    file_out.flush();
                    Log.d("CREATION", String.valueOf(((float)i/(float)fileSize)*100));
                    SetProgressBar((int) (((float)i/(float)fileSize)*100));
                }
                Log.d("CREATION", "Ok RECV");
                out.println("ok");

                SetProgressBar(-1);

                while (true) { if(input.readLine() != null) { break; } }

                MediaPlayer mPlayer = new MediaPlayer();
                mPlayer.setDataSource(mp3.getAbsolutePath());
                mPlayer.prepare();
                mPlayer.start();
            } catch (IOException e) { show_TOAST("Ошибка подключения"); e.printStackTrace(); }
        }
    }



}
