/*
 * Copyright 2015-2017 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canoo.dp.impl.client;

import com.canoo.dp.impl.client.legacy.ClientModelStore;
import com.canoo.dp.impl.client.legacy.communication.AbstractClientConnector;
import com.canoo.dp.impl.platform.client.session.StrictClientSessionSupportingURLConnectionResponseHandler;
import com.canoo.dp.impl.platform.core.Assert;
import com.canoo.dp.impl.remoting.codec.OptimizedJsonCodec;
import com.canoo.dp.impl.remoting.legacy.util.Function;
import com.canoo.platform.client.PlatformClient;
import com.canoo.platform.client.http.HttpClient;
import com.canoo.platform.client.http.HttpURLConnectionHandler;
import com.canoo.platform.client.session.ClientSessionStore;
import com.canoo.platform.remoting.DolphinRemotingException;
import com.canoo.platform.remoting.client.ClientConfiguration;
import com.canoo.platform.remoting.client.ClientContext;
import com.canoo.platform.remoting.client.ClientContextFactory;
import com.canoo.platform.remoting.client.ClientInitializationException;
import com.canoo.platform.remoting.client.RemotingExceptionHandler;

import java.util.concurrent.CompletableFuture;

/**
 * Factory to create a {@link ClientContext}. Normally you will create a {@link ClientContext} at the bootstrap of your
 * client by using the {@link #create(ClientConfiguration)} method and use this context as a singleton in your client.
 * The {@link ClientContext} defines the connection between the client and the Dolphin Platform server endpoint.
 */
public class ClientContextFactoryImpl implements ClientContextFactory{

    public ClientContextFactoryImpl() {
    }

    /**
     * Create a {@link ClientContext} based on the given configuration. This method doesn't block and returns a
     * {@link CompletableFuture} to receive its result. If the {@link ClientContext} can't be created the
     * {@link CompletableFuture#get()} will throw a {@link ClientInitializationException}.
     *
     * @param clientConfiguration the configuration
     * @return the future
     */
    public ClientContext create(final ClientConfiguration clientConfiguration) {
        Assert.requireNonNull(clientConfiguration, "clientConfiguration");
        HttpClient httpClient = PlatformClient.getService(HttpClient.class);
        final HttpURLConnectionHandler clientSessionCheckResponseHandler = new StrictClientSessionSupportingURLConnectionResponseHandler(clientConfiguration.getServerEndpoint());
        httpClient.addResponseHandler(clientSessionCheckResponseHandler);

        return new ClientContextImpl(clientConfiguration, new Function<ClientModelStore, AbstractClientConnector>() {
            @Override
            public AbstractClientConnector call(final ClientModelStore clientModelStore) {
                return new DolphinPlatformHttpClientConnector(clientConfiguration, clientModelStore, OptimizedJsonCodec.getInstance(), new RemotingExceptionHandler() {
                    @Override
                    public void handle(DolphinRemotingException e) {
                        for(RemotingExceptionHandler handler : clientConfiguration.getRemotingExceptionHandlers()) {
                            handler.handle(e);
                        }
                    }
                }, httpClient);
            }
        }, httpClient, PlatformClient.getService(ClientSessionStore.class));
    }

}
