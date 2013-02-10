package com.akdeniz.googleplaycrawler.gsf.packets;

import java.nio.ByteBuffer;
import java.util.Random;

import com.akdeniz.googleplaycrawler.gsf.Gsf.BindAccountRequest;

/**
 * 
 * @author akdeniz
 *
 */
public class BindAccountRequestPacket extends Packet {

    private static byte[] PACKET_TYPE = { 0x0D };

    private final String id;
    private BindAccountRequest bindAccountRequest;

    public BindAccountRequestPacket(String email, String token) {
	id = nextID();

	bindAccountRequest = BindAccountRequest.newBuilder()
		.setPacketid(id)
		.setDomain(parseServer(email))
		.setUser(parseName(email))
		.setResource(computeJIDResource())
		.setToken(token)
	.build();
    }

    protected static String computeJIDResource() {
	StringBuilder strBuilder = new StringBuilder("android_talk");
	String str = Long.toHexString(new Random(System.currentTimeMillis()).nextLong());
	if (str.length() > 12) {
	    str = str.substring(0, 12);
	}

	for (int i = 0; i < 12 - str.length(); i++) {
	    strBuilder.append("0");
	}

	strBuilder.append(str);
	return strBuilder.toString();
    }

    @Override
    public String getPacketID() {
	return id;
    }

    public ByteBuffer getBytes() {

	byte[] requestBytes = bindAccountRequest.toByteArray();
	ByteBuffer buffer = ByteBuffer.allocate(0xFFFF);

	buffer.put(PACKET_TYPE);
	buffer.put(length(requestBytes.length));
	buffer.put(requestBytes);
	buffer.flip();

	return buffer;
    }

    public static void main(String[] args) {
	for (int m = 0; m < 15; m++) {
	    System.out.println(computeJIDResource());
	}
    }
}
