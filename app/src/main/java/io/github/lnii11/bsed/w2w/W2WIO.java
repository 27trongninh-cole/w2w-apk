package io.github.lnii11.bsed.w2w;

public abstract class W2WIO {
    /**
     * Gọi hàm native trong libmwem.so (đã trích xuất từ SBank Editor gốc).
     * str = đường dẫn WAV input, str2 = đường dẫn OGG output,
     * i7 = channels, i8 = sample rate, f7 = quality (-0.1 .. 1.0)
     * Trả về 1 nếu thành công.
     */
    public static native int wenc(String str, String str2, int i7, int i8, float f7);
}
