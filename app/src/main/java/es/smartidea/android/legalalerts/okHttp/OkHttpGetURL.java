package es.smartidea.android.legalalerts.okHttp;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpGetURL {
    OkHttpClient client = new OkHttpClient();

    public InputStream run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();

        return response.body().byteStream();
    }
}