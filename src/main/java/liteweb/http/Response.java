package liteweb.http;

import liteweb.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Response {

    private static final Logger log = LogManager.getLogger(Response.class);

    public static final String VERSION = "HTTP/1.0";

    private final List<String> headers = new ArrayList<>();

    private ByteBuffer body;

    public List<String> getHeaders() {
        return new ArrayList<>(headers);
    }

    public Response(Request req) {

        switch (req.getMethod()) {
            case HEAD:
                fillHeaders(Status._200);
                break;
            case GET:
                try {
                    // TODO fix dir bug http://localhost:8080/src/test
                    String uri = req.getUri();
                    File file = new File("." + uri);
                    if (file.isDirectory()) {
                        generateResponseForFolder(uri, file);
                    } else if (file.exists()) {
                        fillHeaders(Status._200);
                        setContentType(uri);
                        FileChannel fileChannel = FileChannel.open(Paths.get("." + uri), StandardOpenOption.READ);
                        fillResponse(fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size()));
                    } else {
                        log.info("File not found: %s", req.getUri());
                        fillHeaders(Status._404);

                        fillResponse(Status._404.toString());
                    }
                } catch (IOException e) {
                    log.error("Response Error", e);
                    fillHeaders(Status._400);
                    fillResponse(Status._400.toString());
                }
                break;
            default:
                fillHeaders(Status._400);
                fillResponse(Status._400.toString());
        }

    }

    private void generateResponseForFolder(String uri, File file) {
        fillHeaders(Status._200);

        headers.add(ContentType.of("HTML"));
        StringBuilder result = new StringBuilder("<html><head><title>Index of ");
        result.append(uri);
        result.append("</title></head><body><h1>Index of ");
        result.append(uri);
        result.append("</h1><hr><pre>");

        // TODO add Parent Directory
        File[] files = file.listFiles();
        for (File subFile : files) {
            result.append(" <a href=\"" + subFile.getPath() + "\">" + subFile.getPath() + "</a>\n");
        }
        result.append("<hr></pre></body></html>");
        fillResponse(result.toString());
    }

    private byte[] getBytes(File file) throws IOException {
        int length = (int) file.length();
        byte[] array = new byte[length];
        try (InputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < length) {
                int count = in.read(array, offset, (length - offset));
                offset += count;
            }
        }
        return array;
    }

    private void fillHeaders(Status status) {
        headers.add(Response.VERSION + " " + status.toString());
        headers.add("Connection: close");
        headers.add("Server: simple-web-server");
    }
    private void fillResponse(String response) {
        body = ByteBuffer.wrap(response.getBytes());
    }
    private void fillResponse(ByteBuffer byteBuffer) {
        body = byteBuffer;
    }

    public void write(SocketChannel socketChannel) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        try (SocketChannel closeableSocketChannel = socketChannel) {
            for (String header : headers) {
                byteBuffer.put(ServerConfig.CHARSET.encode(header + "\r\n"));
            }
            byteBuffer.put(ServerConfig.CHARSET.encode("\r\n"));
            byteBuffer.flip();
            closeableSocketChannel.write(byteBuffer);
            byteBuffer.clear();
            if (body != null) {
                closeableSocketChannel.write(body);
            }
            byteBuffer.put(ServerConfig.CHARSET.encode("\r\n"));
            byteBuffer.flip();
            closeableSocketChannel.write(byteBuffer);
            System.out.println("write response");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            byteBuffer.clear();
        }
    }

    private void setContentType(String uri) {
        try {
            String ext = uri.substring(uri.indexOf(".") + 1);
            headers.add(ContentType.of(ext));
        } catch (RuntimeException e) {
            log.error("ContentType not found:", e);
        }
    }
}
