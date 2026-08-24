package com.w2wtest.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private Uri selectedWav;
    private TextView logView;
    private static final int PICK_WAV = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.txtLog);
        Button pick = findViewById(R.id.btnPick);
        Button convert = findViewById(R.id.btnConvert);
        Button copyLog = findViewById(R.id.btnCopyLog);

        try {
            System.loadLibrary("mwem");
            log("Da nap libmwem.so thanh cong.");
        } catch (Throwable t) {
            log("LOI NAP THU VIEN NATIVE (libmwem.so): " + t.getMessage());
            log("=> Kiem tra file .so co dung trong jniLibs/arm64-v8a/ va thiet bi co ho tro arm64 khong.");
        }

        pick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            startActivityForResult(intent, PICK_WAV);
        });

        convert.setOnClickListener(v -> doConvert());

        copyLog.setOnClickListener(v -> {
            String text = logView.getText().toString();
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("W2W Log", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã copy log (" + text.length() + " ký tự)", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_WAV && resultCode == RESULT_OK && data != null) {
            selectedWav = data.getData();
            log("Da chon file: " + selectedWav);
        }
    }

    private void log(String msg) {
        runOnUiThread(() -> {
            logView.append(msg + "\n");
        });
    }

    private void doConvert() {
        if (selectedWav == null) {
            Toast.makeText(this, "Chua chon file WAV", Toast.LENGTH_SHORT).show();
            return;
        }
        logView.setText("");
        new Thread(() -> {
            try {
                File cacheDir = getCacheDir();
                File wavFile = new File(cacheDir, "input.wav");
                try (InputStream is = getContentResolver().openInputStream(selectedWav);
                     FileOutputStream os = new FileOutputStream(wavFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
                }
                log("Da copy WAV vao cache: " + wavFile.length() + " byte.");

                byte[] codebookBytes;
                try (InputStream is = getAssets().open("packed_codebooks_aoTuV_603.bin")) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
                    codebookBytes = baos.toByteArray();
                }
                log("Da nap codebook dictionary: " + codebookBytes.length + " byte.");

                File outDir = getExternalFilesDir(null);
                File result = WemConverter.convert(wavFile, outDir, 0.4f, codebookBytes, this::log);

                log("=== THANH CONG ===");
                log("File luu tai: " + result.getAbsolutePath());
            } catch (Exception e) {
                log("LOI: " + e.getMessage());
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                log(sw.toString());
            }
        }).start();
    }
}
