package com.akdeniz.googleplaycrawler.gsf;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.BindAccountResponse;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.Close;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.DataMessageStanza;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.HeartbeatAck;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.IQStanza;
import com.akdeniz.googleplaycrawler.gsf.GoogleServicesFramework.LoginResponse;
import com.akdeniz.googleplaycrawler.gsf.packets.Packet;
import com.akdeniz.googleplaycrawler.gsf.packets.UnknownResponse;
/**
 * Protocol codec factory for MINA to encode & decode related packets.
 * 
 * @author akdeniz
 *
 */
public class GSFCodecFactory implements ProtocolCodecFactory {

    private ProtocolEncoder protocolEncoder;
    private ProtocolDecoder protocolDecoder;

    public GSFCodecFactory() {
	protocolEncoder = new GSFProtocolEncoder();
	protocolDecoder = new GSFRequestDecoder();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
	return protocolEncoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
	return protocolDecoder;
    }

}

class GSFRequestDecoder extends CumulativeProtocolDecoder {

    @Override
    protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {

	ByteBuffer buffer = in.buf();
	buffer.mark();
	boolean success = parse(buffer, out);
	if (!success) {
	    buffer.reset();
	    return false;
	}
	return true;

    }
    
    public static boolean parse(ByteBuffer buffer, ProtocolDecoderOutput out) throws IOException {

	if (buffer.remaining() == 1) { // one byte sanity check
	    byte[] data = new byte[1];
	    buffer.get(data);
	    out.write(new UnknownResponse(0, 0, data));
	    return true;
	}

	if (buffer.remaining() < 2) {
	    return false;
	}

	byte tag = buffer.get();
	int length = Packet.unlength(buffer);

	if (buffer.remaining() < length) {
	    return false;
	}
	byte[] data = new byte[length];
	buffer.get(data);

	switch (tag) {
	case 0x01:
	    out.write(HeartbeatAck.parseFrom(data));
	    return true;
	case 0x03:
	    out.write(LoginResponse.parseFrom(data));
	    return true;
	case 0x04:
	    out.write(Close.parseFrom(data));
	    return true;
	case 0x07:
	    out.write(IQStanza.parseFrom(data));
	    return true;
	case 0x08:
	    out.write(DataMessageStanza.parseFrom(data));
	    return true;
	case 0x0E:
	    out.write(BindAccountResponse.parseFrom(data));
	    return true;
	default:
	    out.write(new UnknownResponse(tag, length, data));
	    return true;
	}
    }
}

class GSFProtocolEncoder implements ProtocolEncoder {

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {

	if (message instanceof Packet) {
	    Packet packet = (Packet) message;
	    out.write(IoBuffer.wrap(packet.getBytes()));
	} else {
	    out.write(message);
	}
    }

    @Override
    public void dispose(IoSession session) throws Exception {
    }
}
