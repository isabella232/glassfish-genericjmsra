/*
 * Copyright (c) 2003-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sun.genericra.inbound.sync;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import java.util.Enumeration;

/**
 * See WMessage
 *
 * @author Frank Kieviet
 * @version $Revision: 1.1 $
 */
public class WMapMessageIn extends WMessageIn implements MapMessage {
    private MapMessage mDelegate;
    
    /**
     * Constructor
     * 
     * @param delegate real msg
     * @param ackHandler callback to call when ack() or recover() is called
     * @param ibatch index of this message in a batch; -1 for non-batched
     */
    public WMapMessageIn(MapMessage delegate, AckHandler ackHandler, int ibatch) {
        super(delegate, ackHandler, ibatch);
        mDelegate = delegate;
    }

    /**
     * @see javax.jms.MapMessage#getBoolean(java.lang.String)
     */
    public boolean getBoolean(String arg0) throws JMSException {
        return mDelegate.getBoolean(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getByte(java.lang.String)
     */
    public byte getByte(String arg0) throws JMSException {
        return mDelegate.getByte(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getBytes(java.lang.String)
     */
    public byte[] getBytes(String arg0) throws JMSException {
        return mDelegate.getBytes(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getChar(java.lang.String)
     */
    public char getChar(String arg0) throws JMSException {
        return mDelegate.getChar(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getDouble(java.lang.String)
     */
    public double getDouble(String arg0) throws JMSException {
        return mDelegate.getDouble(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getFloat(java.lang.String)
     */
    public float getFloat(String arg0) throws JMSException {
        return mDelegate.getFloat(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getInt(java.lang.String)
     */
    public int getInt(String arg0) throws JMSException {
        return mDelegate.getInt(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getLong(java.lang.String)
     */
    public long getLong(String arg0) throws JMSException {
        return mDelegate.getLong(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getMapNames()
     */
    public Enumeration getMapNames() throws JMSException {
        return mDelegate.getMapNames();
    }

    /**
     * @see javax.jms.MapMessage#getObject(java.lang.String)
     */
    public Object getObject(String arg0) throws JMSException {
        return mDelegate.getObject(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getShort(java.lang.String)
     */
    public short getShort(String arg0) throws JMSException {
        return mDelegate.getShort(arg0);
    }

    /**
     * @see javax.jms.MapMessage#getString(java.lang.String)
     */
    public String getString(String arg0) throws JMSException {
        return mDelegate.getString(arg0);
    }

    /**
     * @see javax.jms.MapMessage#itemExists(java.lang.String)
     */
    public boolean itemExists(String arg0) throws JMSException {
        return mDelegate.itemExists(arg0);
    }

    /**
     * @see javax.jms.MapMessage#setBoolean(java.lang.String, boolean)
     */
    public void setBoolean(String arg0, boolean arg1) throws JMSException {
        mDelegate.setBoolean(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setByte(java.lang.String, byte)
     */
    public void setByte(String arg0, byte arg1) throws JMSException {
        mDelegate.setByte(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setBytes(java.lang.String, byte[], int, int)
     */
    public void setBytes(String arg0, byte[] arg1, int arg2, int arg3) throws JMSException {
        mDelegate.setBytes(arg0, arg1, arg2, arg3);
    }

    /**
     * @see javax.jms.MapMessage#setBytes(java.lang.String, byte[])
     */
    public void setBytes(String arg0, byte[] arg1) throws JMSException {
        mDelegate.setBytes(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setChar(java.lang.String, char)
     */
    public void setChar(String arg0, char arg1) throws JMSException {
        mDelegate.setChar(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setDouble(java.lang.String, double)
     */
    public void setDouble(String arg0, double arg1) throws JMSException {
        mDelegate.setDouble(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setFloat(java.lang.String, float)
     */
    public void setFloat(String arg0, float arg1) throws JMSException {
        mDelegate.setFloat(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setInt(java.lang.String, int)
     */
    public void setInt(String arg0, int arg1) throws JMSException {
        mDelegate.setInt(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setLong(java.lang.String, long)
     */
    public void setLong(String arg0, long arg1) throws JMSException {
        mDelegate.setLong(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setObject(java.lang.String, java.lang.Object)
     */
    public void setObject(String arg0, Object arg1) throws JMSException {
        mDelegate.setObject(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setShort(java.lang.String, short)
     */
    public void setShort(String arg0, short arg1) throws JMSException {
        mDelegate.setShort(arg0, arg1);
    }

    /**
     * @see javax.jms.MapMessage#setString(java.lang.String, java.lang.String)
     */
    public void setString(String arg0, String arg1) throws JMSException {
        mDelegate.setString(arg0, arg1);
    }
}
