package com.akdeniz.googleplaycrawler.gsf.packets;

import java.nio.ByteBuffer;
import java.util.Random;
/**
 * Base of packets.
 * 
 * @author alidemiroz
 */
public abstract class Packet {

    static char[] ID_CHARS = "0123456789abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    static Random randGen = new Random();

    private static long id = 0;
    private static String prefix = randomString(7) + "-";
    private static Object mutex = new Object();

    protected static String nextID() {
	synchronized (mutex) {
	    return prefix + (id++);
	}
    }

    public abstract ByteBuffer getBytes();

    public abstract String getPacketID();

    protected static ByteBuffer length(int length) {

	ByteBuffer buffer = ByteBuffer.allocate(5);
	for (int j = 0; j < 5; j++) {
	    byte k = (byte) (length & 0x7F);
	    length >>>= 7;
	    if (length == 0) {
		buffer.put(k);
		break;
	    }
	    buffer.put((byte) (k | 0x80));
	}
	buffer.flip();
	return buffer;
    }

    public static int unlength(ByteBuffer buffer) {
	int total = 0;
	int len = buffer.remaining();
	for (int i = 0; i < len; i++) {
	    byte b = buffer.get();
	    total += (((byte) (b & 0x7f)) << (7 * i));
	    if (b > 0) {
		break;
	    }
	}
	return total;
    }

    private static String randomString(int length) {
	char[] randomChars = new char[length];
	for (int i = 0; i < length; i++) {
	    randomChars[i] = ID_CHARS[randGen.nextInt(ID_CHARS.length - 1)];
	}
	return new String(randomChars);
    }

    public static String parseName(String email) {
	if (email == null) {
	    return "";
	}
	int index = email.indexOf("@");
	if (index > 0) {
	    return email.substring(0, index);
	}
	return "";
    }

    public static String parseServer(String email) {
	if (email == null) {
	    return "";
	}
	int index = email.indexOf("@");
	if (index > 0) {
	    return email.substring(index+1);
	}
	return "";
    }
}
