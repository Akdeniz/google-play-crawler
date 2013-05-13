package com.akdeniz.googleplaycrawler.gsf.packets;

import com.akdeniz.googleplaycrawler.Utils;

/**
 * Wraps unknown or non-funtional data from server.
 * 
 * @author akdeniz
 */
public class UnknownResponse {
    
    private final int tag;
    private final int length;
    private final byte[] data;

    public UnknownResponse(int tag, int length, byte[] data) {
	this.tag = tag;
	this.length = length;
	this.data = data;
    }

    public int getTag() {
	return tag;
    }

    public int getLength() {
	return length;
    }

    public byte[] getData() {
	return data;
    }
    
    @Override
    public String toString() {
	return "UnknownResponse[ tag="+tag+", length="+length+", data="+ Utils.bytesToHex(data)+" ]";
    }

}
