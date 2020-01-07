package com.karandash_and_samodelkin.multimusic;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
    public void OnClick_Client (View view) { delete_start_button(); new Thread(new Client()).start(); Log.d("CREATION", "CLIENT START");}


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

    public String ChoosePlayFile() {
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

    public class Server implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            try {
                if(main_server) { filePath = ChoosePlayFile(); main_server = false; }
                Socket server = ServerSocketObject.accept();
                new Thread(new Server()).start();
                server.setSoTimeout(10000000);

                Log.d("CREATION", "NEW CLIENT");

                File file = new File(filePath);
                //Log.d("CREATION", "LOAD FILE - " + String.valueOf(file.length()));
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

                int i = 0; int len;
                byte[] buffer = new byte[1024];
                Integer fileSize = (int) file.length();
                while(i<fileSize){
                    len = file_input.read(buffer);
                    i+=len;
                    //Log.d("CREATION", new String(buffer, "UTF-8"));
                    file_send.write(buffer, 0, len);
                    file_send.flush();
                }

                while (true) { if(input.readLine() != null) { break; } }



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
                Socket client = new Socket(InetAddress.getByName("192.168.32.211"), 2048);

                Log.d("CREATION", "CONNECT SERVER");

                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
                BufferedInputStream file_in = new BufferedInputStream(client.getInputStream());
                File mp3 = new File(getExternalStorageDirectory().getAbsolutePath().toString() + "/2.mp3");
                if (!mp3.exists()) { mp3.createNewFile(); }
                BufferedOutputStream file_out = new BufferedOutputStream(new FileOutputStream(mp3));

                int fileSize = Integer.parseInt(input.readLine());
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
                while (true) { if(input.readLine() != null) { break; } }

                MediaPlayer mPlayer = new MediaPlayer();
                mPlayer.setDataSource(mp3.getAbsolutePath());
                mPlayer.prepare();
                mPlayer.start();

                SetProgressBar(-1);

            } catch (IOException e) { e.printStackTrace(); }
        }
    }



}
