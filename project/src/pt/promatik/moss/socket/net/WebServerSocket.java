package pt.promatik.moss.socket.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;

public class WebServerSocket extends ServerSocket {

	public WebServerSocket(int port, InetAddress bindAddress) throws IOException {
		super(port, 0, bindAddress);
	}
	
	@Override
	public final Socket accept() throws IOException {
		return new WebSocket(super.accept());
	}

	@Override
	public final ServerSocketChannel getChannel() {
		throw new UnsupportedOperationException();
	}
}
