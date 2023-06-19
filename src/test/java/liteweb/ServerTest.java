package liteweb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServerTest {

    @ParameterizedTest
    @CsvSource({", 8080", "1234, 1234", "8080, 8080"})
    void shouldReturnTrue_whenValid(String value, int port) {
        String[] args = value == null ? new String[]{} : new String[]{value};
        assertEquals(port, Server.getValidPortParam(args));
    }

    @ParameterizedTest
    @CsvSource({"asda", "0", "65535"})
    void wrongParamThrowException(String value) {
        String[] args = {value};
        ;
        assertThrows(NumberFormatException.class, () -> {
            Server.getValidPortParam(args);
        });
    }

//    @Test
//    void handleSelectionKeyIfAcceptable() throws IOException {
//
//        Selector mockedSelector = new Selector() {
//            @Override
//            public boolean isOpen() {
//                return false;
//            }
//
//            @Override
//            public SelectorProvider provider() {
//                return null;
//            }
//
//            @Override
//            public Set<SelectionKey> keys() {
//                return null;
//            }
//
//            @Override
//            public Set<SelectionKey> selectedKeys() {
//                return null;
//            }
//
//            @Override
//            public int selectNow() throws IOException {
//                return 0;
//            }
//
//            @Override
//            public int select(long l) throws IOException {
//                return 0;
//            }
//
//            @Override
//            public int select() throws IOException {
//                return 0;
//            }
//
//            @Override
//            public Selector wakeup() {
//                return null;
//            }
//
//            @Override
//            public void close() throws IOException {
//
//            }
//        };
//        ServerSocketChannel mockedChannel = ServerSocketChannel.open();
//        mockedChannel.bind(new InetSocketAddress(2222));
//        SelectionKey mockedSelectionKey = new SelectionKey() {
//            @Override
//            public SelectableChannel channel() {
//                return mockedChannel;
//            }
//
//            @Override
//            public Selector selector() {
//                return null;
//            }
//
//            @Override
//            public boolean isValid() {
//                return false;
//            }
//
//            @Override
//            public void cancel() {
//
//            }
//
//            @Override
//            public int interestOps() {
//                return 0;
//            }
//
//            @Override
//            public SelectionKey interestOps(int i) {
//                return null;
//            }
//
//            @Override
//            public int readyOps() {
//                return 16;
//            }
//        };
//        Server.handleSelectionKeyIfAcceptable(mockedSelector, mockedSelectionKey);
//        Assertions.assertTrue(mockedChannel.isRegistered());
//        mockedChannel.close();
//    }

//    @Test
//    void handleSelectionKeyIfReadable() throws IOException {
//        SelectionKey selectionKey = Mockito.mock(SelectionKey.class);
//        Mockito.doReturn(true).when(selectionKey).isReadable();
//        Server.handleSelectionKeyIfReadable(selectionKey);
//        Mockito.verify()
//    }
}
