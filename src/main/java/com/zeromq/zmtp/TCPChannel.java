package com.zeromq.zmtp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import com.zeromq.io.DirectBuffer;

public class TCPChannel implements Channel, Closeable {
    private final SocketChannel channel;

    public TCPChannel(String host, int port) throws IOException {
        this.channel = SocketChannel.open();
        this.channel.connect(new InetSocketAddress(host, port));
    }

    @Override
    public boolean negotiate() throws IOException {
        ByteBuffer signature = ByteBuffer.wrap(new byte[] { (byte) 0xff, 0, 0, 0, 0, 0, 0, 0, 1, 0x7f });
        ByteBuffer mechanism = ByteBuffer.wrap(new byte[] { 'N', 'U', 'L', 'L', '\0', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0 });
        ByteBuffer filler = ByteBuffer.allocate(31);

        channel.write(signature);
        DirectBuffer incoming = new DirectBuffer(ByteBuffer.allocate(10));
        channel.read(incoming.byteBuffer());
        if (incoming.getByte(0) != 0xff)
            return false;
        if ((incoming.getByte(9) & 1) != 1)
            return false;

        channel.write(ByteBuffer.wrap(new byte[] { '3' }));
        ByteBuffer b = ByteBuffer.allocate(1);
        channel.read(b);
        assert b.get(0) == '3';

        channel.write(ByteBuffer.wrap(new byte[] { '0' }));
        b.clear();
        channel.read(b);
        assert b.get(0) == '0';

        channel.write(mechanism);
        channel.write(ByteBuffer.wrap(new byte[] { '0' }));
        channel.write(filler);

        channel.read(ByteBuffer.allocate(20));
        channel.read(ByteBuffer.allocate(1));
        channel.read(ByteBuffer.allocate(31));

        zmtpSend(channel, "READY\0");

        zmtpRecv(channel);

        return true;
    }

    // TODO: Temp solution
    public static boolean zmtpSend(SocketChannel channel, String str) throws IOException {
        byte frameFlags = Message.ZMTP_COMMAND_FLAG;
        ByteBuffer bb = ByteBuffer.wrap(str.getBytes(Charset.forName("UTF-8")));
        if (bb.limit() > 255) {
            frameFlags |= Message.ZMTP_LARGE_FLAG;
        }
        ByteBuffer flags = ByteBuffer.allocate(1).put(frameFlags);
        flags.flip();
        channel.write(flags);
        if (bb.limit() <= 255) {
            ByteBuffer size = ByteBuffer.allocate(1).put((byte) bb.limit());
            size.flip();
            channel.write(size);
        } else {
            ByteBuffer size = ByteBuffer.allocate(8).putLong(bb.limit());
            size.flip();
            channel.write(size);
        }
        channel.write(bb);
        return true;
    }

    public static ByteBuffer zmtpRecv(SocketChannel channel) throws IOException {
        ByteBuffer flags = ByteBuffer.allocate(1);
        channel.read(flags);
        long size = -1;
        if ((flags.get(0) & Message.ZMTP_LARGE_FLAG) == 0) {
            ByteBuffer s = ByteBuffer.allocate(1);
            channel.read(s);
            size = s.get(0);
        } else {
            ByteBuffer s = ByteBuffer.allocate(8);
            channel.read(s);
            size = s.getLong(0);
        }
        ByteBuffer data = ByteBuffer.allocate((int) size);
        channel.read(data);
        data.flip();
        return data;
    }

    @Override
    public boolean send(ByteBuffer buf, int position, int length) {
        return false;
    }

    @Override
    public boolean receive(ByteBuffer out, int position, int length) {
        return false;
    }

    @Override
    public void close() throws IOException {
        this.channel.close();
    }
}
