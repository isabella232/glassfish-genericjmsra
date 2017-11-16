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

package com.sun.s1peqe.connector.mq.simplestress.client;

import javax.jms.*;
import javax.naming.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Simple stress test with Queue. The test starts NUM_CLIENT threads. Each
 * thread sends NUM_CYCLES messages to a Queue. An MDB consumes these messages
 * concurrently and responds with a reply Message to a ReplyQueue. While the
 * threads are being exercised, an attempt to read NUM_CLIENT*NUM_CYCLES
 * messages is performed. An attempt to read an extra message is also done to
 * ascertain that no more than NUM_CLIENT*NUM_CYCLES messages are available.
 */
public class SimpleMessageClient implements Runnable {
    static int NUM_CLIENTS = 20;
    static int NUM_CYCLES = 20;
    //static int NUM_CLIENTS = 2;
    //static int NUM_CYCLES = 2;
    static int TIME_OUT = 60000;
    static long MDB_SLEEP_TIME = 2000;
    static boolean debug = true;
    int id = 0;

    public SimpleMessageClient(int i) {
        this.id = i;
    }

    public static void main(String[] args) throws Exception {
        debug("In stress client " + args);
        /**
         * Start the threads that send messages to MDB
         */
        ArrayList al = new ArrayList();
        /*
         * Send first half
         */
        try {
            //for (int i =0; i < NUM_CLIENTS/2; i++) {
            for (int i = 0; i < NUM_CLIENTS; i++) {
                debug("creating Thread " + i);
                Thread client = new Thread(new SimpleMessageClient(i));
                al.add(client);
                client.start();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        debug("Now reading the reply queue");
        /*
         * Send second half
         */
        /*
         * al = new ArrayList(); try { for (int i = NUM_CLIENTS/2; i <
         * NUM_CLIENTS; i++) { Thread client = new Thread(new
         * SimpleMessageClient(i)); al.add(client); client.start(); } } catch
         * (Throwable t) { t.printStackTrace(); }
         *  
         */
        Context jndiContext = null;
        QueueConnectionFactory queueConnectionFactory = null;
        QueueConnection queueConnection = null;
        QueueSession queueSession = null;
        Queue queue = null;
        QueueReceiver queueReceiver = null;
        TextMessage message = null;
        try {
            jndiContext = new InitialContext();
            queueConnectionFactory = (QueueConnectionFactory) jndiContext
                            .lookup("java:comp/env/jms/QCFactory");
            queue = (Queue) jndiContext.lookup("java:comp/env/jms/clientQueue");
            queueConnection = queueConnectionFactory.createQueueConnection();
            queueSession = queueConnection.createQueueSession(false,
                            Session.AUTO_ACKNOWLEDGE);
            queueConnection.start();
            queueReceiver = queueSession.createReceiver(queue);
            HashMap map = new HashMap();
            long startTime = System.currentTimeMillis();
            boolean pass = true;
            // Receives all the messages and keep in the data structure
            for (int i = 0; i < NUM_CLIENTS * NUM_CYCLES ; i++) {
                TextMessage msg = (TextMessage) queueReceiver.receive(TIME_OUT);
                if (msg == null) {
                    debug("Received :" + i + " messages before exiting");
                    pass = false;
                    break;
                }
                Integer id = new Integer(msg.getIntProperty("replyid"));
                if (map.containsKey(id)) {
                    pass = false;
                    debug("Duplicate :" + id);
                }
                map.put(id, msg.getText());
            }
            long totalTime = System.currentTimeMillis() - startTime;
            debug("Received messages in :" + totalTime + " milliseconds");
            debug("------------------------------------------------------");

            // Now examine the received data
            for (int i = 0; i < NUM_CLIENTS * NUM_CYCLES ; i++) {
                String reply = (String) map.get(new Integer(i));
                if (!reply.equals("REPLIED:CLIENT")) {
                    pass = false;
                }
                debug("Receeived :" + i + ":" + reply);
            }
            // NEGATIVE TEST ::
            // Try to receive one more message than expected with a timeout
            TextMessage msg = (TextMessage) queueReceiver.receive(TIME_OUT);
            if (msg != null) {
                pass = false;
                debug("Received more than expected number of messages :"
                                + msg.getText());
            }
            if (pass) {
                System.out.println("Concurrent message delivery test - Queue : PASS");
            } else {
                System.out.println("Concurrent message delivery test - Queue : FAIL");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Concurrent message delivery test - Queue : FAIL");
        } finally {
            /*
             * Wait for all threads to finish
             */
            for (int i = 0; i < al.size(); i++) {
                Thread client = (Thread) al.get(i);
                try {
                    client.join();
                } catch (Exception e) {
                    debug(e.getMessage());
                }
            }
            try {
                queueConnection.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        }
    }

    public void run() {
        debug(" run of thread called");
        Context jndiContext = null;
        QueueConnectionFactory queueConnectionFactory = null;
        QueueConnection queueConnection = null;
        QueueSession queueSession = null;
        Queue queue = null;
        QueueSender queueSender = null;
        TextMessage message = null;
        try {
            jndiContext = new InitialContext();
            debug("Attempting to lookup QCF");
            queueConnectionFactory = (QueueConnectionFactory) jndiContext
                            .lookup("java:comp/env/jms/QCFactory");
            debug("Attempting to lookup QCF");
            queue = (Queue) jndiContext.lookup("java:comp/env/jms/SampleQueue");
            int startId = id * NUM_CYCLES;
            int endId = (id * NUM_CYCLES) + NUM_CYCLES;
            for (int i = startId; i < endId; i++) {
                try {
                    queueConnection = queueConnectionFactory
                                    .createQueueConnection();
                    queueSession = queueConnection.createQueueSession(false,
                                    Session.AUTO_ACKNOWLEDGE);
                    queueConnection.start();
                    queueSender = queueSession.createSender(queue);
                    message = queueSession.createTextMessage();
                    message.setText("CLIENT");
                    message.setIntProperty("id", i);
                    message.setLongProperty("sleeptime", MDB_SLEEP_TIME);
                    queueSender.send(message);
                    debug("Send the message :" + message.getIntProperty("id")
                                    + ":" + message.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                    debug("Exception occurred: " + e.toString());
                } finally {
                    if (queueSender != null) {
                        try {
                            //debug("about to close sender");
                            queueSender.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (queueSession != null) {
                        try {
                            //debug("about to close session");
                            queueSession.close();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (queueConnection != null) {
                        try {
                            //debug("about to close connection");
                            queueConnection.close();
                            //debug("Closed connection");
                        } catch (JMSException e) {
                            e.printStackTrace();
                        }
                    } 
                }
            }
        } catch (Throwable e) {
            debug("Exception occurred: " + e.toString());
        } 
    } 

    static void debug(String msg) {
        if (debug) {
            System.out.println("[SimpleClient] " + msg);
        }
    }
} 
