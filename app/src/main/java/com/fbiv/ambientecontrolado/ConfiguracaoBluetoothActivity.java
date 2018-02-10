package com.fbiv.ambientecontrolado;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection;

public class ConfiguracaoBluetoothActivity extends AppCompatActivity implements ThreadCompleteListener {

    private int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            if (msg.what == MyBluetoothService.MessageConstants.PAIRED) {

                BluetoothDevice device = (BluetoothDevice) msg.obj;

                Toast.makeText(ConfiguracaoBluetoothActivity.this, getString(R.string.pareadoA) + " " + device.getName(), Toast.LENGTH_SHORT).show();

                // TODO:

                bluetooth.disconnect();
                scan();

            }

            return false;
        }
    }); // handler that gets info from Bluetooth service

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }

    private BluetoothAdapter bluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices = Collections.emptySet();
    private List<BluetoothDevice> availableDevices = new ArrayList<>();
    private MyBluetoothService bluetooth;
    private RecyclerView recyclerView;

    private BroadcastReceiver bluetoothScanning = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                boolean paired = false;

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                for (BluetoothDevice pairedDevice : pairedDevices) {

                    if (pairedDevice.getAddress().equals(device.getAddress())) {

                        paired = true;
                        break;

                    }

                    paired = false;

                }

                if (!paired) {

                    availableDevices.add(device);
                    refreshRecyclerView();

                }

            }

        }
    };

    @Override
    public void onThreadComplete(Thread thread) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scan();
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracao_bluetooth);

        refreshRecyclerView();

        if (getSupportActionBar() != null) {

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("Configurações Bluetooth");

        }

        bluetooth = new MyBluetoothService(this, handler);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_ACCESS_COARSE_LOCATION);

        } else {

            registerReceiver(bluetoothScanning, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            scan();

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        onBackPressed();

        return true;
    }

    private void refreshRecyclerView() {

        // Create an instance of SectionedRecyclerViewAdapter
        SectionedRecyclerViewAdapter sectionAdapter = new SectionedRecyclerViewAdapter();

        // Add your Sections
        if (pairedDevices.size() > 0) {

            List<String> pairedDevicesName = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {

                pairedDevicesName.add(device.getName() != null ? device.getName() : device.getAddress());

            }

            sectionAdapter.addSection(new BluetoothSection(getString(R.string.dispositivosPareados), pairedDevicesName));

        }

        List<String> availableDevicesName = new ArrayList<>();
        for (BluetoothDevice device : availableDevices) {

            availableDevicesName.add(device.getName() != null ? device.getName() : device.getAddress());

        }

        sectionAdapter.addSection(new BluetoothSection(getString(R.string.dispositivosDisponiveis), availableDevicesName));

        // Set up your RecyclerView with the SectionedRecyclerViewAdapter
        recyclerView = (RecyclerView) findViewById(R.id.configuracaoBluetooth_recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_ACCESS_COARSE_LOCATION) {

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                registerReceiver(bluetoothScanning, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                scan();

            }

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


        } else {

            unregisterReceiver(bluetoothScanning);

        }

    }

    public void findDevices() {

        pairedDevices = bluetoothAdapter.getBondedDevices();

        refreshRecyclerView();

        bluetoothAdapter.cancelDiscovery();
        bluetoothAdapter.startDiscovery();

    }

    public void scan() {

        pairedDevices = Collections.emptySet();
        availableDevices = new ArrayList<>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

            Toast.makeText(this, "" + getString(R.string.bluetoothNaoSuportado), Toast.LENGTH_LONG).show();

        } else {

            if (!bluetoothAdapter.isEnabled()) {

                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, 1);

            } else {

                findDevices();

            }

        }

    }

    private class BluetoothSection extends StatelessSection {

        String title;
        List<String> bluetoothList;

        private BluetoothSection(String title, List<String> bluetoothList) {
            // call constructor with layout resources for this Section header and items
            super(R.layout.bluetooth_section_header, R.layout.item_bluetooth);

            this.title = title;

            if (bluetoothList.size() == 0) {

                bluetoothList.add(getString(R.string.nenhumDispositivo));

            }

            this.bluetoothList = bluetoothList;

        }

        @Override
        public int getContentItemsTotal() {
            return bluetoothList.size(); // number of items of this section
        }

        @Override
        public RecyclerView.ViewHolder getItemViewHolder(View view) {
            // return a custom instance of ViewHolder for the items of this section
            return new ItemViewHolder(view);
        }

        @Override
        public void onBindItemViewHolder(RecyclerView.ViewHolder holder, final int position) {
            final ItemViewHolder itemHolder = (ItemViewHolder) holder;

            final String deviceName = bluetoothList.get(position);
            if (deviceName.equals(getString(R.string.nenhumDispositivo))) {

                itemHolder.imageView.setVisibility(View.INVISIBLE);
                itemHolder.itemView.getRootView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scan();
                    }
                });

            } else {

                itemHolder.imageView.setVisibility(View.VISIBLE);
                itemHolder.imageView.setColorFilter(new PorterDuffColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP));
                itemHolder.itemView.getRootView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (title.equals(getString(R.string.dispositivosPareados))) {

                            final CharSequence[] items = {getString(R.string.desemparelhar)};

                            AlertDialog.Builder builder = new AlertDialog.Builder(ConfiguracaoBluetoothActivity.this);

                            builder.setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int item) {

                                    for (Iterator<BluetoothDevice> it = pairedDevices.iterator(); it.hasNext(); ) {
                                        BluetoothDevice device = it.next();
                                        if (device.getName().equals(deviceName)) {

                                            try {
                                                Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                                m.invoke(device, (Object[]) null);
                                                Timer timer = new Timer();
                                                timer.schedule(new TimerTask() {
                                                    @Override
                                                    public void run() {
                                                        runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {
                                                                pairedDevices = bluetoothAdapter.getBondedDevices();
                                                                refreshRecyclerView();
                                                            }
                                                        });

                                                    }
                                                }, 1000);

                                            } catch (Exception e) {
                                                Log.e("tag", e.getMessage());
                                            }

                                        }
                                    }


                                }
                            });
                            builder.show();

                        } else {

                            BluetoothDevice device = availableDevices.get(position);
                            Toast.makeText(ConfiguracaoBluetoothActivity.this, "" + getString(R.string.enviarPedidoPareamento), Toast.LENGTH_LONG).show();
                            bluetoothAdapter.cancelDiscovery();
                            bluetooth.pair(device);

                        }
                    }
                });

            }
            itemHolder.textView.setText(deviceName);
        }

        @Override
        public RecyclerView.ViewHolder getHeaderViewHolder(View view) {
            return new HeaderViewHolder(view);
        }

        @Override
        public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder) {

            HeaderViewHolder headerHolder = (HeaderViewHolder) holder;

            headerHolder.textView.setText(title);

        }
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        private HeaderViewHolder(View view) {
            super(view);

            textView = (TextView) view.findViewById(R.id.bluetoothSectionHeader_textView);
        }
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView textView;

        private ItemViewHolder(View itemView) {
            super(itemView);

            imageView = (ImageView) itemView.findViewById(R.id.itemBluetooth_imageView);
            textView = (TextView) itemView.findViewById(R.id.itemBluetooth_textView);

        }
    }

}
