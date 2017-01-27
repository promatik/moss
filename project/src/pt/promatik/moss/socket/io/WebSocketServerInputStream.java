package pt.promatik.moss.socket.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import pt.promatik.moss.socket.http.HttpRequest;

public class WebSocketServerInputStream extends InputStream {
	public static final int EOF = -1;
	public static final int NULL = 0;
	public static final int PIPE = 124;
	public static final int CR = 13;
	public static final int LF = 10;
	public static final int HANDSHAKE_NONCE_LENGTH = 16;
	public static final int LENGTH_16 = 0x7E;
	public static final int LENGTH_16_MIN = 126;
	public static final int LENGTH_64 = 0x7F;
	public static final int LENGTH_64_MIN = 0x10000;
	public static final int MASK_BYTE = 0x000000FF;
	public static final int MASK_FINAL = 0x80;
	public static final int MASK_MASK = 0x80;
	public static final int MASK_MASKING_INDEX = 0x03;
	public static final int MASK_OPCODE = 0x0F;
	public static final int MASK_CONTROL_OPCODE = 0x08;
	public static final int MASK_PAYLOAD_SIZE = 0x7F;
	public static final int MASK_RESERVED = 0x70;
	public static final int NUM_MASKING_BYTES = 4;
	public static final int NUM_OCTET_64 = 8;
	public static final int OCTET = 8;
	public static final int OPCODE_CONTINUATION = 0x00;
	public static final int OPCODE_TEXT = 0x01;
	public static final int OPCODE_BINARY = 0x02;
	public static final int OPCODE_RESERVED_NON_CONTROL_LOW = 0x03;
	public static final int OPCODE_RESERVED_NON_CONTROL_HIGH = 0x07;
	public static final int OPCODE_CLOSE = 0x08;
	public static final int OPCODE_PING = 0x09;
	public static final int OPCODE_PONG = 0x0A;
	public static final int OPCODE_RESERVED_CONTROL_LOW = 0x0B;
	public static final int OPCODE_RESERVED_CONTROL_HIGH = 0x0F;
	public static final int OPCODE_CONTROL_LOW = 0x08;
	public static final int OPCODE_CONTROL_HIGH = 0x0F;
	public static final String UTF_8 = StandardCharsets.UTF_8.name();
	public static final String WEBSOCKET_ACCEPT_UUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

	private boolean lastWasCarriageReturn = false;
	private boolean closeReceived = false;
	private boolean failed = false;
	private boolean handshakeComplete = false;
	private boolean isWebSocket = false;
	private InputStream inputStream = null;
	private final int[] maskingBytes = new int[NUM_MASKING_BYTES];
	private int maskingIndex = 0;
	private WebSocketServerOutputStream outputPeer = null;
	private long payloadLength = 0L;
	private boolean haveFirstMessage = false;
	private String firstMessage = null;

	public static final int asUnsignedInt(final byte b) {
		int x = b;
		x &= MASK_BYTE;
		return x;
	}

	public static final byte[] asUTF8(final String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	public static boolean checkContains(final String s1, final String s2) {
		if (s1 == null) {
			return false;
		}
		return s1.contains(s2);
	}

	public static boolean checkStartsWith(final String s1, final String s2) {
		if (s1 == null) {
			return false;
		}
		return s1.startsWith(s2);
	}

	public static byte[] sha1(String input) {
		byte[] result = null;
		try {
			MessageDigest msg = MessageDigest.getInstance("SHA-1");
			msg.reset();
			msg.update(input.getBytes());
			result = msg.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static byte[] base64Encode(byte[] hash) {
		return Base64.getEncoder().encode(hash);
	}

	public static String base64EncodeToString(byte[] hash) {
		return Base64.getEncoder().encodeToString(hash);
	}

	public static byte[] base64Decode(byte[] hash) {
		return Base64.getDecoder().decode(hash);
	}
	
	public static <T> T checkNotNull(T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	public WebSocketServerInputStream(final InputStream is) {
		checkNotNull(is);
		this.inputStream = is;
	}

	public WebSocketServerInputStream(final InputStream is, final WebSocketServerOutputStream wsos) {
		checkNotNull(is);
		checkNotNull(wsos);
		this.inputStream = is;
		this.outputPeer = wsos;
	}

	public final OutputStream getOutputPeer() {
		return outputPeer;
	}

	private boolean isCloseSent() {
		return outputPeer.isCloseSent();
	}

	public final boolean isClosed() {
		return closeReceived && isCloseSent();
	}

	public final boolean isFailed() {
		return failed;
	}

	public final boolean isHandshakeComplete() {
		return handshakeComplete;
	}

	@Override
	public final int read() throws IOException {
		if (isClosed() || isFailed()) {
			return EOF;
		}
		if (!handshakeComplete) {
			firstMessage = shakeHands();
			haveFirstMessage = firstMessage != null;
			if (!handshakeComplete) {
				failTheWebSocketConnection();
				return EOF;
			}
		}
		if(haveFirstMessage) {
			char c = firstMessage.charAt(0);
			firstMessage = firstMessage.substring(1);
			if(firstMessage.length() == 0)
				haveFirstMessage = false;
			
			return c;
		}
		
		return nextWebSocketByte();
	}

	public final String readLine() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean inputTaken = false;
		while (true) {
			int data = inputStream.read();
			if (data == EOF) {
				return inputTaken ? baos.toString(UTF_8) : null;
			} else if (data == CR) {
				lastWasCarriageReturn = true;
			} else if (data == LF) {
				if (lastWasCarriageReturn) {
					lastWasCarriageReturn = false;
					return baos.toString(UTF_8);
				} else {
					inputTaken = true;
					lastWasCarriageReturn = false;
					baos.write(LF);
				}
			} else if (data == NULL || data == PIPE) {
				return baos.toString(UTF_8);
			} else {
				if (lastWasCarriageReturn) {
					baos.write(CR);
				}
				inputTaken = true;
				lastWasCarriageReturn = false;
				baos.write(data);
			}
		}
	}

	public final void setOutputPeer(final WebSocketServerOutputStream op) {
		this.outputPeer = op;
	}

	// -----------------------------------------------------------------------

	private int nextWebSocketByte() throws IOException {
		if(!isWebSocket)
			return inputStream.read();
			
		while (payloadLength == 0L) {
			nextWebSocketFrame();
			if (isClosed() || isFailed()) {
				return EOF;
			}
		}
		int data = inputStream.read() ^ maskingBytes[maskingIndex];
		payloadLength--;
		maskingIndex++;
		maskingIndex &= MASK_MASKING_INDEX;
		return data;
	}

	private void nextWebSocketFrame() throws IOException {
		int flagOps = inputStream.read();
		if ((flagOps & MASK_RESERVED) != 0x00) {
			failTheWebSocketConnection();
			return;
		}
		int opcode = flagOps & MASK_OPCODE;
		if (opcode >= OPCODE_RESERVED_NON_CONTROL_LOW && opcode <= OPCODE_RESERVED_NON_CONTROL_HIGH) {
			failTheWebSocketConnection();
			return;
		}
		if (opcode >= OPCODE_RESERVED_CONTROL_LOW) {
			failTheWebSocketConnection();
			return;
		}
		boolean finalFragment = (flagOps & MASK_FINAL) == MASK_FINAL;
		boolean controlOpcode = (flagOps & MASK_CONTROL_OPCODE) == MASK_CONTROL_OPCODE;
		if (controlOpcode && !finalFragment) {
			failTheWebSocketConnection();
			return;
		}
		int maskPayload = inputStream.read();
		boolean masked = (maskPayload & MASK_MASK) == MASK_MASK;
		if (!masked) {
			failTheWebSocketConnection();
			return;
		}
		int payloadSize = maskPayload & MASK_PAYLOAD_SIZE;
		if (payloadSize == LENGTH_16) {
			if (controlOpcode) {
				failTheWebSocketConnection();
				return;
			}
			payloadLength = (inputStream.read() << OCTET) | (inputStream.read());
			if (payloadLength < LENGTH_16_MIN) {
				failTheWebSocketConnection();
				return;
			}
		} else if (payloadSize == LENGTH_64) {
			if (controlOpcode) {
				failTheWebSocketConnection();
				return;
			}
			payloadLength = 0L;
			for (int i = 0; i < NUM_OCTET_64; i++) {
				payloadLength |= inputStream.read() << (NUM_OCTET_64 - 1 - i) * OCTET;
			}
			if (payloadLength < LENGTH_64_MIN) {
				failTheWebSocketConnection();
				return;
			}
		} else {
			payloadLength = payloadSize;
		}
		for (int i = 0; i < NUM_MASKING_BYTES; i++) {
			maskingBytes[i] = inputStream.read();
		}
		maskingIndex = 0;
		if (opcode == OPCODE_CLOSE) {
			handleCloseFrame();
		}
		if (opcode == OPCODE_PING) {
			handlePingFrame();
		}
		if (opcode == OPCODE_PONG) {
			handlePongFrame();
		}
	}

	private String shakeHands() throws IOException {
		HttpRequest req = new HttpRequest(inputStream);
		String requestLine = req.get(HttpRequest.REQUEST_LINE);
		handshakeComplete = checkStartsWith(requestLine, "GET /") && checkContains(requestLine, "HTTP/")
				&& req.get("Host") != null && checkContains(req.get("Upgrade"), "websocket")
				&& checkContains(req.get("Connection"), "Upgrade") && "13".equals(req.get("Sec-WebSocket-Version"))
				&& req.get("Sec-WebSocket-Key") != null;
		String nonce = req.get("Sec-WebSocket-Key");
		
		if (handshakeComplete) {
			isWebSocket = true;
			byte[] nonceBytes = base64Decode(nonce.getBytes(StandardCharsets.UTF_8));
			if (nonceBytes.length != HANDSHAKE_NONCE_LENGTH) {
				handshakeComplete = false;
			}
		} else {
			handshakeComplete = true;
		}
		
		if (handshakeComplete && isWebSocket) {
			byte[] hash = sha1(nonce + WEBSOCKET_ACCEPT_UUID);
			String acceptKey = base64EncodeToString(hash);

			outputPeer.write(asUTF8("HTTP/1.1 101 Switching Protocols\r\n"));
			outputPeer.write(asUTF8("Upgrade: websocket\r\n"));
			outputPeer.write(asUTF8("Connection: upgrade\r\n"));
			outputPeer.write(asUTF8("Sec-WebSocket-Accept: "));
			outputPeer.write(asUTF8(acceptKey));
			outputPeer.write(asUTF8("\r\n\r\n"));
			
			outputPeer.setHandshakeComplete(true);
		}
		
		return isWebSocket ? null : requestLine;
	}

	private void failTheWebSocketConnection() {
		failed = true;
	}

	private void handleCloseFrame() throws IOException {
		closeReceived = true;
		if (isCloseSent()) {
			this.close();
			return;
		}
		byte[] closePayload = consumePayload();
		if (closePayload.length >= 2) {
			int highByte = asUnsignedInt(closePayload[0]);
			int lowByte = asUnsignedInt(closePayload[1]);
			int closeStatusCode = (highByte << OCTET) | lowByte;
			outputPeer.writeClose(closeStatusCode);
		} else {
			outputPeer.writeClose();
		}
		this.close();
	}

	private void handlePingFrame() throws IOException {
		byte[] pingPayload = consumePayload();
		outputPeer.writePong(pingPayload);
	}

	private void handlePongFrame() throws IOException {
		consumePayload();
	}

	private byte[] consumePayload() throws IOException {
		byte[] payload = new byte[(int) payloadLength];
		int count = 0;
		while (payloadLength > 0L) {
			payload[count] = (byte) this.read();
			count++;
		}
		return payload;
	}
}
