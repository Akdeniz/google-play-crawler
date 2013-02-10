package com.akdeniz.googleplaycrawler.gsf.packets;

import java.nio.ByteBuffer;

import com.akdeniz.googleplaycrawler.gsf.Gsf.Extension;
import com.akdeniz.googleplaycrawler.gsf.Gsf.IQStanza;

/**
 * This packets notifies that server notification stream has received and used
 * successfully. If this is not sent for every server notification, servers
 * sends all notification at every connect.
 * 
 * @author akdeniz
 * 
 */
public class NotificationPacket extends Packet {

    private static byte[] PACKET_TYPE = { 0x07 };

    private final String id;
    private IQStanza stanza;

    public NotificationPacket(int lastStreamId) {
	id = nextID();

	stanza = IQStanza.newBuilder().setType(1).setPacketid(id)
		.setExtension(Extension.newBuilder().setCode(10).setMessage("\b\001\020\001")).setLaststreamid(lastStreamId)
		.setAccountid(1000000).build();
    }

    public ByteBuffer getBytes() {

	byte[] requestBytes = stanza.toByteArray();
	ByteBuffer buffer = ByteBuffer.allocate(0xFF);

	buffer.put(PACKET_TYPE);
	buffer.put(length(requestBytes.length));
	buffer.put(requestBytes);
	buffer.flip();

	return buffer;
    }

    @Override
    public String getPacketID() {
	return id;
    }
}
