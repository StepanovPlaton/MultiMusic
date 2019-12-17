package com.karandash_and_samodelkin.multimusic;

import android.Manifest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.material.bottomnavigation.BottomNavigationView;

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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends AppCompatActivity {

    ServerSocket ServerSocketObject;

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
    }

    public void delete_start_button() {
        Button server_button = findViewById(R.id.Server); ((ViewGroup) server_button.getParent()).removeView(server_button);
        Button client_button = findViewById(R.id.Client); ((ViewGroup) client_button.getParent()).removeView(client_button);
    }

    public void OnClick_Server (View view) { delete_start_button(); new Thread(new Server()).start(); Log.d("CREATION", "SERVER START"); }
    public void OnClick_Client (View view) { delete_start_button(); new Thread(new Client()).start(); Log.d("CREATION", "CLIENT START");}


    public class Server implements Runnable {
        @Override
        public void run() {
            try {
                Socket server = ServerSocketObject.accept();
                new Thread(new Server()).start();
                server.setSoTimeout(10000);

                Log.d("CREATION", "NEW CLIENT");

                File file = new File("/storage/emulated/0/1.mp3");
                Log.d("CREATION", "LOAD FILE - " + String.valueOf(file.length()));

                BufferedReader input = new BufferedReader(new InputStreamReader(server.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(server.getOutputStream())), true);
                BufferedOutputStream file_send = new BufferedOutputStream(server.getOutputStream());
                BufferedInputStream file_input = new BufferedInputStream(new FileInputStream(file));

                out.println(file.length());

                byte [] buffer = new byte[(int)file.length()];

                file_input.read(buffer, 0, buffer.length); Log.d("CREATION", "READ FILE");
                Log.d("CREATION", new String(buffer, "UTF-8"));
                //SystemClock.sleep(1000);
                file_send.write(buffer, 0, buffer.length); Log.d("CREATION", "SEND FILE");
                //SystemClock.sleep(1000);
                file_send.flush();

                MediaPlayer mPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(file.getAbsolutePath()));
                mPlayer.start();

            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    public class Client implements Runnable {

        @Override
        public void run() {
            try {
                Socket client = new Socket(InetAddress.getByName("192.168.32.211"), 2048);

                Log.d("CREATION", "CONNECT SERVER");

                BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())), true);
                BufferedInputStream file_in = new BufferedInputStream(client.getInputStream());
                Log.d("CREATION", "INIT OK");
                int len_file = Integer.parseInt(input.readLine());
                Log.d("CREATION", "LEN OK");

                final byte[] buffer = new byte[len_file];

                file_in.read(buffer, 0, len_file);
                Log.d("CREATION", "TOOK FILE");

                Log.d("CREATION", new String(buffer, "UTF-8"));
                File mp3 = new File(getExternalStorageDirectory().getAbsolutePath().toString() + "/2.mp3");
                mp3.deleteOnExit();
                if (!mp3.exists()) { mp3.createNewFile(); }
                BufferedOutputStream file_out = new BufferedOutputStream(new FileOutputStream(mp3));
                file_out.write(buffer);
                file_out.flush(); file_out.close();

                MediaPlayer mPlayer = new MediaPlayer();
                mPlayer.setDataSource(mp3.getAbsolutePath());
                mPlayer.prepare();
                //SystemClock.sleep(3000);
                //mPlayer.setLooping(true);
                mPlayer.start();

            } catch (IOException e) { e.printStackTrace(); }
        }
    }



}
