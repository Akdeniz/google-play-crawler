package com.akdeniz.googleplaycrawler.gsf.packets;

import java.nio.ByteBuffer;

import com.akdeniz.googleplaycrawler.gsf.Gsf.HeartBeatStat;
import com.akdeniz.googleplaycrawler.gsf.Gsf.LoginRequest;

/**
 * 
 * @author akdeniz
 */
public class LoginRequestPacket extends Packet {

    private static byte[] PACKET_TYPE = {0x02};
    
    
    private final String id;
    private LoginRequest loginRequest;

    public LoginRequestPacket(String user, String token, String deviceID) {
	id = nextID();
	
	loginRequest = LoginRequest.newBuilder()
		.setPacketid(id)
		.setDomain("mcs.android.com")
		.setUser( user)
		.setResource( user)
		.setToken( token)
		.setDeviceid("android-"+deviceID)
		//.setLastrmqid(0)
		//settings
		.setCompress(0)
		//persistent ids
		//included streams in protobuf
		.setAdaptiveheartbeat(false)
		.setHeartbeatstat(
			HeartBeatStat.newBuilder()
			.setTimeout(false)
			.setInterval(0))
		// userrmq2
		.setAccountid(-1)
		.setUnknown1(2)
		.setNetworktype(0)
	.build();
    }

    public ByteBuffer getBytes() {
	
	byte[] requestBytes = loginRequest.toByteArray();
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
