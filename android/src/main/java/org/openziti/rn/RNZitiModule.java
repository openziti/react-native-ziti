
package org.openziti.rn;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.network.OkHttpClientFactory;
import com.facebook.react.modules.network.OkHttpClientProvider;
import com.facebook.react.modules.network.ReactCookieJarContainer;

import org.openziti.Ziti;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RNZitiModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNZitiModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;

    OkHttpClientProvider.setOkHttpClientFactory(zitiClientFactory);
  }

  @Override
  public String getName() {
    return "RNZiti";
  }

  private final Dns zitiDns = new Dns(){
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
      InetAddress address = Ziti.getDNSResolver().resolve(hostname);
      if (address == null) {
        address = InetAddress.getByName(hostname);
      }

      return address != null ? Collections.singletonList(address)
              : Collections.<InetAddress>emptyList();
    }
  };

  private final OkHttpClientFactory zitiClientFactory = new OkHttpClientFactory() {
    @Override
    public OkHttpClient createNewNetworkModuleClient() {
      SSLSocketFactory sslSocketFactory = Ziti.getSSLSocketFactory();
      X509TrustManager tm = null;
      try {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init((KeyStore) null);
        tm = (X509TrustManager) tmf.getTrustManagers()[0];
      } catch (Exception ex) {

      }
      OkHttpClient.Builder builder = new OkHttpClient.Builder()
              .connectTimeout(0, TimeUnit.MILLISECONDS)
              .readTimeout(0, TimeUnit.MILLISECONDS)
              .writeTimeout(0, TimeUnit.MILLISECONDS)
              .socketFactory(Ziti.getSocketFactory())
              .sslSocketFactory(sslSocketFactory, tm)
              .dns(zitiDns)
              .cookieJar(new ReactCookieJarContainer())
              .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                  // chain = chain.withReadTimeout(5, TimeUnit.MINUTES);
                  Request req = chain.request();
                  Log.i("ziti-rn", String.format("requesting  %s %s (to %d)", req.method(), req.url(), chain.connectTimeoutMillis()));
                  Response resp = chain.proceed(req);
                  Log.i("ziti-rn", String.format("resp %s", resp));
                  return resp;
                }
              });

      return builder.build();
    }
  };

}