package sslengine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import messages.Message;
import peer.Constants;

public class SSLUtils {
  public static AsynchronousSocketChannel getSocketChannel() throws KeyManagementException, Exception {
    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(createKeyManagers("jsse" + File.separator + "client.keys", "123456", "123456"),
        createTrustManagers("jsse" + File.separator + "truststore", "123456"), new SecureRandom());
    SSLEngine engine = sslContext.createSSLEngine();
    engine.setUseClientMode(true);
    String[] cipher_suites = new String[] {"TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384", "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256"};
    engine.setEnabledCipherSuites(cipher_suites);
    return new SLLAsynchronousSocketChannel(AsynchronousSocketChannel.open(), engine);
  }

  public static AsynchronousServerSocketChannel getServerSocketChannel(AsynchronousChannelGroup group)
      throws KeyManagementException, Exception {
    AsynchronousServerSocketChannel socketChannel = AsynchronousServerSocketChannel.open(group);

    SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
    sslContext.init(createKeyManagers("jsse" + File.separator + "server.keys", "123456", "123456"),
        createTrustManagers("jsse" + File.separator + "truststore", "123456"), new SecureRandom());
    return new SSLAsynchronousServerSocketChannel(socketChannel, sslContext);
  }

  private static KeyManager[] createKeyManagers(String filepath, String keystorePassword, String keyPassword)
      throws Exception {
    KeyStore keyStore = KeyStore.getInstance("JKS");
    InputStream keyStoreIS = new FileInputStream(filepath);
    try {
      keyStore.load(keyStoreIS, keystorePassword.toCharArray());
    } finally {
      if (keyStoreIS != null) {
        keyStoreIS.close();
      }
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, keyPassword.toCharArray());
    return kmf.getKeyManagers();
  }

  private static TrustManager[] createTrustManagers(String filepath, String keystorePassword) throws Exception {
    KeyStore trustStore = KeyStore.getInstance("JKS");
    InputStream trustStoreIS = new FileInputStream(filepath);
    try {
      trustStore.load(trustStoreIS, keystorePassword.toCharArray());
    } finally {
      if (trustStoreIS != null) {
        trustStoreIS.close();
      }
    }
    TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustFactory.init(trustStore);
    return trustFactory.getTrustManagers();
  }

  public static <A> void sendMessage(Message reply, AsynchronousSocketChannel channel, A attachment, CompletionHandler<Void, A> handler) {
    ByteBuffer buffer = ByteBuffer.wrap(reply.getContent());
    channel.write(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, new CompletionHandler<Integer, Void>() {
      @Override
      public void completed(Integer result, Void attatchment) {
        if (result > 0) {
          if (buffer.hasRemaining()) {
            channel.write(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, this);
          } else handler.completed(null, attachment);
        } else handler.failed(null, attachment);
      }

      @Override
      public void failed(Throwable exc, Void attatchment) {
        handler.failed(null, attachment);
      }
    });
  }

  public static <A> void sendMessage(Message reply, AsynchronousSocketChannel channel, InetSocketAddress address, A attachment, CompletionHandler<Void, A> handler) {
    channel.connect(address, null, new CompletionHandler<Void,Void>() {
      @Override
      public void completed(Void arg0, Void arg1) {
        SSLUtils.sendMessage(reply, channel, null, handler);
      }

      @Override
      public void failed(Throwable arg0, Void arg1) {
        handler.failed(arg0, attachment);
      }
    });
  }

  public static void readMessage(AsynchronousSocketChannel channel, CompletionHandler<Void,Message> handler) {
    ByteBuffer buffer = ByteBuffer.allocate(Constants.BUFFER_SIZE);
    StringBuffer stringBuffer = new StringBuffer();

    channel.read(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, new CompletionHandler<Integer,Void>() {

      @Override
      public void completed(Integer result, Void attachment) {
        if (result < 0) {
          handler.failed(null, null);
        } else {
            if (result > 0) {
              stringBuffer.append(new String(buffer.array()));
            }

            if (stringBuffer.toString().contains(Message.CRLF + Message.CRLF)) {
              handler.completed(null, messages.MessageHandler.handleMessage(buffer));
            } else {
              buffer.clear();
              channel.read(buffer, Constants.TIMEOUT_SECONDS, TimeUnit.SECONDS, null, this);
            }
        }
        
      }

      @Override
      public void failed(Throwable exc, Void attachment) {
        handler.failed(null, null);
      }
    });
  }

  public static void closeChannel(AsynchronousSocketChannel channel) {
    try {
      if(channel.isOpen()) {
        channel.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void closeChannel(AsynchronousServerSocketChannel serverChannel) {
    try {
      if(serverChannel.isOpen()) {
        serverChannel.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}