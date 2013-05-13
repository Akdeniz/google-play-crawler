package com.akdeniz.googleplaycrawler.gsf;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.google.protobuf.Message;

/**
 * Base of message filters.
 * 
 * @author akdeniz
 *
 * @param <T>
 */
@SuppressWarnings("unchecked")
public abstract class MessageFilter<T extends Message> {

    private Queue<T> filteredMessages = new ConcurrentLinkedDeque<>();
    private Class<?> clazz;
    
    public MessageFilter(Class<?> clazz) {
	this.clazz = clazz;
    }

    public void add(Message message) {

	if (message != null && clazz.isInstance(message)) {
	    if (accept((T) message)) {
		filteredMessages.add((T) message);
		// notify that message has accepted and added to queue
		synchronized (filteredMessages) {
		    filteredMessages.notify();
		}
	    }
	}
    }

    public T nextMessage(int interval) {
	synchronized (filteredMessages) {
	    try {
		filteredMessages.wait(interval);
	    } catch (InterruptedException e) {
	    }
	}
	if (filteredMessages.isEmpty()) {
	    return null;
	}
	return filteredMessages.remove();
    }

    protected abstract boolean accept(T message);

}
