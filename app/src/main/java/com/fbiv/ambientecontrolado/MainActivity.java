package com.fbiv.ambientecontrolado;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

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

public class MainActivity extends AppCompatActivity {

    private TextView temperaturaReferencia;
    private TextView temperaturaAtual;
    private LineChart graficoTemperatura;
    private ArrayList<Integer> temperaturaArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperaturaReferencia = findViewById(R.id.temperaturaReferenciaTextView);
        temperaturaAtual = findViewById(R.id.temperaturaAtualTextView);

        graficoTemperatura = findViewById(R.id.graficoTemperatura);
        graficoTemperatura.animateY(2000);
        graficoTemperatura.setNoDataText(getString(R.string.conecteAoDispositivo));
        graficoTemperatura.getDescription().setText("");
        graficoTemperatura.setTouchEnabled(false);
        graficoTemperatura.getXAxis().setEnabled(false);
        graficoTemperatura.getAxisRight().setTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));
        graficoTemperatura.getAxisLeft().setAxisMinimum(0);
        graficoTemperatura.getAxisLeft().setDrawGridLines(false);

        temperaturaArray = new ArrayList<>();

        inserirDadosDeTeste();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (temperaturaArray.size() > 0) {

            List<Entry> dados = new ArrayList<>();

            for (int i = 0; i < temperaturaArray.size(); i++) {

                dados.add(new Entry(i, temperaturaArray.get(i)));

            }

            LineDataSet dataSet = new LineDataSet(dados, getString(R.string.temperatura));

            dataSet.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimary));
            dataSet.setValueTextColor(ContextCompat.getColor(getApplicationContext(), android.R.color.transparent));

            LineData lineData = new LineData(dataSet);

            graficoTemperatura.getAxisLeft().setAxisMaximum(50);
            graficoTemperatura.setData(lineData);
            graficoTemperatura.invalidate();

        }

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

}
