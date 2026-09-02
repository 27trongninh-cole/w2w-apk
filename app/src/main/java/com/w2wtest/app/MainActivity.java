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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private Uri selectedWav;
    private TextView logView;
    private TextView txtQualityLabel;
    private SeekBar seekQuality;
    private static final int PICK_WAV = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = findViewById(R.id.txtLog);
        Button pick = findViewById(R.id.btnPick);
        Button convert = findViewById(R.id.btnConvert);
        Button copyLog = findViewById(R.id.btnCopyLog);
        txtQualityLabel = findViewById(R.id.txtQualityLabel);
        seekQuality = findViewById(R.id.seekQuality);

        updateQualityLabel(seekQuality.getProgress());
        seekQuality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                updateQualityLabel(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

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
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "audio/wav", "audio/x-wav", "audio/wave",
                    "audio/mpeg", "audio/mp3",
                    "audio/ogg", "application/ogg", "audio/x-ogg"
            });
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

    /** progress: 0..9 (SeekBar) -> muc hien thi 1..10 -> quality native Vorbis (-0.1 .. 1.0). */
    private float qualityFromSlider(int progress) {
        int level = progress + 1; // 1..10
        return -0.1f + (level - 1) / 9f * 1.1f;
    }

    private void updateQualityLabel(int progress) {
        int level = progress + 1;
        float q = qualityFromSlider(progress);
        txtQualityLabel.setText(String.format(java.util.Locale.US,
                "Mức nén Vorbis (Quality): %d/10 (quality=%.2f)", level, q));
    }

    /** Doan dinh dang file nguon dua tren MIME type roi ten file, mac dinh coi la wav. */
    private String detectSourceType(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime != null) {
            mime = mime.toLowerCase();
            if (mime.contains("mpeg") || mime.contains("mp3")) return "mp3";
            if (mime.contains("ogg")) return "ogg";
            if (mime.contains("wav")) return "wav";
        }
        String name = queryDisplayName(uri);
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".mp3")) return "mp3";
            if (lower.endsWith(".ogg")) return "ogg";
            if (lower.endsWith(".wav")) return "wav";
        }
        return "wav";
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void doConvert() {
        if (selectedWav == null) {
            Toast.makeText(this, "Chua chon file WAV", Toast.LENGTH_SHORT).show();
            return;
        }
        logView.setText("");
        float quality = qualityFromSlider(seekQuality.getProgress());
        new Thread(() -> {
            try {
                File cacheDir = getCacheDir();
                File wavFile = new File(cacheDir, "input.wav");

                String type = detectSourceType(selectedWav);
                log("Loai file nguon phat hien: " + type.toUpperCase());

                if ("wav".equals(type)) {
                    try (InputStream is = getContentResolver().openInputStream(selectedWav);
                         FileOutputStream os = new FileOutputStream(wavFile)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
                    }
                    log("Da copy WAV vao cache: " + wavFile.length() + " byte.");
                } else {
                    log("Dang giai ma " + type.toUpperCase() + " sang WAV trung gian truoc khi encode .wem...");
                    AudioDecoder.decodeToWav(this, selectedWav, wavFile, this::log);
                }

                log("Muc quality vorbis su dung: " + quality);

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
                File result = WemConverter.convert(wavFile, outDir, quality, codebookBytes, this::log);

                log("=== THANH CONG ===");
                log("File luu tai (thu muc rieng app): " + result.getAbsolutePath());

                String wemSavedPath = DownloadSaver.saveToDownloads(this, result, "application/octet-stream", this::log);
                if (wemSavedPath != null) log("Da luu .wem vao: " + wemSavedPath);

                File oggFile = new File(outDir, "out.ogg");
                if (oggFile.exists()) {
                    String oggSavedPath = DownloadSaver.saveToDownloads(this, oggFile, "audio/ogg", this::log);
                    if (oggSavedPath != null) log("Da luu .ogg vao: " + oggSavedPath);
                }
            } catch (Exception e) {
                log("LOI: " + e.getMessage());
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                log(sw.toString());
            }
        }).start();
    }
}
