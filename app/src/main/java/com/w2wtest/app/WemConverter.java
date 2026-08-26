package com.w2wtest.app;

import a6.d;
import androidx.lifecycle.z;
import com.google.android.gms.internal.ads.vk;
import io.github.lnii11.bsed.w2w.W2WIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

public class WemConverter {

    public interface Logger { void log(String msg); }

    public static File convert(File wavFile, File outDir, float quality, byte[] codebookBinBytes, Logger log) throws Exception {
        WavInfo wav = WavInfo.read(wavFile.getAbsolutePath());
        log.log("WAV: " + wav.channels + " kenh, " + wav.sampleRate + " Hz, " + wav.numSamples + " sample.");

        File oggFile = new File(outDir, "out.ogg");
        int wencResult = W2WIO.wenc(wavFile.getAbsolutePath(), oggFile.getAbsolutePath(), wav.channels, wav.sampleRate, quality);
        log.log("wenc() ket qua: " + wencResult);
        if (wencResult != 1) throw new RuntimeException("Native wenc() that bai");
        log.log("Encode xong: " + oggFile.length() + " byte.");

        i5.b bReader = new i5.b(new RandomAccessFile(oggFile, "r"));
        Iterator it = bReader.f11781c.values().iterator();
        if (!it.hasNext()) throw new RuntimeException("Khong tim thay stream nao trong file ogg");
        i5.c cVar = (i5.c) it.next();

        z dictionary = new z(codebookBinBytes);
        vk vkVar = new vk(cVar, dictionary);
        log.log("Da parse ID header + match codebook.");

        a6.a idHeader = (a6.a) vkVar.f8888c;
        d setupData = (d) vkVar.f8889d;

        File finalWemFile = new File(outDir, wavFile.getName().replaceAll("\\.[^.]+$", "") + ".wem");
        File audioTempFile = new File(outDir, "audio_temp.bin");
        FileOutputStream fileOutputStream = new FileOutputStream(audioTempFile);

        // ---- Ghi codebook index (count-1 8bit + moi codebook 10bit index) ----
        v5.a aVarD = v5.a.d();
        aVarD.j(8, setupData.f154c.size() - 1);
        int i19 = 0;
        for (Object entryObj : setupData.f154c.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObj;
            int key = (Integer) entry.getKey();
            aVarD.j(10, ((a6.c) ((ArrayList) entry.getValue()).get(0)).f152a);
            if (i19 != key) throw new IllegalArgumentException("Codebook thu tu sai: " + i19 + " != " + key);
            i19++;
        }

        // ---- Thoi gian domain (chuan spec: 6bit count+1, moi cai phai =0) ----
        v5.a src = (v5.a) setupData.f10944a;
        int timeCount = src.f(6) + 1;
        for (int t = 0; t < timeCount; t++) {
            if (src.f(16) != 0) throw new UnsupportedOperationException("Time domain transformation != 0");
        }
        log.log("Time domain OK (count=" + timeCount + ").");

        // ---- Floor ----
        int floorCount = aVarD.i(6, 6, src) + 1;
        for (int i = 0; i < floorCount; i++) {
            int floorType = src.f(16);
            if (floorType != 1) throw new UnsupportedOperationException("Floor type " + floorType + " khong ho tro");
            int partitions = aVarD.i(5, 5, src);
            int[] partClass = new int[partitions];
            int maxClass = -1;
            for (int j = 0; j < partitions; j++) {
                int c = aVarD.i(4, 4, src);
                partClass[j] = c;
                if (c > maxClass) maxClass = c;
            }
            int[] classDims = new int[maxClass + 1];
            for (int c = 0; c <= maxClass; c++) {
                classDims[c] = aVarD.i(3, 3, src) + 1;
                int subclasses = aVarD.i(2, 2, src);
                if (subclasses != 0) aVarD.i(8, 8, src);
                for (int k = 0; k < (1 << subclasses); k++) aVarD.i(8, 8, src);
            }
            aVarD.i(2, 2, src);
            int rangebits = aVarD.i(4, 4, src);
            for (int j = 0; j < partitions; j++) {
                int cn = partClass[j];
                for (int k = 0; k < classDims[cn]; k++) aVarD.i(rangebits, rangebits, src);
            }
        }

        log.log("Floor OK (count=" + floorCount + ").");

        // ---- Residue ----
        int residueCount = aVarD.i(6, 6, src) + 1;
        for (int i = 0; i < residueCount; i++) {
            int rtype = src.f(16);
            aVarD.j(2, rtype); // Wwise setup blob luu residue_type o 2 bit, KHONG PHAI 16 bit
                                // nhu trong file .ogg goc (chuan Vorbis day du). Ghi sai do rong
                                // o day lam lech toan bo bit phia sau (residue/mapping/mode).
            aVarD.i(24, 24, src);
            aVarD.i(24, 24, src);
            aVarD.i(24, 24, src);
            int classifications = aVarD.i(6, 6, src) + 1;
            aVarD.i(8, 8, src);
            int[] cascade = new int[classifications];
            for (int j = 0; j < classifications; j++) {
                int low = aVarD.i(3, 3, src);
                boolean flag = src.g();
                aVarD.j(1, flag ? 1 : 0);
                int high = 0;
                if (flag) high = aVarD.i(5, 5, src);
                cascade[j] = high * 8 + low;
            }
            for (int j = 0; j < classifications; j++)
                for (int k = 0; k < 8; k++)
                    if ((cascade[j] & (1 << k)) != 0) aVarD.i(8, 8, src);
        }

        log.log("Residue OK (count=" + residueCount + ").");

        // ---- Mapping ----
        int mapCount = aVarD.i(6, 6, src) + 1;
        for (int i = 0; i < mapCount; i++) {
            int mappingType = src.f(16);
            if (mappingType != 0) throw new UnsupportedOperationException("Mapping type != 0: " + mappingType);
            // KHONG ghi mapping_type vao setup blob: dinh dang rut gon cua Wwise luoc bo han
            // field nay (chi co 1 gia tri hop le = 0 nen khong can luu, decoder tu dien lai).
            // Ghi du 16 bit o day lam lech toan bo bit con lai cua moi mapping entry.
            boolean submapsFlag = src.g();
            aVarD.j(1, submapsFlag ? 1 : 0);
            int submaps = 1;
            if (submapsFlag) submaps = aVarD.i(4, 4, src) + 1;
            boolean squareFlag = src.g();
            aVarD.j(1, squareFlag ? 1 : 0);
            if (squareFlag) {
                int steps = aVarD.i(8, 8, src) + 1;
                int ib = ilog(idHeader.f144c - 1);
                for (int j = 0; j < steps; j++) { aVarD.i(ib, ib, src); aVarD.i(ib, ib, src); }
            }
            aVarD.i(2, 2, src);
            if (submaps > 1) for (int j = 0; j < idHeader.f144c; j++) aVarD.i(4, 4, src);
            for (int j = 0; j < submaps; j++) { aVarD.i(8, 8, src); aVarD.i(8, 8, src); aVarD.i(8, 8, src); }
        }

        log.log("Mapping OK (count=" + mapCount + ").");

        // ---- Mode ----
        int modeCount = aVarD.i(6, 6, src) + 1;
        setupData.f157f = new boolean[modeCount];
        for (int i = 0; i < modeCount; i++) {
            boolean blockflag = src.g();
            aVarD.j(1, blockflag ? 1 : 0);
            int windowtype = src.f(16);
            int transformtype = src.f(16);
            aVarD.i(8, 8, src);
            if (windowtype != 0) throw new UnsupportedOperationException("Window type != 0");
            if (transformtype != 0) throw new UnsupportedOperationException("Transform type != 0");
            setupData.f157f[i] = blockflag;
        }
        int modeBits = 0;
        { int mm = modeCount - 1; while (mm > 0) { mm >>= 1; modeBits++; } }
        setupData.f158g = modeBits;
        aVarD.j(1, 1); // framing bit

        byte[] setupBlob = aVarD.c();
        log.log("Setup blob: " + setupBlob.length + " byte.");

        // ---- Audio packet: dung dung logic that (bo bit packet-type + mode-selector, KHONG copy nguyen) ----
        c6.a packetReader = new c6.a(cVar);
        v5.a packetOut = v5.a.d();
        v5.a tmpD = v5.a.d();
        while (!packetReader.f1917e) {
            byte[] assembled = null;
            while (true) {
                if (packetReader.f1916d >= packetReader.f1918f) {
                    if (packetReader.f1915c != null) { packetReader.a(); packetReader.f1916d = 0; }
                    else { packetReader.f1917e = true; }
                }
                if (packetReader.f1917e) break;
                int[] sizes = packetReader.f1919g;
                int idx = packetReader.f1916d;
                int segSize = sizes[idx];
                int segOff = packetReader.f1920h[idx];
                byte[] chunk = Arrays.copyOfRange(packetReader.f1921i, segOff, segOff + segSize);
                assembled = (assembled == null) ? chunk : concat(assembled, chunk);
                packetReader.f1916d++;
                if (segSize < 255) break;
            }
            if (assembled != null && assembled.length != 0) {
                // THU NGHIEM: TAT han buoc cat bit packet-type + mode-selector (mod_packets).
                // Gia thuyet: dinh dang that cua SBank co the KHONG dung mod_packets - tuc la
                // audio packet duoc giu NGUYEN XI tu file .ogg chuan (van con du bit type +
                // window flag), chi can them khung 2-byte length. Neu day la nguyen nhan gay
                // cam lang, ban build nay se phat am duoc; neu van cam lang, se revert lai
                // buoc cat bit va tim huong khac.
                byte[] packetCompact = assembled;
                if (packetCompact.length > setupData.f160i) setupData.f160i = packetCompact.length;
                packetOut.j(16, packetCompact.length);
                packetOut.l(packetCompact);
                setupData.f159h += packetOut.b();
                fileOutputStream.write(packetOut.c());
                packetOut.a();
            }
        }
        fileOutputStream.close();
        log.log("Audio packet stream: " + audioTempFile.length() + " byte, max packet: " + setupData.f160i);

        // ---- Build header ----
        // QUAN TRỌNG: so sánh byte-by-byte với file .wem thật (SBank) cho thấy gói setup bên
        // trong chunk "data" PHẢI có prefix 2-byte little-endian ghi độ dài, giống hệt cách
        // từng audio packet đã có (packetOut.j(16, packetCompact.length)). Code cũ ghi thẳng
        // setupBlob vào đầu "data" mà KHÔNG có prefix này -> parser đọc nhầm 2 byte đầu của
        // setup blob thành "độ dài gói" (ra một số vô nghĩa) -> lệch toàn bộ bitstream từ đó.
        byte[] setupLenPrefix = new byte[]{
            (byte) (setupBlob.length & 0xFF),
            (byte) ((setupBlob.length >> 8) & 0xFF)
        };
        int setupPacketTotalLen = setupLenPrefix.length + setupBlob.length; // 2 + setupBlob.length

        // f149h = setup_packet_offset, f150i = first_audio_packet_offset (tính từ đầu chunk "data").
        // Setup packet (kèm prefix) luôn nằm đầu "data" -> offset 0; audio bắt đầu ngay sau đó.
        idHeader.f149h = 0;
        idHeader.f150i = setupPacketTotalLen;
        byte[] header = idHeader.o(setupBlob.length,
                (long) (audioTempFile.length() + setupPacketTotalLen));

        FileOutputStream out = new FileOutputStream(finalWemFile);
        out.write(header);
        out.write(setupLenPrefix);
        out.write(setupBlob);
        java.io.FileInputStream fis = new java.io.FileInputStream(audioTempFile);
        byte[] buf = new byte[8192];
        int n;
        while ((n = fis.read(buf)) != -1) out.write(buf, 0, n);
        fis.close();
        out.close();
        audioTempFile.delete();
        // KHONG xoa oggFile luc nay de con test truc tiep file .ogg trung gian (debug am thanh cam).
        // oggFile.delete();

        log.log("XONG! File .wem: " + finalWemFile.length() + " byte -> " + finalWemFile.getAbsolutePath());
        return finalWemFile;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static int ilog(int v) {
        int r = 0;
        while (v > 0) { v >>>= 1; r++; }
        return r;
    }
}
