package com.google.android.gms.internal.ads;

import java.util.ArrayList;
import java.util.Arrays;

public final class vk {
    public final long f8886a;
    public final Object f8887b;
    public Object f8888c;
    public Object f8889d;
    public final Object f8890e;

    // Bo doc goi (packet) header dung chung cho ca 3 header packet (id/comment/setup),
    // duoc tiep tuc su dung o WemConverter cho vong doc audio packet phia sau (KHONG tao
    // moi c6.a khac, tranh mat/lech 1 page da duoc "doc truoc" - xem b()).
    public final c6.a f8891f;

    public vk(i5.c cVar, androidx.lifecycle.z zVar) {
        this.f8887b = cVar;
        this.f8890e = zVar;
        ArrayList arrayList = cVar.f11784c;
        this.f8886a = ((Long) arrayList.get(arrayList.size() - 1)).longValue();

        // Truoc day code doc "1 page = 1 header packet" va dung mot cong thuc offset (dua
        // vao kich thuoc segment dau cua page thu 2) de nhay thang vao du lieu setup header,
        // voi gia dinh ngam la ca comment header + setup header (chua toan bo codebook) nam
        // gon trong DUY NHAT 1 Ogg page. Voi file WAV lon/chat luong cao, setup header (danh
        // sach codebook) co the vuot qua 1 page va trai dai qua nhieu page ke tiep, khien
        // gia dinh do sai -> doc trung du lieu bi cat cut/lech bit -> loi "code book sync
        // pattern is not correct".
        //
        // Fix: dung chung mot bo doc packet (c6.a) co kha nang noi cac segment/page lien tiep
        // lai thanh 1 packet logic hoan chinh (giong cach WemConverter dang doc cac audio
        // packet ben duoi), roi lay dung 3 packet header dau tien theo thu tu chuan cua
        // Vorbis: packet 0 = identification, packet 1 = comment (bo qua, khong dung), packet 2
        // = setup (chua codebook).
        c6.a packetReader = new c6.a(cVar);
        for (int i7 = 0; i7 < 3; i7++) {
            byte[] packetBytes = b(packetReader);
            if (packetBytes == null) break;
            v5.a aVarE = v5.a.e(packetBytes);
            if (i7 == 0) {
                aVarE.f(8);
                this.f8888c = new a6.a(this, aVarE);
            } else if (i7 == 2) {
                aVarE.f(8);
                this.f8889d = new a6.d(this, aVarE);
            }
            // i7 == 1: comment/vendor header, khong can dung, bo qua.
        }
        this.f8891f = packetReader;

        if (this.f8888c == null) {
            throw new UnsupportedOperationException("The file has no identification header.");
        }
        if (this.f8889d == null) {
            throw new UnsupportedOperationException("The file has no setup header.");
        }
    }

    // Doc 1 packet logic hoan chinh, tu dong noi cac segment (va sang ca page ke tiep neu
    // packet bi cat qua nhieu page - lacing value = 255 bao hieu con tiep).
    private static byte[] b(c6.a reader) {
        byte[] assembled = null;
        while (true) {
            if (reader.f1916d >= reader.f1918f) {
                if (reader.f1915c != null) {
                    reader.a();
                    reader.f1916d = 0;
                } else {
                    reader.f1917e = true;
                }
            }
            if (reader.f1917e) break;
            int[] sizes = reader.f1919g;
            int idx = reader.f1916d;
            int segSize = sizes[idx];
            int segOff = reader.f1920h[idx];
            byte[] chunk = Arrays.copyOfRange(reader.f1921i, segOff, segOff + segSize);
            assembled = (assembled == null) ? chunk : concat(assembled, chunk);
            reader.f1916d++;
            if (segSize < 255) break;
        }
        return assembled;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }
}
