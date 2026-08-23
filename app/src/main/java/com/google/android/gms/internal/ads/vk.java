package com.google.android.gms.internal.ads;

import java.util.ArrayList;

public final class vk {
    public final long f8886a;
    public final Object f8887b;
    public Object f8888c;
    public Object f8889d;
    public final Object f8890e;

    public vk(i5.c cVar, androidx.lifecycle.z zVar) {
        this.f8887b = cVar;
        this.f8890e = zVar;
        ArrayList arrayList = cVar.f11784c;
        this.f8886a = ((Long) arrayList.get(arrayList.size() - 1)).longValue();
        for (int i7 = 0; i7 < 3; i7++) {
            i5.d dVarB = cVar.b();
            v5.a aVarE = v5.a.e(dVarB.f11793h);
            if (i7 == 0) {
                aVarE.f(8);
                this.f8888c = new a6.a(this, aVarE);
            } else if (i7 == 1) {
                aVarE.h(dVarB.f11789d[1] * 8, 2);
                aVarE.f(8);
                this.f8889d = new a6.d(this, aVarE);
            }
        }
        if (this.f8888c == null) {
            throw new UnsupportedOperationException("The file has no identification header.");
        }
        if (this.f8889d == null) {
            throw new UnsupportedOperationException("The file has no setup header.");
        }
    }
}
