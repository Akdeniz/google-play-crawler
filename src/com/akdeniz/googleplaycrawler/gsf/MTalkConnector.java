package com.akdeniz.googleplaycrawler.gsf;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.akdeniz.googleplaycrawler.Googleplay.Notification;
import com.akdeniz.googleplaycrawler.gsf.Gsf.AppData;
import com.akdeniz.googleplaycrawler.gsf.Gsf.Close;
import com.akdeniz.googleplaycrawler.gsf.Gsf.DataMessageStanza;
import com.akdeniz.googleplaycrawler.gsf.packets.NotificationPacket;
import com.akdeniz.googleplaycrawler.gsf.packets.UnknownResponse;
import com.akdeniz.googleplaycrawler.misc.Base64;
import com.akdeniz.googleplaycrawler.misc.DummyX509TrustManager;
import com.google.protobuf.Message;

/**
 * This class connects and logins <i>mtalk.google.com:5228</i> server and
 * listens for server app installation notifications. When it receives the
 * notification, it downloads the application by using thread mechanism.
 * 
 * @author akdeniz
 * 
 */
public class MTalkConnector extends AbstractIoHandler {

    private static final int PORT = 5228;
    private static final String HOSTNAME = "mtalk.google.com";
    private NioSocketConnector connector;
    private volatile int lastStreamID = 0;
    private static Object mutex = new Object();

    private List<MessageFilter> filters = new ArrayList<MessageFilter>();
    private NotificationListener notificationListener;

    public MTalkConnector(NotificationListener notificationListener) {
	this.notificationListener = notificationListener;
	connector = new NioSocketConnector();
	connector.setHandler(this);
	connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new GSFCodecFactory()));
	connector.getFilterChain().addFirst("sslfilter", getSSLFilter());
    }

    public ConnectFuture connect() {
	return connector.connect(new InetSocketAddress(HOSTNAME, PORT));
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
	cause.printStackTrace();
	session.close(true);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {

	if (message instanceof Message) {
	    Message msg = (Message) message;

	    if (msg instanceof Close) {
		session.close(true);
		return;
	    }

	    synchronized (filters) {
		for (ListIterator<MessageFilter> filterIter = filters.listIterator(); filterIter.hasNext(); ) {
			MessageFilter filter = filterIter.next();
			filter.add(msg);
		}
	    }

	    if (msg instanceof DataMessageStanza) {
		for (AppData appData : ((DataMessageStanza) msg).getAppdataList()) {
		    if (appData.getKey().equals("NOTIFICATION_PAYLOAD")) {
			byte[] bs = Base64.decode(appData.getValue(), Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
			Notification notification = Notification.parseFrom(bs);

			// TODO not every notification is for download! filter
			// by type!
			notificationListener.notificationReceived(notification);
		    }
		}
		// XXX how to find right last stream id?
		for (int i = lastStreamID; i < lastStreamID + 5; i++) {
		    session.write(new NotificationPacket(i));
		}
		synchronized (mutex) {
		    lastStreamID += 5;
		}
	    }

	} else {
	    UnknownResponse response = (UnknownResponse) message;
	}
    }

    public <T extends Message> void addFilter(MessageFilter<T> filter) {
	synchronized (filters) {
	    filters.add(filter);	    
	}
    }

    public <T extends Message> void removeFilter(MessageFilter<T> filter) {
	synchronized (filters) {
	    filters.remove(filter);	    
	}
    }

    private SslFilter getSSLFilter() {
	try {
	    SSLContext sslcontext = SSLContext.getInstance("SSL");
	    sslcontext.init(null, new TrustManager[] { new DummyX509TrustManager() }, new java.security.SecureRandom());
	    SslFilter sslfilter = new SslFilter(sslcontext);
	    sslfilter.setUseClientMode(true);
	    return sslfilter;
	} catch (Exception e) {
	    throw new IllegalStateException(e);
	}
    }
}
