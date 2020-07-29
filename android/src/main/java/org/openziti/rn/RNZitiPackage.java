
package org.openziti.rn;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;

import org.openziti.android.Ziti;

import javax.annotation.Nullable;
import javax.net.SocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Protocol;
import okhttp3.internal.platform.Platform;
import okhttp3.internal.tls.CertificateChainCleaner;
import okhttp3.internal.tls.TrustRootIndex;

public class RNZitiPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        // we have to use seamless mode here to make React Native websockets module work
        // RN websockets does not use client factory override
        try {
            Ziti.init(reactContext.getApplicationContext(), true);
            final Platform realPlatform = Platform.get();

            Platform proxyPlatform = new zitiPlatform(realPlatform);

            Field plf = Platform.class.getDeclaredField("PLATFORM");
            plf.setAccessible(true);
            plf.set(null, proxyPlatform);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return Arrays.<NativeModule>asList(new RNZitiModule(reactContext));
    }

    // Deprecated from RN 0.47
    public List<Class<? extends JavaScriptModule>> createJSModules() {
      return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
      return Collections.emptyList();
    }

    class ZitiSSLSPI extends SSLContextSpi {
        SSLContext defssl;
        ZitiSSLSPI() {
            try {
                defssl = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
            }
        }

        @Override
        protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) throws KeyManagementException {
            defssl.init(km, tm, sr);
        }

        @Override
        protected SSLSocketFactory engineGetSocketFactory() {
            return new SSLSocketFactory() {
                @Override
                public String[] getDefaultCipherSuites() { return defssl.getDefaultSSLParameters().getCipherSuites(); }

                @Override
                public String[] getSupportedCipherSuites() { return defssl.getSupportedSSLParameters().getCipherSuites(); }

                @Override
                public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
                    try {
                        return Ziti.getSSLSocketFactory().createSocket(s, host, port, autoClose);
                    } catch (Exception ex) {
                        return null;
                    }
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                    return defssl.getSocketFactory().createSocket(host, port);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
                    return defssl.getSocketFactory().createSocket(host, port, localHost, localPort);
                }

                @Override
                public Socket createSocket(InetAddress host, int port) throws IOException {
                    return defssl.getSocketFactory().createSocket(host, port);
                }

                @Override
                public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
                    return defssl.getSocketFactory().createSocket(address, port, localAddress, localPort);
                }
            };
        }

        @Override
        protected SSLServerSocketFactory engineGetServerSocketFactory() {
            return defssl.getServerSocketFactory();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine() {
            return defssl.createSSLEngine();
        }

        @Override
        protected SSLEngine engineCreateSSLEngine(String host, int port) {
            return defssl.createSSLEngine(host, port);
        }

        @Override
        protected SSLSessionContext engineGetServerSessionContext() {
            return defssl.getServerSessionContext();
        }

        @Override
        protected SSLSessionContext engineGetClientSessionContext() {
            return defssl.getClientSessionContext();
        }
    }

    class zitiPlatform extends Platform {
        final Platform realPlatform;
        SSLContext defaultSSL;

        zitiPlatform(Platform real) {
            realPlatform = real;
            try {
                defaultSSL = SSLContext.getInstance("TLS");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }

        @Nullable
        @Override
        protected X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
            return super.trustManager(sslSocketFactory);
        }

        @Override
        public void configureTlsExtensions(SSLSocket sslSocket, @Nullable String hostname, List<Protocol> protocols) throws IOException {
            super.configureTlsExtensions(sslSocket, hostname, protocols);
        }

        @Override
        public void afterHandshake(SSLSocket sslSocket) {
            realPlatform.afterHandshake(sslSocket);
        }

        @Nullable
        @Override
        public String getSelectedProtocol(SSLSocket socket) {
            return realPlatform.getSelectedProtocol(socket);
        }

        @Override
        public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
            realPlatform.connectSocket(socket, address, connectTimeout);
        }

        @Override
        public void log(int level, String message, @Nullable Throwable t) {
            realPlatform.log(level, message, t);
        }

        @Override
        public boolean isCleartextTrafficPermitted(String hostname) {
            return realPlatform.isCleartextTrafficPermitted(hostname);
        }

        @Nullable
        @Override
        public Object getStackTraceForCloseable(String closer) {
            return realPlatform.getStackTraceForCloseable(closer);
        }

        @Override
        public void logCloseableLeak(String message, Object stackTrace) {
            realPlatform.logCloseableLeak(message, stackTrace);
        }

        @Override
        public CertificateChainCleaner buildCertificateChainCleaner(X509TrustManager trustManager) {
            return realPlatform.buildCertificateChainCleaner(trustManager);
        }

        @Override
        public CertificateChainCleaner buildCertificateChainCleaner(SSLSocketFactory sslSocketFactory) {
            return realPlatform.buildCertificateChainCleaner(sslSocketFactory);
        }

        @Override
        public TrustRootIndex buildTrustRootIndex(X509TrustManager trustManager) {
            return realPlatform.buildTrustRootIndex(trustManager);
        }

        @Override
        public void configureSslSocketFactory(SSLSocketFactory socketFactory) {
            realPlatform.configureSslSocketFactory(socketFactory);
        }

        @Override
        public SSLContext getSSLContext() {
            return new SSLContext(new ZitiSSLSPI(), defaultSSL.getProvider(), defaultSSL.getProtocol()) {
            };
        }
    }
}