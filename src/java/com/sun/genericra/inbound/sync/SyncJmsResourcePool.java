/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2004-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.genericra.inbound.sync;

import com.sun.genericra.inbound.AbstractJmsResourcePool;
import com.sun.genericra.outbound.ConnectionFactory;
import com.sun.genericra.util.ExceptionUtils;
import com.sun.genericra.util.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import java.util.logging.Level;
import java.util.List;
import javax.jms.*;

import javax.resource.ResourceException;

import javax.transaction.xa.XAResource;

/**
 *
 */
public class SyncJmsResourcePool extends AbstractJmsResourcePool    {
    private static Logger _logger;
    
    static {
        _logger = LogUtils.getLogger();
    }
    private int sessions ;
    
    private int connections;
    
    private List mResources;
    private int mSessions;
    private int mBatchSize;
    private boolean mHoldUntilAck;
    private boolean stopped = true;
    /** Creates a new instance of SyncJmsResourcePool */
    public SyncJmsResourcePool(SyncConsumer cons, boolean transacted) {
        super(cons, transacted);
        if (this.consumer.getSpec().getDeliveryConcurrencyMode() ==
                "SERIAL") {
            mSessions = 1;
        } else if (this.consumer.getSpec().getDestinationType().equals(
                javax.jms.Topic.class.getName())) {
            mSessions = 1;
        } else {
            mSessions =
                    this.consumer.getSpec().getMaxPoolSize();
        }
        
        _logger.log(Level.FINE, "number of sessions specified to be " + mSessions);
        
        // Batch
        mBatchSize = this.consumer.getSpec().getBatchSize();
        // HUA mode
        boolean huaMode = this.consumer.getSpec().getHUAMode();
        if (huaMode) {
            mHoldUntilAck = true;
        }
        mResources = new ArrayList();
    }
    
    public int getBatchSize(){
        return this.mBatchSize;
    }
    
    public boolean getHUAMode() {
        return this.mHoldUntilAck;
    }
    
    public int getSessions() {
        return this.mSessions;
    }
    public int getMaxSize() {
        return 1;
    }
    
    public long getMaxWaitTime() {
        return 1;
    }
    
    public int getCurrentResources() {
        
        return 1;
    }
    
    public int getBusyResources() {
        
        return 1;
    }
    
    public int getFreeResources() {
        
        return 1;
    }
    
    public int getConnectionsInUse() {
        return 1;
    }
    
    public int getWaiting() {
        
        return 1;
    }
    public synchronized void initialize() throws ResourceException {
        try {
            this.sessions = consumer.getSpec().getMaxPoolSize();
            //this.maxWaitTime = consumer.getSpec().getMaxWaitTime() * 1000;
            if (consumer.getSpec().getSupportsXA()) {
                _logger.log(Level.FINE, "Creating CF ");
                XAConnectionFactory xacf = (XAConnectionFactory) consumer.getConnectionFactory();
                _logger.log(Level.FINE, "Created CF ");
                _logger.log(Level.FINE, "Creating XA Connection ");                
                this.con = createXAConnection(xacf);
                String clientID = consumer.getSpec().getClientID();
                //Set the clientID only if it is not null
                if (clientID != null && !"".equals(clientID))
                    con.setClientID(clientID);
                _logger.log(Level.FINE, "DMD connection factory " + consumer.getDmdConnectionFactory());
                javax.jms.ConnectionFactory cf = (javax.jms.ConnectionFactory) consumer.getDmdConnectionFactory();
                
                if (consumer.getSpec().getSendBadMessagesToDMD() == true) {
                    this.dmdCon = createDmdConnection(cf);
                }
            } else {
                if (!(consumer.getConnectionFactory() instanceof javax.jms.ConnectionFactory)) {
                    String msg = sm.getString("classtype_not_correct",
                            consumer.getConnectionFactory().getClass().getName());
                    throw new ResourceException(msg);
                }
                
                cf = (javax.jms.ConnectionFactory) consumer.getConnectionFactory();
                this.con = createConnection(cf);
                String clientID = consumer.getSpec().getClientID();
                 //Set the clientID only if it is not null
                if (clientID != null && !"".equals(clientID))
                    con.setClientID(clientID);
            }
            try {
                for (int i = 0; i < mSessions; i++) {
                    SyncJmsResource res = create();
                    res.setSessionid(i);
                    mResources.add(res);
                    res.start();
                }
                
            } catch (JMSException e) {
                throw e;
            }
            stopped = false;
        } catch (JMSException e) {
            throw ExceptionUtils.newResourceException(e);
        }
    }
    
    public SyncJmsResource create() throws JMSException {
        _logger.log(Level.FINER, "Creating the Session");
        
        Session sess = null;
        XAResource xar = null;
        
        if (transacted) {
            sess = createXASession((XAConnection) con);
            xar = getXAResource((XASession) sess);
            _logger.log(Level.FINE, "Created new XA Session");
        } else {
            sess = createSession(con);
            _logger.log(Level.FINE, "Created new Session");
        }
        
        return new SyncJmsResource(sess, this, xar);
    }
    public void releaseAllResources() {
        Iterator it = mResources.iterator();
        
        while (it.hasNext()) {
            SyncJmsResource obj = (SyncJmsResource) it.next();
            
            try {
                obj.destroy();
            } catch (Exception e) {
                // This is just to make sure that if one resource fails to destroy
                // we still call  destroy on others.
                _logger.log(Level.SEVERE,
                        "Cannot destroy resource " + obj.toString());
            }
        }
    }
    /**
     * Stops message delivery. Any message that is currently being delivered
     * will not be affected. It can be resumed later.
     */
    public void stop() throws JMSException {
        this.stopped = true;
        releaseAllResources();
        
        if (dmdCon != null) {
            this.dmdCon.close();
        }
    }
    public void destroy() throws JMSException {
        
        stop();
    }
}
