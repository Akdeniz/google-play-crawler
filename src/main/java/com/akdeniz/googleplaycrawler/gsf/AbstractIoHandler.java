package com.akdeniz.googleplaycrawler.gsf;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 * Abstract class not handle all {@link IoHandler} methods everytime.
 * 
 * @author akdeniz
 *
 */
public abstract class AbstractIoHandler implements IoHandler {

    @Override
    public void sessionCreated(IoSession session) throws Exception {
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
    }
}
