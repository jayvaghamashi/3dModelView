package com.example.a3dviewapp.network;

import com.example.a3dviewapp.model.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "https://shirts.mobilealchemy.co/";
    private static Retrofit retrofit = null;

    public interface ApiInterface {
        @POST("api/index.php")
        Call<ApiResponse> getProducts(@Body ProductRequest request);
    }

    public static class ProductRequest {
        private String action;
        private String page;
        private String limit;

        public ProductRequest(String action, String page, String limit) {
            this.action = action;
            this.page = page;
            this.limit = limit;
        }
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}