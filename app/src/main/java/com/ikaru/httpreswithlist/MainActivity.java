package com.ikaru.httpreswithlist;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.ikaru.httpreswithlist.UserService.BASE_URL;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    UserAdapter adapter;
    List<User> userList = new ArrayList<>();
    UserService apiService;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = new UserAdapter(userList);

        recyclerView = findViewById(R.id.recyclerview);
        Button button = findViewById(R.id.buttonRefresh);

        setupRetrofitAndOkHttp();
        getUsers();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUsers();
                Toast.makeText(MainActivity.this, "Getting Data", Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void getUsers(){
        Observable<List<User>> observable = apiService.getUsers();
        observable.subscribeOn(Schedulers.newThread()).
                observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<User>, List<User>>() {
                    @Override
                    public List<User> apply(List<User> users) throws Exception {
                        return users;
                    }

                }).subscribe(new Observer<List<User>>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(List<User> value) {
                userList = value;
                adapter.refill(value);
            }

            @Override
            public void onError(Throwable e) {
                Log.d("TEST", e.toString());
                Toast.makeText(getApplicationContext(), "There is no offline data on your phone , pls connect internet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public void setupRetrofitAndOkHttp() {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        File httpCacheDirectory = new File(getCacheDir(), "myListCach");

        //10 MB
        Cache cache = new Cache(httpCacheDirectory, 10 * 1024 * 1024);

        OkHttpClient client = new OkHttpClient().newBuilder()
                .cache(cache)
                .addNetworkInterceptor(REWRITE_RESPONSE_INTERCEPTOR)
                .addInterceptor(REWRITE_RESPONSE_INTERCEPTOR_OFFLINE)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .build();

        apiService = retrofit.create(UserService.class);

    }

    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Response originalResponse = chain.proceed(chain.request());
            String cacheControl = originalResponse.header("Cache-Control");
            if (cacheControl == null || cacheControl.contains("no-store") || cacheControl.contains("no-cache") ||
                    cacheControl.contains("must-revalidate") || cacheControl.contains("max-age=0")) {
                return originalResponse.newBuilder()
                        .removeHeader("Pragma")
                        //Cache Age 3600 = 1 hour
                        .header("Cache-Control", "public, max-age=" + 3600)
                        .build();
            } else {
                return originalResponse;
            }
        }
    };

    private final Interceptor REWRITE_RESPONSE_INTERCEPTOR_OFFLINE = new Interceptor() {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request.Builder builder = chain.request().newBuilder();


            //Do checking if there internet or not
            if (!isNetworkConnected()) {
                //Remove this header if you try to Get Path
                builder.removeHeader("Pragma")
                        //Reading from cache if there is no internet
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .header("Cache-Control", "public, only-if-cached")
                        .build();


            }else{
                //Force the data get from internet if there is internet
                builder.cacheControl(CacheControl.FORCE_NETWORK)
                        .build();
            }
            return chain.proceed(builder.build());
        }
    };


    //Checking the connection
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }


}
