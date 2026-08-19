/* wav2ogg.c - Encode file WAV (16-bit PCM) sang Ogg Vorbis, dùng để test build aoTuV.
 * Cấu trúc dựa theo encoder_example.c chuẩn đi kèm libvorbis (Xiph.Org, BSD-style license).
 * Build: gcc wav2ogg.c -o wav2ogg -lvorbisenc -lvorbis -logg -lm
 * Usage: ./wav2ogg input.wav output.ogg quality(-0.1..1.0)
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <vorbis/vorbisenc.h>

typedef struct {
    int channels;
    int sampleRate;
    int bitsPerSample;
    unsigned char *data;
    unsigned int dataSize;
} WavData;

static unsigned int read_u32le(FILE *f) {
    unsigned char b[4];
    fread(b, 1, 4, f);
    return b[0] | (b[1] << 8) | (b[2] << 16) | (b[3] << 24);
}
static unsigned short read_u16le(FILE *f) {
    unsigned char b[2];
    fread(b, 1, 2, f);
    return b[0] | (b[1] << 8);
}

static int load_wav(const char *path, WavData *out) {
    FILE *f = fopen(path, "rb");
    if (!f) { fprintf(stderr, "Khong mo duoc file WAV: %s\n", path); return -1; }
    char riff[4], wave[4];
    fread(riff, 1, 4, f);
    read_u32le(f); /* riff size */
    fread(wave, 1, 4, f);
    if (memcmp(riff, "RIFF", 4) != 0 || memcmp(wave, "WAVE", 4) != 0) {
        fprintf(stderr, "File khong phai WAV hop le\n");
        fclose(f);
        return -1;
    }
    int haveFmt = 0, haveData = 0;
    while (!feof(f)) {
        char id[4];
        if (fread(id, 1, 4, f) != 4) break;
        unsigned int size = read_u32le(f);
        long chunkStart = ftell(f);
        if (memcmp(id, "fmt ", 4) == 0) {
            read_u16le(f); /* audio format */
            out->channels = read_u16le(f);
            out->sampleRate = read_u32le(f);
            read_u32le(f); /* byte rate */
            read_u16le(f); /* block align */
            out->bitsPerSample = read_u16le(f);
            haveFmt = 1;
        } else if (memcmp(id, "data", 4) == 0) {
            out->dataSize = size;
            out->data = malloc(size);
            fread(out->data, 1, size, f);
            haveData = 1;
        }
        fseek(f, chunkStart + size + (size % 2), SEEK_SET);
    }
    fclose(f);
    if (!haveFmt || !haveData) {
        fprintf(stderr, "Thieu chunk fmt/data\n");
        return -1;
    }
    if (out->bitsPerSample != 16) {
        fprintf(stderr, "Chi ho tro 16-bit PCM (file nay la %d-bit)\n", out->bitsPerSample);
        return -1;
    }
    return 0;
}

int main(int argc, char **argv) {
    if (argc < 3) {
        fprintf(stderr, "Usage: %s input.wav output.ogg [quality -0.1..1.0]\n", argv[0]);
        return 1;
    }
    const char *inPath = argv[1];
    const char *outPath = argv[2];
    float quality = (argc > 3) ? (float)atof(argv[3]) : 0.4f;

    WavData wav = {0};
    if (load_wav(inPath, &wav) != 0) return 1;

    int numSamples = wav.dataSize / 2 / wav.channels;
    fprintf(stderr, "WAV: %d kenh, %d Hz, %d sample, quality=%.2f\n",
            wav.channels, wav.sampleRate, numSamples, quality);

    FILE *out = fopen(outPath, "wb");
    if (!out) { fprintf(stderr, "Khong mo duoc file output\n"); return 1; }

    ogg_stream_state os;
    ogg_page og;
    ogg_packet op;
    vorbis_info vi;
    vorbis_comment vc;
    vorbis_dsp_state vd;
    vorbis_block vb;

    vorbis_info_init(&vi);
    int ret = vorbis_encode_init_vbr(&vi, wav.channels, wav.sampleRate, quality);
    if (ret) {
        fprintf(stderr, "vorbis_encode_init_vbr that bai: %d\n", ret);
        return 1;
    }

    vorbis_comment_init(&vc);
    vorbis_comment_add_tag(&vc, "ENCODER", "wav2ogg-aotuv-test");

    vorbis_analysis_init(&vd, &vi);
    vorbis_block_init(&vd, &vb);

    srand(time(NULL));
    ogg_stream_init(&os, rand());

    {
        ogg_packet header, header_comm, header_code;
        vorbis_analysis_headerout(&vd, &vc, &header, &header_comm, &header_code);
        ogg_stream_packetin(&os, &header);
        ogg_stream_packetin(&os, &header_comm);
        ogg_stream_packetin(&os, &header_code);
        while (ogg_stream_flush(&os, &og)) {
            fwrite(og.header, 1, og.header_len, out);
            fwrite(og.body, 1, og.body_len, out);
        }
    }

    int channels = wav.channels;
    short *pcm = (short *)wav.data;
    int pos = 0;
    int eos = 0;

    while (!eos) {
        int chunk = 1024;
        int remain = numSamples - pos;
        int n = (remain < chunk) ? remain : chunk;

        if (n > 0) {
            float **buffer = vorbis_analysis_buffer(&vd, chunk);
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < channels; c++) {
                    buffer[c][i] = pcm[(pos + i) * channels + c] / 32768.0f;
                }
            }
            vorbis_analysis_wrote(&vd, n);
            pos += n;
        } else {
            vorbis_analysis_wrote(&vd, 0);
        }

        while (vorbis_analysis_blockout(&vd, &vb) == 1) {
            vorbis_analysis(&vb, NULL);
            vorbis_bitrate_addblock(&vb);
            while (vorbis_bitrate_flushpacket(&vd, &op)) {
                ogg_stream_packetin(&os, &op);
                while (!ogg_page_eos(&og)) {
                    int result = ogg_stream_pageout(&os, &og);
                    if (result == 0) break;
                    fwrite(og.header, 1, og.header_len, out);
                    fwrite(og.body, 1, og.body_len, out);
                    if (ogg_page_eos(&og)) { eos = 1; break; }
                }
            }
        }
        if (n == 0) break;
    }

    ogg_stream_clear(&os);
    vorbis_block_clear(&vb);
    vorbis_dsp_clear(&vd);
    vorbis_comment_clear(&vc);
    vorbis_info_clear(&vi);
    fclose(out);
    free(wav.data);

    fprintf(stderr, "Xong: %s\n", outPath);
    return 0;
}
