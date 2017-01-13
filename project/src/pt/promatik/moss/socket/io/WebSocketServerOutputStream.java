package pt.promatik.moss.socket.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WebSocketServerOutputStream extends OutputStream {
	public static final int LENGTH_16 = 0x7E;
	public static final int LENGTH_16_MIN = 126;
	public static final int LENGTH_64 = 0x7F;
	public static final int LENGTH_64_MIN = 0x10000;
	public static final int MASK_HIGH_WORD_HIGH_BYTE_NO_SIGN = 0x7f000000;
	public static final int MASK_HIGH_WORD_LOW_BYTE = 0x00ff0000;
	public static final int MASK_LOW_WORD_HIGH_BYTE = 0x0000ff00;
	public static final int MASK_LOW_WORD_LOW_BYTE = 0x000000ff;
	public static final int OCTET_ONE = 8;
	public static final int OCTET_TWO = 16;
	public static final int OCTET_THREE = 24;
	public static final int OPCODE_FRAME_BINARY = 0x82;
	public static final int OPCODE_FRAME_CLOSE = 0x88;
	public static final int OPCODE_FRAME_PONG = 0x8A;
	public static final int OPCODE_FRAME_TEXT = 0x81;

	private boolean closeSent = false;
	private boolean handshakeComplete = false;
	private final OutputStream outputStream;
	
	public static <T> T checkNotNull(T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	public WebSocketServerOutputStream(final OutputStream os) {
		if (os == null) throw new NullPointerException();
		this.outputStream = os;
	}

	@Override
	public final void write(final int b) throws IOException {
		if (handshakeComplete) {
			byte[] ba = new byte[] { (byte) b };
			writeBinary(ba);
		} else {
			outputStream.write(b);
		}
	}

	@Override
	public final void write(final byte[] b, final int off, final int len) throws IOException {
		if (handshakeComplete) {
			byte[] dst = new byte[len];
			System.arraycopy(b, off, dst, 0, len);
			writeBinary(dst);
		} else {
			super.write(b, off, len);
		}
	}

	@Override
	public final void write(final byte[] b) throws IOException {
		if (handshakeComplete) {
			writeBinary(b);
		} else {
			super.write(b);
		}
	}

	public final boolean isCloseSent() {
		return closeSent;
	}

	public final boolean isHandshakeComplete() {
		return handshakeComplete;
	}

	public final void setHandshakeComplete(final boolean complete) {
		this.handshakeComplete = complete;
	}

	public final void writeBinary(final byte[] bytes) throws IOException {
		writeString(new String(bytes, StandardCharsets.UTF_8));
	}

	public final void writeClose() throws IOException {
		if (!closeSent) {
			closeSent = true;
			outputStream.write(new byte[] { (byte) OPCODE_FRAME_CLOSE, (byte) 0x00 });
		}
	}

	public final void writeClose(final int statusCode) throws IOException {
		if (!closeSent) {
			closeSent = true;
			outputStream.write(new byte[] { (byte) OPCODE_FRAME_CLOSE, (byte) 0x02,
					(byte) ((statusCode & MASK_LOW_WORD_HIGH_BYTE) >> OCTET_ONE),
					(byte) (statusCode & MASK_LOW_WORD_LOW_BYTE) });
		}
	}

	public final void writePong(final byte[] pongPayload) throws IOException {
		outputStream.write(new byte[] { (byte) OPCODE_FRAME_PONG, (byte) (pongPayload.length) });
		outputStream.write(pongPayload);
	}

	public final void writeString(final String string) throws IOException {
		byte[] utfBytes = string.getBytes(StandardCharsets.UTF_8);
		int utfLength = utfBytes.length;
		outputStream.write(OPCODE_FRAME_TEXT);
		if (utfLength < LENGTH_16_MIN) {
			outputStream.write(utfLength);
		} else if (utfLength < LENGTH_64_MIN) {
			outputStream.write(LENGTH_16);
			outputStream.write((utfLength & MASK_LOW_WORD_HIGH_BYTE) >> OCTET_ONE);
			outputStream.write(utfLength & MASK_LOW_WORD_LOW_BYTE);
		} else {
			outputStream.write(LENGTH_64);
			outputStream.write(new byte[]{0,0,0,0});
			outputStream.write((utfLength & MASK_HIGH_WORD_HIGH_BYTE_NO_SIGN) >> OCTET_THREE);
			outputStream.write((utfLength & MASK_HIGH_WORD_LOW_BYTE) >> OCTET_TWO);
			outputStream.write((utfLength & MASK_LOW_WORD_HIGH_BYTE) >> OCTET_ONE);
			outputStream.write(utfLength & MASK_LOW_WORD_LOW_BYTE);
		}
		outputStream.write(utfBytes);
	}
}
