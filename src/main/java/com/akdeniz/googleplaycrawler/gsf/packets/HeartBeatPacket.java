package com.akdeniz.googleplaycrawler.gsf.packets;

import java.nio.ByteBuffer;

/**
 * 
 * @author akdeniz
 *
 */
public class HeartBeatPacket extends Packet {

    private static byte[] PACKET_TYPE = {0x00};
    
    public HeartBeatPacket() {
    }

    public ByteBuffer getBytes() {
	
	ByteBuffer buffer = ByteBuffer.allocate(0x2);
	
	buffer.put(PACKET_TYPE);
	buffer.put(length(0));
	buffer.flip();
	
	return buffer;
    }

    @Override
    public String getPacketID() {
	return null;
    }
}
