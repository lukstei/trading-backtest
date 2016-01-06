package org.lst.trading.lib.util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.function.Consumer;

public class Http {
    private static final Logger log = LoggerFactory.getLogger(Http.class);

    private static CloseableHttpClient client;

    public synchronized static HttpClient getDefaultHttpClient() {
        if (client == null) {
            client = HttpClients.createDefault();
        }
        return client;
    }

    public static Observable<HttpResponse> get(String url, Consumer<HttpGet> configureRequest) {
        HttpGet request = new HttpGet(url);
        configureRequest.accept(request);

        return Observable.create(new OnSubscribe<HttpResponse>() {
            @Override public void call(Subscriber<? super HttpResponse> s) {
                try {
                    log.debug("GET {}", url);
                    s.onNext(getDefaultHttpClient().execute(request));
                    s.onCompleted();
                } catch (IOException e) {
                    s.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io());
    }

    public static Observable<HttpResponse> get(String url) {
        return get(url, x -> {
        });
    }

    public static Func1<? super HttpResponse, ? extends Observable<String>> asString() {
        return t -> {
            try {
                return Observable.just(EntityUtils.toString(t.getEntity()));
            } catch (IOException e) {
                return Observable.error(e);
            }
        };
    }
}
