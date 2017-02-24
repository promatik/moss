package pt.promatik.moss.socket.net;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.promatik.moss.socket.io.WebSocketServerInputStream;
import pt.promatik.moss.socket.io.WebSocketServerOutputStream;

public class WebSocket extends Socket {
	private final Socket socket;
	private final Pattern pattern = Pattern.compile("addr=/([0-9\\.]+),port=([0-9]+)");
	private WebSocketServerInputStream wssis = null;
	private WebSocketServerOutputStream wssos = null;

	public WebSocket(final Socket s) {
		this.socket = s;
	}

	@Override
	public final WebSocketServerInputStream getInputStream() throws IOException {
		if (wssos == null) {
			this.getOutputStream();
		}
		if (wssis == null) {
			wssis = new WebSocketServerInputStream(socket.getInputStream(), wssos);
		}
		return wssis;
	}

	@Override
	public final WebSocketServerOutputStream getOutputStream() throws IOException {
		if (wssos == null) {
			wssos = new WebSocketServerOutputStream(socket.getOutputStream());
		}
		return wssos;
	}

	@Override
	public final void setTcpNoDelay(final boolean on) throws SocketException {
		socket.setTcpNoDelay(on);
	}

	@Override
	public final boolean getTcpNoDelay() throws SocketException {
		return socket.getTcpNoDelay();
	}

	@Override
	public final void setSoLinger(final boolean on, final int linger) throws SocketException {
		socket.setSoLinger(on, linger);
	}

	@Override
	public final int getSoLinger() throws SocketException {
		return socket.getSoLinger();
	}

	@Override
	public final void sendUrgentData(final int data) throws IOException {
		socket.sendUrgentData(data);
	}

	@Override
	public final void setOOBInline(final boolean on) throws SocketException {
		socket.setOOBInline(on);
	}

	@Override
	public final boolean getOOBInline() throws SocketException {
		return socket.getOOBInline();
	}

	@Override
	public final synchronized void setSoTimeout(final int timeout) throws SocketException {
		socket.setSoTimeout(timeout);
	}

	@Override
	public final synchronized int getSoTimeout() throws SocketException {
		return socket.getSoTimeout();
	}

	@Override
	public final synchronized void setSendBufferSize(final int size) throws SocketException {
		socket.setSendBufferSize(size);
	}

	@Override
	public final synchronized int getSendBufferSize() throws SocketException {
		return socket.getSendBufferSize();
	}

	@Override
	public final synchronized void setReceiveBufferSize(final int size) throws SocketException {
		socket.setReceiveBufferSize(size);
	}

	@Override
	public final synchronized int getReceiveBufferSize() throws SocketException {
		return socket.getReceiveBufferSize();
	}

	@Override
	public final void setKeepAlive(final boolean on) throws SocketException {
		socket.setKeepAlive(on);
	}

	@Override
	public final boolean getKeepAlive() throws SocketException {
		return socket.getKeepAlive();
	}

	@Override
	public final void setTrafficClass(final int tc) throws SocketException {
		socket.setTrafficClass(tc);
	}

	@Override
	public final int getTrafficClass() throws SocketException {
		return socket.getTrafficClass();
	}

	@Override
	public final void setReuseAddress(final boolean on) throws SocketException {
		socket.setReuseAddress(on);
	}

	@Override
	public final boolean getReuseAddress() throws SocketException {
		return socket.getReuseAddress();
	}

	@Override
	public final synchronized void close() throws IOException {
		socket.close();
	}

	@Override
	public final void shutdownInput() throws IOException {
		socket.shutdownInput();
	}

	@Override
	public final void shutdownOutput() throws IOException {
		socket.shutdownOutput();
	}

	@Override
	public final String toString() {
		String s = socket.toString();
		Matcher matcher = pattern.matcher(s);
		return matcher.find() ? (matcher.group(1) + ":" + matcher.group(2)) : s;
	}

	@Override
	public final boolean isConnected() {
		return socket.isConnected();
	}

	@Override
	public final boolean isBound() {
		return socket.isBound();
	}

	@Override
	public final boolean isClosed() {
		return socket.isClosed();
	}

	@Override
	public final boolean isInputShutdown() {
		return socket.isInputShutdown();
	}

	@Override
	public final boolean isOutputShutdown() {
		return socket.isOutputShutdown();
	}

	@Override
	public final void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
		socket.setPerformancePreferences(connectionTime, latency, bandwidth);
	}
}
