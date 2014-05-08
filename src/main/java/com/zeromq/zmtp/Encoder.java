package com.zeromq.zmtp;

import java.nio.ByteBuffer;

public interface Encoder {
    public void encode(ByteBuffer bb, int position, int length);
}
