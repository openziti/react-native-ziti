
package io.netfoundry.ziti.rn;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.modules.network.OkHttpClientFactory;
import com.facebook.react.modules.network.OkHttpClientProvider;
import com.facebook.react.modules.network.ReactCookieJarContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.netfoundry.ziti.Ziti;
import okhttp3.Dns;
import okhttp3.OkHttpClient;

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
              .cookieJar(new ReactCookieJarContainer());

      return builder.build();
    }
  };

}