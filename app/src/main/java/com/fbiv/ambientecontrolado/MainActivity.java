package com.fbiv.ambientecontrolado;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private TextView temperaturaReferencia;
    private TextView temperaturaAtual;
    private LineChart graficoTemperatura;
    private ArrayList<Integer> temperaturaArray;
    private MyBluetoothService bluetooth;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> dispositivosPareados;
    private Timer timer;
    private List<BluetoothDevice> availableDevices = new ArrayList<>();
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {

                case MyBluetoothService.MessageConstants.TOAST:
                    String toast = (String) msg.obj;
                    Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
                    break;
                case MyBluetoothService.MessageConstants.NEW_DATA:

                    temperaturaReferencia.setText(msg.arg1+"º");
                    temperaturaAtual.setText(msg.arg2+"º");

                    temperaturaArray.add(msg.arg2);
                    recarregarGrafico();
                    Toast.makeText(MainActivity.this, "Dados recebidos!", Toast.LENGTH_SHORT).show();

                    break;
                default:
                    break;

            }

            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperaturaReferencia = findViewById(R.id.temperaturaReferenciaTextView);
        temperaturaAtual = findViewById(R.id.temperaturaAtualTextView);

        graficoTemperatura = findViewById(R.id.graficoTemperatura);
        graficoTemperatura.animateY(2000);
        graficoTemperatura.setNoDataText("");
        graficoTemperatura.getDescription().setText("");
        graficoTemperatura.setTouchEnabled(false);
        graficoTemperatura.getXAxis().setEnabled(false);
        graficoTemperatura.getAxisRight().setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));
        graficoTemperatura.getAxisLeft().setAxisMinimum(0);
        graficoTemperatura.getAxisLeft().setDrawGridLines(false);

        temperaturaArray = new ArrayList<>();

    }

    @Override
    protected void onResume() {
        super.onResume();

        recarregarGrafico();

        if (bluetoothAdapter != null) {

            dispositivosPareados = bluetoothAdapter.getBondedDevices();
            bluetooth = null;
            bluetooth = new MyBluetoothService(this, handler);

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            establishConnectionWithBoard();
                        }
                    });

                }
            }, 0, 5000);

        }

    }

    @Override
    protected void onPause() {
        super.onPause();

        timer.cancel();
        timer = null;
        bluetooth.disconnect();

    }

    private void recarregarGrafico() {

        if (temperaturaArray.size() > 0) {

            List<Entry> dados = new ArrayList<>();

            for (int i = 0; i < temperaturaArray.size(); i++) {

                dados.add(new Entry(i, temperaturaArray.get(i)));

            }

            LineDataSet dataSet = new LineDataSet(dados, getString(R.string.temperatura));

            dataSet.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            dataSet.setCircleColor(ContextCompat.getColor(getApplicationContext(), android.R.color.black));
            dataSet.setValueTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));

            LineData lineData = new LineData(dataSet);

            graficoTemperatura.getAxisLeft().setAxisMaximum(50);
            graficoTemperatura.setData(lineData);
            graficoTemperatura.invalidate();

        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        Intent intent = new Intent(MainActivity.this, ConfiguracaoBluetoothActivity.class);
        startActivity(intent);

        return true;
    }

    public void inserirDadosDeTeste() {

        for (int i = 0; i < 20; i++) {
            if (i< 10) {
                temperaturaArray.add(24);
            } else {
                temperaturaArray.add(25);
            }

        }

    }

    private void establishConnectionWithBoard() {

        if (dispositivosPareados.size() > 0) {

            BluetoothDevice device = (BluetoothDevice) dispositivosPareados.toArray()[0];
            bluetooth.listenToNewData(device);

        } else {

            Toast.makeText(this, "Nenhum dispositivo pareado", Toast.LENGTH_LONG).show();
            timer.cancel();
            timer = null;

        }

    }

}
