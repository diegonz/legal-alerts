package es.smartidea.android.legalalerts.network;

import java.io.IOException;
import java.io.InputStream;

import es.smartidea.android.legalalerts.services.boeHandler.BoeHandler;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetWorker {

    OkHttpClient okHttpClient;

    public NetWorker() {
        this.okHttpClient = new OkHttpClient();
    }

    /**
     * Runs a OkHttpClient and returns a Response
     *
     * @param url URL of http document to fetch
     * @return OkHttpClient Response of http document according to given URL
     * @throws IOException If cannot resolve/fetch given URL
     */
    public Response getUrlAsResponse(String url) throws IOException {
        Request request = new Request.Builder().url(url).build();
        return okHttpClient.newCall(request).execute();
    }

    /**
     * Runs a OkHttpClient and returns a byte InputStream of http document according to given URL
     *
     * @param url URL of http document to fetch
     * @return Byte InputStream of http document according to given URL
     * @throws IOException If cannot resolve/fetch given URL
     */
    public InputStream getUrlAsByteStream(String url) throws IOException {
        return getUrlAsResponse(url).body().byteStream();
    }

    /**
     * Runs a OkHttpClient and returns true or false according to WAN availability
     * trying to resolve main server url (http://www.boe.es)
     *
     * @return boolean value true if WAN is available and false on working network unavailability
     */
    public boolean isWanAvailable(){
        Response response = null;
        try {
            response = getUrlAsResponse(BoeHandler.BOE_BASE_URL);
            return response.isSuccessful();
        } catch (IOException e){
            return false;
        } finally {
            if (response != null){
                response.body().close();
            }
        }

    }
}