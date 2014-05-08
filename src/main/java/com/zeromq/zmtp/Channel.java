package com.zeromq.zmtp;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Channel {
    public boolean negotiate() throws IOException;

    public boolean send(ByteBuffer buf, int position, int length)
            throws IOException;

    public boolean receive(ByteBuffer out, int position, int length)
            throws IOException;
}
