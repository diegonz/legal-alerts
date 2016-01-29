package es.smartidea.android.legalalerts.okHttp;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpGetURL {
    OkHttpClient okHttpClient = new OkHttpClient();

    /**
     * Runs a OkHttpClient and returns an byte InputStream
     *
     * @param url URL of http document to fetch
     * @return Byte InputStream of http document according to given URL
     * @throws IOException If cannot resolve/fetch given URL
     */
    public InputStream run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = okHttpClient.newCall(request).execute();

        return response.body().byteStream();
    }

    /**
     * Runs a OkHttpClient and returns true or false according to WAN availability.
     *
     * @return boolean value true if WAN is available and false on working network unavailability
     * @throws IOException If cannot resolve given URL
     */
    public boolean isWanAvailable() throws IOException {
        Request request = new Request.Builder()
                .url("http://www.google.com")
                .build();

        return okHttpClient.newCall(request).execute().isSuccessful();

    }
}