// Copyright (c) 2017-Present Pivotal Software, Inc.  All rights reserved.
//
// This software, the RabbitMQ Java client library, is triple-licensed under the
// Mozilla Public License 1.1 ("MPL"), the GNU General Public License version 2
// ("GPL") and the Apache License version 2 ("ASL"). For the MPL, please see
// LICENSE-MPL-RabbitMQ. For the GPL, please see LICENSE-GPL2.  For the ASL,
// please see LICENSE-APACHE2.
//
// This software is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
// either express or implied. See the LICENSE file for specific language governing
// rights and limitations of this software.
//
// If you have any questions regarding licensing, please contact us at
// info@rabbitmq.com.

package com.rabbitmq.client.test;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Command;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.impl.AMQImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

public class ChannelAsyncCompletableFutureTest extends BrokerTestCase {

    ExecutorService executor;

    String queue;

    @Before public void init() {
        executor = Executors.newSingleThreadExecutor();
        queue = UUID.randomUUID().toString();
    }

    @After public void tearDown() throws IOException {
        executor.shutdownNow();
        channel.queueDelete(queue);
    }

    @Test
    public void async() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AMQP.Queue.Declare method = new AMQImpl.Queue.Declare.Builder()
            .queue(queue)
            .durable(true)
            .exclusive(false)
            .autoDelete(false)
            .arguments(null)
            .build();
        CompletableFuture<Command> future = channel.asyncCompletableRpc(method);
        future.thenAcceptAsync(action -> {
            try {
                channel.basicPublish("", queue, null, "dummy".getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor).thenAccept((whatever) -> {
            try {
                channel.basicConsume(queue, true, new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        latch.countDown();
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

}