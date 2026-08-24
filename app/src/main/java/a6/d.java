package a6;

import androidx.lifecycle.z;
import com.google.android.gms.internal.ads.vk;
import e.j0;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public final class d extends j0 {
    public static final q3.d[] f153k;
    public static final StringBuilder DEBUG_LOG = new StringBuilder();

    public final HashMap f154c;
    public final int f155d;
    public int f156e;
    public boolean[] f157f;
    public int f158g;
    public long f159h;
    public int f160i;
    public int f161j;

    static {
        int i7 = 1;
        int i8 = 0;
        int i9 = 6;
        f153k = new q3.d[]{
            new q3.d(1, i8, i8, i9), new q3.d(2, i8, i8, i9), new q3.d(3, i8, i8, i9), new q3.d(4, i8, i8, i9), new q3.d(5, i8, i8, i9),
            new q3.d(1, i7, i8, i9), new q3.d(2, i7, i8, i9), new q3.d(3, i7, i8, i9), new q3.d(4, i7, i8, i9), new q3.d(5, i7, i8, i9),
            new q3.d(1, i8, i7, i9), new q3.d(2, i8, i7, i9), new q3.d(3, i8, i7, i9), new q3.d(4, i8, i7, i9), new q3.d(5, i8, i7, i9),
            new q3.d(1, i7, i7, i9), new q3.d(2, i7, i7, i9), new q3.d(3, i7, i7, i9), new q3.d(4, i7, i7, i9), new q3.d(5, i7, i7, i9)
        };
    }

    public d(vk vkVar, v5.a aVar) {
        super(vkVar, aVar);
        int i7;
        boolean z6;
        int iPow;
        DEBUG_LOG.setLength(0);
        this.f154c = new HashMap();
        int i8 = 0;
        this.f156e = 0;
        this.f159h = 0L;
        this.f160i = 0;
        this.f161j = 0;
        int iF = aVar.f(8) + 1;
        boolean z7 = true;
        int i9 = 0;
        while (i8 < iF) {
            int i10 = aVar.f15030d;
            this.f154c.put(Integer.valueOf(i8), new ArrayList());
            boolean z8 = z7;
            int i11 = i9;
            while (i9 < 20) {
                q3.d dVar = f153k[i9];
                aVar.h(i10, z8 ? 1 : 0);
                v5.a aVarD = v5.a.d();
                if (((v5.a) this.f10944a).f(24) != 5653314) {
                    throw new UnsupportedOperationException("The code book sync pattern is not correct. DEBUG: i8(codebook)=" + i8 + " i9(template)=" + i9 + " i10(bitpos)=" + i10 + " actualReaderPos=" + ((v5.a) this.f10944a).f15030d);
                }
                int i12 = aVarD.i(16, 4, (v5.a) this.f10944a);
                int i13 = aVarD.i(24, 14, (v5.a) this.f10944a);
                int[] iArr = new int[i13];
                boolean zG = ((v5.a) this.f10944a).g();
                aVarD.j(z8 ? 1 : 0, zG ? 1 : 0);
                if (zG) {
                    aVarD.i(5, 5, (v5.a) this.f10944a);
                    int i14 = i11;
                    while (i14 < i13) {
                        int i15 = i13 - i14;
                        int i16 = i11;
                        while (i15 > 0) {
                            i15 >>= 1;
                            i16++;
                        }
                        i14 += aVarD.i(i16, i16, (v5.a) this.f10944a);
                        if (i14 > i13) {
                            throw new UnsupportedOperationException("The codebook entry length list is longer than the actual number of entry lengths.");
                        }
                    }
                } else {
                    int i17 = dVar.f13441b;
                    aVarD.j(3, i17);
                    boolean zG2 = ((v5.a) this.f10944a).g();
                    aVarD.j(z8 ? 1 : 0, zG2 ? 1 : 0);
                    int i18 = 0;
                    while (i18 < i13) {
                        if (zG2) {
                            boolean zG3 = ((v5.a) this.f10944a).g();
                            aVarD.j(z8 ? 1 : 0, zG3 ? 1 : 0);
                            z8 = zG3;
                        }
                        if (z8) {
                            int iF2 = ((v5.a) this.f10944a).f(5) - dVar.f13442c;
                            iArr[i18] = iF2;
                            aVarD.j(i17, iF2);
                        }
                        i18++;
                        z8 = true;
                    }
                }
                int iF3 = ((v5.a) this.f10944a).f(4);
                if (i9 == 0) {
                    DEBUG_LOG.append("cb#").append(i8).append(": dims=").append(i12)
                        .append(" entries=").append(i13).append(" ordered=").append(zG)
                        .append(" lookupType=").append(iF3).append("\n");
                    if (DEBUG_LOG.length() > 4000) DEBUG_LOG.delete(0, DEBUG_LOG.length() - 3000);
                }
                aVarD.j(1, (iF3 == 1 || iF3 == 2) ? 1 : 0);
                boolean debugThis = (i9 == 0 && i8 == 28);
                if (debugThis) DEBUG_LOG.append("  [truoc lookup] pos=").append(((v5.a) this.f10944a).f15030d).append("\n");
                if (iF3 != 0) {
                    if (iF3 != 1 && iF3 != 2) {
                        throw new UnsupportedOperationException("Unsupported codebook lookup type: " + iF3);
                    }
                    aVarD.i(32, 32, (v5.a) this.f10944a);
                    aVarD.i(32, 32, (v5.a) this.f10944a);
                    if (debugThis) DEBUG_LOG.append("  [sau min+max] pos=").append(((v5.a) this.f10944a).f15030d).append("\n");
                    int i19 = aVarD.i(4, 4, (v5.a) this.f10944a) + dVar.f13443d;
                    int i20 = 1;
                    aVarD.j(1, ((v5.a) this.f10944a).g() ? 1 : 0);
                    if (debugThis) DEBUG_LOG.append("  [sau valuebits+seq] pos=").append(((v5.a) this.f10944a).f15030d).append(" i19(valuebits)=").append(i19).append("\n");
                    if (iF3 == 1) {
                        iPow = (int) Math.pow(2.718281828459045d, Math.log(i13) / ((double) i12));
                        int i21 = iPow + 1;
                        while (i12 > 0) {
                            i20 *= i21;
                            i12--;
                        }
                        if (i20 <= i13) {
                            iPow = i21;
                        }
                    } else {
                        iPow = i13 * i12;
                    }
                    if (debugThis) DEBUG_LOG.append("  [iPow(quantvals)=").append(iPow).append("]\n");
                    for (int i22 = 0; i22 < iPow; i22++) {
                        aVarD.i(i19, i19, (v5.a) this.f10944a);
                    }
                    if (debugThis) DEBUG_LOG.append("  [sau doc xong values] pos=").append(((v5.a) this.f10944a).f15030d).append("\n");
                }
                byte[] bArrC = aVarD.c();
                Iterator it = ((z) vkVar.f8890e).f1304a.values().iterator();
                int i23 = 0;
                while (true) {
                    if (!it.hasNext()) {
                        i23 = -1;
                        break;
                    }
                    byte[] bArr = (byte[]) it.next();
                    if (bArr != bArrC) {
                        if (bArr != null && bArrC != null) {
                            int length = bArrC.length;
                            if (bArrC.length <= bArr.length) {
                                int i24 = 0;
                                while (true) {
                                    if (i24 >= length) {
                                        z6 = true;
                                        break;
                                    } else if (bArr[i24] == bArrC[i24]) {
                                        i24++;
                                    } else {
                                        z6 = false;
                                        break;
                                    }
                                }
                            } else {
                                z6 = false;
                            }
                        } else {
                            z6 = false;
                        }
                    } else {
                        z6 = true;
                    }
                    if (z6) {
                        break;
                    } else {
                        i23++;
                    }
                }
                if (i23 != -1) {
                    ((ArrayList) this.f154c.get(Integer.valueOf(i8))).add(new c(i23));
                }
                i9++;
                i11 = 0;
                z8 = true;
                i10 = i10;
            }
            i8++;
            i9 = 0;
            z7 = true;
        }
        this.f155d = aVar.f15030d;
        this.f154c.entrySet().removeIf(entryObj -> ((ArrayList) ((Map.Entry) entryObj).getValue()).isEmpty());
        this.f154c.values().forEach(new Consumer() {
            @Override
            public void accept(Object obj) {
                ArrayList arrayList = (ArrayList) obj;
                int size = arrayList.size();
                if (size > f156e) {
                    f156e = size;
                }
            }
        });
        if (this.f154c.size() != iF) {
            throw new UnsupportedOperationException("Invalid codebook");
        }
    }
}
