package com.zeromq.zmtp;

import java.nio.ByteBuffer;

public interface Decoder {
	public ByteBuffer decode(ByteBuffer bb, int position, int length);
}
