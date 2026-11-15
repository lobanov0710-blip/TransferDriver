package ru.transferservis.driver.data.registration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual HTTP/OkHttp follow-up, not an application interceptor that bypasses retries. */
public final class MutationReplayTest {
    @Test public void stockReplayableBodyRepeats503DespiteConnectionRetriesDisabled() throws Exception {
        assertEquals(2, requests(RequestBody.create(null, "{}")));
    }
    @Test public void registrationMutationDoesNotRepeat503RetryAfterZero() throws Exception {
        RequestBody body=ru.transferservis.driver.data.api.SecureHttpClient.oneShot(RequestBody.create(null, "{}"));
        assertTrue(body.isOneShot());assertEquals(1, requests(body));
    }
    private int requests(RequestBody body) throws Exception {
        AtomicInteger calls=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();
        try(ServerSocket server=new ServerSocket(0,2,java.net.InetAddress.getLoopbackAddress())) {
            server.setSoTimeout(5000);
            Thread provider=new Thread(()->{
                try {
                    while(calls.get()<2)try(Socket socket=server.accept()) {
                        socket.setSoTimeout(5000);BufferedReader reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                        int length=0;String line;
                        while((line=reader.readLine())!=null&&!line.isEmpty())if(line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))length=Integer.parseInt(line.substring(15).trim());
                        for(int i=0;i<length;i++)if(reader.read()<0)throw new java.io.IOException("Incomplete synthetic body");
                        calls.incrementAndGet();socket.getOutputStream().write("HTTP/1.1 503 Unavailable\r\nRetry-After: 0\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                    }
                }catch(java.net.SocketException cancelled){if(!server.isClosed())error.set(cancelled);}
                catch(Throwable failure){error.set(failure);}
            });
            provider.start();
            OkHttpClient client=new OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).callTimeout(8,java.util.concurrent.TimeUnit.SECONDS).build();
            try {
                Request request=new Request.Builder().url("http://127.0.0.1:"+server.getLocalPort()+"/synthetic-mutation").post(body).build();
                try(Response response=client.newCall(request).execute()){assertEquals(503,response.code());if(body.isOneShot())assertNull(response.priorResponse());}
            }finally{server.close();provider.join(1000);client.connectionPool().evictAll();client.dispatcher().executorService().shutdownNow();}
            assertFalse(provider.isAlive());assertNull(error.get());return calls.get();
        }
    }
}
