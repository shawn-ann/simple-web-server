package liteweb;

import liteweb.http.Request;
import liteweb.http.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class NIOServer {

    private static final Logger log = LogManager.getLogger(NIOServer.class);
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException, InterruptedException {
        // 238619
        new NIOServer().startListen(getValidPortParam(args));
    }


    public void startListen(int port) throws IOException, InterruptedException {
        try (ServerSocketChannel socketChannel = ServerSocketChannel.open(); Selector selector = Selector.open()) {
            socketChannel.configureBlocking(false);
            socketChannel.bind(new InetSocketAddress(port), 60);
            socketChannel.register(selector, SelectionKey.OP_ACCEPT);

            log.info("Web server listening on port %d (press CTRL-C to quit)", port);
            while (true) {
                // 轮询Selector上的事件
                int select = selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                while (selectionKeyIterator.hasNext()) {
                    SelectionKey selectionKey = selectionKeyIterator.next();
                    handleSelectionKeyIfAcceptable(selector, selectionKey);
                    handleSelectionKeyIfReadable(selectionKey);
                    selectionKeyIterator.remove();
                }
            }
        }
    }

    /**
     * Parse command line arguments (string[] args) for valid port number
     *
     * @return int valid port number or default value (8080)
     */
    static int getValidPortParam(String[] args) throws NumberFormatException {
        if (args.length > 0) {
            int port = Integer.parseInt(args[0]);
            if (port > 0 && port < 65535) {
                return port;
            } else {
                throw new NumberFormatException("Invalid port! Port value is a number between 0 and 65535");
            }
        }
        return DEFAULT_PORT;
    }

    static void handleSelectionKeyIfAcceptable(Selector selector, SelectionKey selectionKey) throws IOException {
        if (selectionKey.isAcceptable()) {
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    static void handleSelectionKeyIfReadable(SelectionKey selectionKey) throws IOException {
        if (selectionKey.isReadable()) {
            SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
            Request request = getRequest(selectionKey, socketChannel);
            if (request == null){
                System.out.println("User cancel the request");
                socketChannel.close();
                selectionKey.cancel();
                return;
            }
            System.out.println("request uri is " + request.getUri());
            Response response = new Response(request);
            response.write(socketChannel);
            selectionKey.cancel();
        }
    }

    private static Request getRequest(SelectionKey selectionKey, SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        // 读取请求数据
        int readBytes = 0;
        while ((readBytes = socketChannel.read(byteBuffer)) > 0 || byteBuffer.position() == 0) {
            // 判断 ByteBuffer 容量是否足够
            if (!byteBuffer.hasRemaining()) {
                // 扩容 ByteBuffer
                ByteBuffer newBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2);
                byteBuffer.flip();
                newBuffer.put(byteBuffer);
                byteBuffer = newBuffer;
            }
        }
        if (readBytes == -1) {
            return null;
        }
        byteBuffer.flip();
        byte[] requestData = new byte[byteBuffer.remaining()];
        byteBuffer.get(requestData);
        String requestContent = new String(requestData, ServerConfig.CHARSET).trim();
        return new Request(Arrays.asList(requestContent.split("\r\n")));
    }
}
