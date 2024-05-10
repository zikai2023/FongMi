package com.fongmi.android.tv.server;

import android.text.TextUtils;
import android.util.Base64;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Device;
import com.fongmi.android.tv.server.process.Action;
import com.fongmi.android.tv.server.process.Cache;
import com.fongmi.android.tv.server.process.Local;
import com.fongmi.android.tv.server.process.Process;
import com.fongmi.android.tv.utils.M3U8;
import com.github.catvod.utils.Asset;
import com.github.tvbox.osc.event.InputMsgEvent;
import com.github.tvbox.osc.event.LogEvent;
import com.google.common.net.HttpHeaders;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class Nano extends NanoHTTPD {

    private List<Process> process;
    private final static char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();


    private final AtomicReference<NanoWSD.WebSocket> wsReference = new AtomicReference<>();
    private Timer timer;
    private Timer getTimer(){

        if(timer == null){
            timer = new Timer(true);
        }
        return timer;
    }

    public Nano(int port) {
        super(port);
        EventBus.getDefault().register(this);
        addProcess();
    }

    private void addProcess() {
        process = new ArrayList<>();
        process.add(new Action());
        process.add(new Cache());
        process.add(new Local());
    }

    private boolean isWebSocketConnectionHeader(Map<String, String> headers) {
        String connection = headers.get("connection");
        return connection != null && connection.toLowerCase().contains("Upgrade".toLowerCase());
    }

    private boolean isWebsocketRequested(IHTTPSession session) {
        Map<String, String> headers = session.getHeaders();
        String upgrade = headers.get("upgrade");
        boolean isCorrectConnection = isWebSocketConnectionHeader(headers);
        boolean isUpgrade = "websocket".equalsIgnoreCase(upgrade);
        return isUpgrade && isCorrectConnection;
    }

    private static String encodeBase64(byte[] buf) {
        int size = buf.length;
        char[] ar = new char[(size + 2) / 3 * 4];
        int a = 0;
        int i = 0;
        while (i < size) {
            byte b0 = buf[i++];
            byte b1 = i < size ? buf[i++] : 0;
            byte b2 = i < size ? buf[i++] : 0;

            int mask = 0x3F;
            ar[a++] = ALPHABET[b0 >> 2 & mask];
            ar[a++] = ALPHABET[(b0 << 4 | (b1 & 0xFF) >> 4) & mask];
            ar[a++] = ALPHABET[(b1 << 2 | (b2 & 0xFF) >> 6) & mask];
            ar[a++] = ALPHABET[b2 & mask];
        }
        switch (size % 3) {
            case 1:
                ar[--a] = '=';
            case 2:
                ar[--a] = '=';
        }
        return new String(ar);
    }

    public static String makeAcceptKey(String key) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        md.update(text.getBytes(), 0, text.length());
        byte[] sha1hash = md.digest();
        return encodeBase64(sha1hash);
    }

    public static Response success() {
        return success("OK");
    }

    public static Response success(String text) {
        return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, text);
    }

    public static Response error(String text) {
        return error(Response.Status.INTERNAL_ERROR, text);
    }

    public static Response error(Response.IStatus status, String text) {
        return newFixedLengthResponse(status, MIME_PLAINTEXT, text);
    }

    public static Response redirect(String url, Map<String, String> headers) {
        Response response = newFixedLengthResponse(Response.Status.REDIRECT, MIME_HTML, "");
        for (Map.Entry<String, String> entry : headers.entrySet()) response.addHeader(entry.getKey(), entry.getValue());
        response.addHeader(HttpHeaders.LOCATION, url);
        return response;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (isWebsocketRequested(session)) {
            Map<String, String> headers = session.getHeaders();
            if (!"13".equalsIgnoreCase(headers.get("sec-websocket-version"))) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT,
                        "Invalid Websocket-Version " + headers.get("sec-websocket-version"));
            }
            if (!headers.containsKey("sec-websocket-key")) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing Websocket-Key");
            }
            NanoWSD.WebSocket webSocket = new DebugWebSocket(session);
            Response handshakeResponse = webSocket.getHandshakeResponse();
            try {
                handshakeResponse.addHeader("sec-websocket-accept", makeAcceptKey(headers.get("sec-websocket-key")));
            } catch (NoSuchAlgorithmException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
                        "The SHA-1 Algorithm required for websockets is not available on the server.");
            }
            if (headers.containsKey("sec-websocket-protocol")) {
                handshakeResponse.addHeader("sec-websocket-protocol", headers.get("sec-websocket-protocol").split(",")[0]);
            }
            return handshakeResponse;
        }

        String url = session.getUri().trim();
        Map<String, String> files = new HashMap<>();
        if (session.getMethod() == Method.POST) parse(session, files);
        if (url.contains("?")) url = url.substring(0, url.indexOf('?'));
        if (url.startsWith("/m3u8")) return m3u8(session);
        if (url.startsWith("/proxy")) return proxy(session);
        if (url.startsWith("/tvbus")) return success(LiveConfig.getResp());
        if (url.startsWith("/device")) return success(Device.get().toString());
        if (url.startsWith("/license")) return success(new String(Base64.decode(url.substring(9), Base64.DEFAULT)));
        for (Process process : process) if (process.isRequest(session, url)) return process.doResponse(session, url, files);
        return getAssets(url.substring(1));
    }

    private void parse(IHTTPSession session, Map<String, String> files) {
        String ct = session.getHeaders().get("content-type");
        if (ct != null && ct.toLowerCase().contains("multipart/form-data") && !ct.toLowerCase().contains("charset=")) {
            Matcher matcher = Pattern.compile("[ |\t]*(boundary[ |\t]*=[ |\t]*['|\"]?[^\"^'^;^,]*['|\"]?)", Pattern.CASE_INSENSITIVE).matcher(ct);
            String boundary = matcher.find() ? matcher.group(1) : null;
            if (boundary != null) session.getHeaders().put("content-type", "multipart/form-data; charset=utf-8; " + boundary);
        }
        try {
            session.parseBody(files);
        } catch (Exception e) {
            com.github.tvbox.osc.util.LOG.e(e);
        }
    }

    private Response m3u8(IHTTPSession session) {
        String url = session.getParms().get("url");
        String result = M3U8.get(url, session.getHeaders());
        if (result.isEmpty()) return redirect(url, session.getHeaders());
        return newChunkedResponse(Response.Status.OK, MIME_PLAINTEXT, new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
    }

    private Response proxy(IHTTPSession session) {
        try {
            Map<String, String> params = session.getParms();
            params.putAll(session.getHeaders());
            Object[] rs = VodConfig.get().proxyLocal(params);
            int code = (Integer) rs[0];
            String mime = (String) rs[1];
            InputStream stream = rs[2] != null ? (InputStream) rs[2] : null;

            Response r = newChunkedResponse(Response.Status.lookup(code), mime, stream);
            if (rs.length > 3 && rs[3] != null) {
                for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) rs[3]).entrySet()) {
                    r.addHeader(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            return rs[0] instanceof Response ? (Response) rs[0] : r;
            //return rs[0] instanceof Response ? (Response) rs[0] : newChunkedResponse(Response.Status.lookup((Integer) rs[0]), (String) rs[1], (InputStream) rs[2]);
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private Response getAssets(String path) {
        try {
            if (path.isEmpty()) path = "index.html";
            InputStream is = Asset.open(path);
            return newFixedLengthResponse(Response.Status.OK, getMimeTypeForFile(path), is, is.available());
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_HTML, null);
        }
    }
    @Override
    public void start(int timeout, boolean daemon) throws IOException {
        super.start(timeout, daemon);
        getTimer().schedule(new TimerTask() {

            @Override
            public void run() {
                if (isAlive()){
                    synchronized (wsReference) {
                        try{
                            NanoWSD.WebSocket wsSocket = wsReference.get();
                            if(wsSocket != null && wsSocket.isOpen()){
                                wsSocket.ping(" ".getBytes());
                            }
                        }catch (Throwable e) {
                            com.github.tvbox.osc.util.LOG.e(e);
                        }
                    }
                }
            }
        }, 0, timeout*3L/4);
    }
    @Override
    public void stop() {
        super.stop();
        EventBus.getDefault().unregister(this);
    }

    private class DebugWebSocket extends NanoWSD.WebSocket {

        public DebugWebSocket(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            synchronized (wsReference) {
                wsReference.set(this);
            }
        }

        @Override
        protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            try {
                send("服务关闭");
            } catch (IOException e) {
                com.github.tvbox.osc.util.LOG.e(e);
            }
        }

        @Override
        protected void onMessage(NanoWSD.WebSocketFrame message) {
            String tag = message.getTextPayload();
            if (TextUtils.isEmpty(tag)) {
                try {
                    send("输入不能为空");
                } catch (IOException e) {
                    com.github.tvbox.osc.util.LOG.e(e);
                }
            } else {
                try {
                    send(tag);
                    EventBus.getDefault().post(new InputMsgEvent(tag));
                } catch (IOException e) {
                    com.github.tvbox.osc.util.LOG.e(e);
                }
            }
        }

        @Override
        protected void onPong(NanoWSD.WebSocketFrame pong) {

        }

        @Override
        protected void onException(IOException exception) {
            try {
                send(exception.getMessage());
            } catch (IOException e) {
                com.github.tvbox.osc.util.LOG.e(e);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onReceiveEvent(LogEvent logEvent) {
        if(logEvent != null) {
            if (isAlive()){
                synchronized (wsReference) {
                    try{
                        NanoWSD.WebSocket wsSocket = wsReference.get();
                        if(wsSocket != null && wsSocket.isOpen()){
                            wsSocket.send(logEvent.getText());
                        }
                    } catch (Throwable e) {
                        com.github.tvbox.osc.util.LOG.e(e);
                    }
                }
            }

        }
    }
}
