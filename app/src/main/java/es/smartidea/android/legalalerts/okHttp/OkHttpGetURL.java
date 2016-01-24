package es.smartidea.android.legalalerts.okHttp;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpGetURL {
    public static InputStream run(final String urlString) throws IOException {
        return new OkHttpClient().newCall(new Request.Builder().url(urlString).build())
                .execute()
                .body()
                .byteStream();
    }
}