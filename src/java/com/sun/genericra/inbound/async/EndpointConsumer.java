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

package com.sun.genericra.inbound.async;

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.inbound.*;
import com.sun.genericra.util.*;

import java.util.logging.*;

import javax.jms.*;

import javax.resource.ResourceException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;

import javax.transaction.xa.XAResource;

import com.sun.genericra.GenericJMSRA;
import com.sun.genericra.util.*;
import com.sun.genericra.monitoring.*;
/**
 * One <code>EndpointConsumer</code> represents one MDB deployment.
 * Important assumptions:
 *   - Each EndpointCOnsumer holds one InboundJmsResourcePool
 *     (ServerSessionPool) which holds a javax.jms.Connection object.
 *   - EndpointConsumer is also created when ra.getXAResources() is
 *     is called for transaction recovery.
 * @author Binod P.G
 */
public class EndpointConsumer extends com.sun.genericra.inbound.AbstractConsumer {
    

    InboundJmsResourcePool jmsPool = null;
    private ConnectionConsumer consumer = null;
    private ReconnectHelper reconHelper = null;


    public EndpointConsumer(MessageEndpointFactory mef,
        javax.resource.spi.ActivationSpec actspec) throws ResourceException {
        super(mef, actspec);       
    }

    public EndpointConsumer(javax.resource.spi.ActivationSpec actspec)
        throws ResourceException {
        this(null, actspec);
    }

  public AbstractJmsResourcePool getPool() {
        return jmsPool;
    }

    public Connection getConnection() {
        return jmsPool.getConnection();
    }

    public void restart() throws ResourceException {
        consumer = _start(reconHelper.getPool(), dest);
    }

    public void start() throws ResourceException {
        setTransaction();
        logger.log(Level.FINE,
            "Registering a endpoint consumer, transaction support :" +
            this.transacted);
        initialize(this.transacted);
        consumer = _start(jmsPool, dest);
    }

    public void initialize(boolean isTx) throws ResourceException {
        super.validate();
        jmsPool = new InboundJmsResourcePool(this, isTx);
        jmsPool.initialize();
    }



    private ConnectionConsumer _start(InboundJmsResourcePool pool, 
                               Destination dst) throws ResourceException {
        ConnectionConsumer consmr = null;
        logger.log(Level.FINE, "Starting the message consumption");

        try {
            Connection con = pool.getConnection();    
            /*
             * Code for tackling the client id uniqueness requirement
             * for durable subscriptions
            */ 
           
            this.setClientId();
            if (spec.getSubscriptionDurability().equals(Constants.DURABLE)) {
                    String subscription_name = 
                            ((spec.getInstanceCount() > 1) && (spec.getInstanceID() != 0)) ?
                            (spec.getSubscriptionName() + spec.getInstanceID()) :
                            (spec.getSubscriptionName());
                    consmr = pool.createDurableConnectionConsumer(                
                    dst, subscription_name, spec.getMessageSelector(),1 );
                    logger.log(Level.FINE, "Created durable connection consumer" + dst);               
            } else {
                consmr = pool.createConnectionConsumer(dst,
                        spec.getMessageSelector(), 1);
                logger.log(Level.FINE,
                    "Created non durable connection consumer" + dst);
            }

            con.start();
            this.reconHelper = new ReconnectHelper(pool, this);

            if (spec.getReconnectAttempts() > 0) {
                con.setExceptionListener(reconHelper);
            }
        } catch (JMSException je) {
            // stop();
            closeConsumer();
            throw ExceptionUtils.newResourceException(je);
        }

        logger.log(Level.INFO, "Generic resource adapter started consumption ");

        return consmr;
    }

    public void stop() {
        logger.log(Level.FINE, "Now stopping the message consumption");
        this.stopped = true;

        if (jmsPool != null) {
            try {
                jmsPool.destroy();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "" + t.getMessage(), t);
            }
        }

        closeConsumer();

        Connection con = jmsPool.getConnection();

        if (con != null) {
            try {
                con.close();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "" + t.getMessage(), t);
            }
        }
    }

    public void closeConsumer() {
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "" + t.getMessage(), t);
            }
        }
    }

    public void consumeMessage(Message message, InboundJmsResource jmsResource) {
        DeliveryHelper helper = jmsResource.getDeliveryHelper();
        helper.deliver(message, dmd);
    }   

}
