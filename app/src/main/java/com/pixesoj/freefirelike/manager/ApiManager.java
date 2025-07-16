package com.pixesoj.freefirelike.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class ApiManager {
    public interface ApiCallback {
        void onSuccess(String responseBody, JsonElement responseElement);
        void onError(Exception e);

        default void onNoInternet(Context context) { }

        default void onTimeout(Context context) {
            System.out.println("Request timed out (default handler).");
        }
    }

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static boolean isInternetAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static void get(Context context, String url, Map<String, String> headers,
                           int timeoutSeconds, int maxRetries, ApiCallback callback) {
        if (!isInternetAvailable(context)) {
            mainHandler.post(() -> callback.onNoInternet(context));
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        Request.Builder builder = new Request.Builder().url(url);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();

        executeWithRetry(context, client, request, maxRetries, callback);
    }

    public static void post(Context context, String url, Map<String, String> headers,
                            String jsonString, int timeoutSeconds, int maxRetries, ApiCallback callback) {
        if (!isInternetAvailable(context)) {
            mainHandler.post(() -> callback.onNoInternet(context));
            return;
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonString, JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(body);

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();

        executeWithRetry(context, client, request, maxRetries, callback);
    }

    private static void executeWithRetry(Context context, OkHttpClient client, Request request, int retriesLeft, ApiCallback callback) {
        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                if (isTimeoutException(e)) {
                    mainHandler.post(() -> callback.onTimeout(context));
                } else if (retriesLeft > 0) {
                    executeWithRetry(context, client, request, retriesLeft - 1, callback);
                } else {
                    mainHandler.post(() -> callback.onError(e));
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        Gson gson = new Gson();
                        mainHandler.post(() -> callback.onSuccess(body, gson.fromJson(body, JsonElement.class)));
                    } else {
                        mainHandler.post(() -> callback.onError(
                                new IOException("Unexpected code: " + response.code())));
                    }
                } catch (IOException | com.google.gson.JsonSyntaxException e) {
                    mainHandler.post(() -> callback.onError(e));
                }
            }
        });
    }

    private static boolean isTimeoutException(IOException e) {
        return e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException
                || e instanceof okhttp3.internal.http2.StreamResetException;
    }
}