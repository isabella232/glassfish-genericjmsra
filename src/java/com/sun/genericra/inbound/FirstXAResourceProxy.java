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

package com.sun.genericra.inbound;

import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import com.sun.genericra.XAResourceType;
import com.sun.genericra.AbstractXAResourceType;


/**
 * <code>XAResource</code> wrapper for Generic JMS Connector. This class
 * intercepts all calls to the actual XAResource to facilitate redelivery.
 *
 *  Basically each (re)delivery for message will happen in different transactions
 *  from appserver perspective. However they will be intercepted and only
 *  one XID will be actually used with JMS provider.
 *
 *  @author Binod P.G
 */
public class FirstXAResourceProxy extends AbstractXAResourceType {
    
    
    private XAResource xar = null;
    private boolean toRollback = true;
    private boolean rolledback = false;
    private boolean suspended = false;
    private boolean endCalled = false;
    private Xid savedxid = null;
    
    
    public FirstXAResourceProxy(XAResource xar) {
        this.xar = xar;
    }
    
    
    /**
     * Commit the global transaction specified by xid.
     *
     * @param xid
     *            A global transaction identifier
     * @param onePhase
     *            If true, the resource manager should use a one-phase commit
     *            protocol to commit the work done on behalf of xid.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        xar.commit(savedXid(), onePhase);
    }
    
    /**
     * Ends the work performed on behalf of a transaction branch.
     *
     * @param xid
     *            A global transaction identifier that is the same as what was
     *            used previously in the start method.
     * @param flags
     *            One of TMSUCCESS, TMFAIL, or TMSUSPEND
     */
    public void end(Xid xid, int flags) throws XAException {
        if (beingRedelivered() == false ) {
            xar.end(savedXid(), flags);
            if (flags == XAResource.TMSUSPEND) {
                suspended = true;
            }
            endCalled = true;
        }
    }
    
    /**
     * When message is being redelivered, i.e, end is called
     * and that too without TMSUSPEND flag, return true.
     *
     * This also assumes that, when the message is being
     * redelivered, the MDB wouldnt be coded to such that
     * transaction would need to be suspended.
     */
    private boolean beingRedelivered() {
        return endCalled == true && suspended == false;
    }
    
    /**
     * Tell the resource manager to forget about a heuristically completed
     * transaction branch.
     *
     * @param xid
     *            A global transaction identifier
     */
    public void forget(Xid xid) throws XAException {
        xar.forget(savedXid());
    }
    
    /**
     * Obtain the current transaction timeout value set for this
     * <code>XAResource</code> instance.
     *
     * @return the transaction timeout value in seconds
     */
    public int getTransactionTimeout() throws XAException {
        return xar.getTransactionTimeout();
    }
    
    /**
     * This method is called to determine if the resource manager instance
     * represented by the target object is the same as the resouce manager
     * instance represented by the parameter xares.
     *
     * @param xares
     *            An <code>XAResource</code> object whose resource manager
     *            instance is to be compared with the resource
     * @return true if it's the same RM instance; otherwise false.
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        XAResource inxa = xares;
        if (xares instanceof XAResourceType) {
            XAResourceType wrapper = (XAResourceType) xares;
            inxa = (XAResource) wrapper.getWrappedObject();
            if (!compare(wrapper) ) {
                return false;
            }
        }
        return xar.isSameRM(inxa);
    }
    
    /**
     * Ask the resource manager to prepare for a transaction commit of the
     * transaction specified in xid.
     *
     * @param xid
     *            A global transaction identifier
     * @return A value indicating the resource manager's vote on the outcome of
     *         the transaction. The possible values are: XA_RDONLY or XA_OK. If
     *         the resource manager wants to roll back the transaction, it
     *         should do so by raising an appropriate <code>XAException</code>
     *         in the prepare method.
     */
    public int prepare(Xid xid) throws XAException {
        return xar.prepare(savedXid());
    }
    
    /**
     * Obtain a list of prepared transaction branches from a resource manager.
     *
     * @param flag
     *            One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS must be
     *            used when no other flags are set in flags.
     * @return The resource manager returns zero or more XIDs for the
     *         transaction branches that are currently in a prepared or
     *         heuristically completed state. If an error occurs during the
     *         operation, the resource manager should throw the appropriate
     *         <code>XAException</code>.
     */
    public Xid[] recover(int flag) throws XAException {
        return xar.recover(flag);
    }
    
    /**
     * Inform the resource manager to roll back work done on behalf of a
     * transaction branch
     *
     * @param xid
     *            A global transaction identifier
     */
    public void rollback(Xid xid) throws XAException {
        rolledback = true;
        if (toRollback) {
            xar.rollback(savedXid());
        }
    }
    
    
    /**
     * Set the current transaction timeout value for this
     * <code>XAResource</code> instance.
     *
     * @param seconds
     *            the transaction timeout value in seconds.
     * @return true if transaction timeout value is set successfully; otherwise
     *         false.
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xar.setTransactionTimeout(seconds);
    }
    
    /**
     * Start work on behalf of a transaction branch specified in xid.
     *
     * @param xid
     *            A global transaction identifier to be associated with the
     *            resource
     * @return flags One of TMNOFLAGS, TMJOIN, or TMRESUME
     */
    public void start(Xid xid, int flags) throws XAException {
        if (beingRedelivered() ) {
            return;
        }
        int actualflag = flags;
        if (this.savedxid == null) {
            this.savedxid = xid;
        } else if (flags == XAResource.TMNOFLAGS) {
            if (rolledback){
                rolledback = false;
                endCalled = false;
                if (suspended) {
                    suspended = false;
                    actualflag = XAResource.TMRESUME;
                } else {
                    actualflag = XAResource.TMJOIN;
                }
            }
        } else if (flags == XAResource.TMRESUME) {
            endCalled = false;
            suspended = false;
        }
        xar.start(savedXid(), actualflag);
    }
    
    public Object getWrappedObject() {
        return this.xar;
    }
    
    public void setToRollback(boolean flag) {
        toRollback = flag;
    }
    
    public boolean endCalled() {
        return endCalled;
    }
    
    private Xid savedXid() {
        return this.savedxid;
    }
    
    public void startDelayedXA(){
        throw new UnsupportedOperationException();
    }
}
