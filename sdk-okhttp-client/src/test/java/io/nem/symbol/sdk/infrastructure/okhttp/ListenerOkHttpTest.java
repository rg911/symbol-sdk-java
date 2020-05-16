/*
 * Copyright 2020 NEM
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
package io.nem.symbol.sdk.infrastructure.okhttp;

import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import io.nem.symbol.sdk.api.Listener;
import io.nem.symbol.sdk.infrastructure.ListenerChannel;
import io.nem.symbol.sdk.infrastructure.ListenerSubscribeMessage;
import io.nem.symbol.sdk.model.account.Account;
import io.nem.symbol.sdk.model.account.Address;
import io.nem.symbol.sdk.model.network.NetworkType;
import io.nem.symbol.sdk.model.transaction.CosignatureSignedTransaction;
import io.nem.symbol.sdk.model.transaction.JsonHelper;
import io.nem.symbol.sdk.model.transaction.Transaction;
import io.nem.symbol.sdk.model.transaction.TransactionStatusError;
import io.nem.symbol.sdk.model.transaction.TransactionStatusException;
import io.nem.symbol.sdk.openapi.okhttp_gson.invoker.JSON;
import io.nem.symbol.sdk.openapi.okhttp_gson.model.Cosignature;
import io.nem.symbol.sdk.openapi.okhttp_gson.model.TransactionInfoDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Tests for the {@link ListenerOkHttp} implementation of the {@link Listener}
 */
public class ListenerOkHttpTest {

    private ListenerOkHttp listener;

    private OkHttpClient httpClientMock;

    private WebSocket webSocketMock;

    private JsonHelper jsonHelper;

    private String wsId = "TheWSid";

    @BeforeEach
    public void setUp() {
        httpClientMock = Mockito.mock(OkHttpClient.class);
        String url = "http://nem.com:3000/";
        listener = new ListenerOkHttp(httpClientMock, url, new JSON());
        jsonHelper = listener.getJsonHelper();
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(webSocketMock);
    }

    @Test
    public void shouldOpen() throws ExecutionException, InterruptedException, TimeoutException {

        Assertions.assertNull(listener.getUid());

        Assertions.assertEquals(
            "Listener has not been opened yet. Please call the open method before subscribing.",
            Assertions
                .assertThrows(IllegalStateException.class, () -> listener.newBlock()).getMessage());

        simulateWebSocketStartup();

        Assertions.assertNotNull(listener.newBlock());

        Assertions.assertEquals(wsId, listener.getUid());

        listener.close();
        listener.close();

        Assertions.assertNull(listener.getUid());

        Map<String, String> sendPayload = new HashMap<>();
        sendPayload.put("uid", wsId);
        sendPayload.put("subscribe", "block");
        Mockito.verify(webSocketMock).send(jsonHelper.print(sendPayload));

        Assertions.assertEquals(
            "Listener has not been opened yet. Please call the open method before subscribing.",
            Assertions
                .assertThrows(IllegalStateException.class, () -> listener.newBlock()).getMessage());

        Mockito.verify(webSocketMock).close(1000, null);
    }


    @Test
    public void confirmAndGetError()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        TransactionInfoDTO transactionInfo = TestHelperOkHttp.loadTransactionInfoDTO(
            "aggregateMosaicCreationTransaction.json");

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(transactionInfo, JsonObject.class);

        Address address = Address.createFromPublicKey(
            jsonHelper.getString(transactionInfoDtoJsonObject, "transaction", "signerPublicKey"),
            NetworkType.MIJIN_TEST);

        String channelName = ListenerChannel.CONFIRMED_ADDED.toString();

        ((JsonObject) transactionInfoDtoJsonObject.get("meta"))
            .addProperty("channelName", channelName);

        List<Transaction> transactions = new ArrayList<>();
        listener.confirmed(address).forEach(transactions::add);

        listener.handle(transactionInfoDtoJsonObject, null);

        Assertions.assertEquals(1, transactions.size());

        Assertions.assertEquals(address, transactions.get(0).getSigner().get().getAddress());

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));

    }

    @Test
    public void cosignatureAdded()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        NetworkType networkType = NetworkType.MIJIN_TEST;
        Cosignature cosignature = new Cosignature().parentHash("aParentHash")
            .signature("aSignature")
            .signerPublicKey(Account.generateNewAccount(networkType).getPublicKey());

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(cosignature, JsonObject.class);

        Address address = Address.createFromPublicKey(cosignature.getSignerPublicKey(),
            networkType);

        String channelName = ListenerChannel.COSIGNATURE.toString();

        List<CosignatureSignedTransaction> transactions = new ArrayList<>();
        listener.cosignatureAdded(address).forEach(transactions::add);

        listener.handle(transactionInfoDtoJsonObject, null);

        Assertions.assertEquals(1, transactions.size());

        Assertions.assertEquals(cosignature.getSignerPublicKey(),
            transactions.get(0).getSignerPublicKey());
        Assertions.assertEquals(cosignature.getParentHash(), transactions.get(0).getParentHash());
        Assertions.assertEquals(cosignature.getSignature(), transactions.get(0).getSignature());

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));
    }

    @Test
    public void confirmedUsingHash()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        TransactionInfoDTO transactionInfo = TestHelperOkHttp.loadTransactionInfoDTO(
            "aggregateMosaicCreationTransaction.json");

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(transactionInfo, JsonObject.class);

        Address address = Address.createFromPublicKey(
            jsonHelper.getString(transactionInfoDtoJsonObject, "transaction", "signerPublicKey"),
            NetworkType.MIJIN_TEST);

        String channelName = ListenerChannel.CONFIRMED_ADDED.toString();

        ((JsonObject) transactionInfoDtoJsonObject.get("meta"))
            .addProperty("channelName", channelName);

        List<Transaction> transactions = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        listener.confirmed(address, transactionInfo.getMeta().getHash()).doOnError(exceptions::add)
            .forEach(transactions::add);

        listener.handle(transactionInfoDtoJsonObject, null);

        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(0, exceptions.size());

        Assertions.assertEquals(address, transactions.get(0).getSigner().get().getAddress());

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));

        Mockito.verify(webSocketMock).send(jsonHelper
            .print(new ListenerSubscribeMessage(this.wsId, "status" + "/" + address.plain())));

    }

    @Test
    public void confirmedUsingHashRaiseError()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        TransactionInfoDTO transactionInfo = TestHelperOkHttp.loadTransactionInfoDTO(
            "aggregateMosaicCreationTransaction.json");

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(transactionInfo, JsonObject.class);

        Address address = Address.createFromPublicKey(
            jsonHelper.getString(transactionInfoDtoJsonObject, "transaction", "signerPublicKey"),
            NetworkType.MIJIN_TEST);

        String channelName = ListenerChannel.CONFIRMED_ADDED.toString();

        Map<String, Object> transactionStatusError = new HashMap<>();
        transactionStatusError.put("address", address.encoded());
        transactionStatusError.put("code", "Fail 666");
        transactionStatusError.put("hash", transactionInfo.getMeta().getHash());
        transactionStatusError.put("deadline", 123);

        List<Transaction> transactions = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        listener.confirmed(address, transactionInfo.getMeta().getHash()).doOnError(exceptions::add)
            .forEach(transactions::add);

        listener.handle(transactionStatusError, null);

        Assertions.assertEquals(0, transactions.size());
        Assertions.assertEquals(1, exceptions.size());
        Assertions.assertEquals(TransactionStatusException.class, exceptions.get(0).getClass());
        Assertions
            .assertEquals("Fail 666 processing transaction " + transactionInfo.getMeta().getHash(),
                exceptions.get(0).getMessage());

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));

        Mockito.verify(webSocketMock)
            .send(jsonHelper
                .print(new ListenerSubscribeMessage(this.wsId, "status" + "/" + address.plain())));
    }

    @Test
    public void aggregateBondedAddedHash()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        TransactionInfoDTO transactionInfo = TestHelperOkHttp.loadTransactionInfoDTO(
            "aggregateMosaicCreationTransaction.json");

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(transactionInfo, JsonObject.class);

        Address address = Address.createFromPublicKey(
            jsonHelper.getString(transactionInfoDtoJsonObject, "transaction", "signerPublicKey"),
            NetworkType.MIJIN_TEST);

        String channelName = ListenerChannel.AGGREGATE_BONDED_ADDED.toString();

        ((JsonObject) transactionInfoDtoJsonObject.get("meta"))
            .addProperty("channelName", channelName);

        List<Transaction> transactions = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        listener.aggregateBondedAdded(address, transactionInfo.getMeta().getHash())
            .doOnError(exceptions::add)
            .forEach(transactions::add);

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));

        Mockito.verify(webSocketMock).send(jsonHelper
            .print(new ListenerSubscribeMessage(this.wsId, "status" + "/" + address.plain())));

        listener.handle(transactionInfoDtoJsonObject, null);

        Assertions.assertEquals(1, transactions.size());
        Assertions.assertEquals(0, exceptions.size());

        Assertions.assertEquals(address, transactions.get(0).getSigner().get().getAddress());

    }

    @Test
    public void aggregateBondedAddedRaisingError()
        throws InterruptedException, ExecutionException, TimeoutException {
        simulateWebSocketStartup();

        TransactionInfoDTO transactionInfo = TestHelperOkHttp.loadTransactionInfoDTO(
            "aggregateMosaicCreationTransaction.json");

        JsonObject transactionInfoDtoJsonObject = jsonHelper
            .convert(transactionInfo, JsonObject.class);

        Address address = Address.createFromPublicKey(
            jsonHelper.getString(transactionInfoDtoJsonObject, "transaction", "signerPublicKey"),
            NetworkType.MIJIN_TEST);

        String channelName = ListenerChannel.AGGREGATE_BONDED_ADDED.toString();

        Map<String, Object> transactionStatusError = new HashMap<>();
        transactionStatusError.put("address", address.encoded());
        transactionStatusError.put("code", "Fail 666");
        transactionStatusError.put("hash", transactionInfo.getMeta().getHash());
        transactionStatusError.put("deadline", 123);

        List<Transaction> transactions = new ArrayList<>();
        List<Throwable> exceptions = new ArrayList<>();
        listener.aggregateBondedAdded(address, transactionInfo.getMeta().getHash())
            .doOnError(exceptions::add)
            .forEach(transactions::add);

        Mockito.verify(webSocketMock)
            .send(jsonHelper.print(new ListenerSubscribeMessage(this.wsId,
                channelName + "/" + address.plain())));

        Mockito.verify(webSocketMock)
            .send(jsonHelper
                .print(new ListenerSubscribeMessage(this.wsId, "status" + "/" + address.plain())));

        listener.handle(transactionStatusError, null);

        Assertions.assertEquals(0, transactions.size());
        Assertions.assertEquals(1, exceptions.size());
        Assertions.assertEquals(TransactionStatusException.class, exceptions.get(0).getClass());
        Assertions
            .assertEquals("Fail 666 processing transaction " + transactionInfo.getMeta().getHash(),
                exceptions.get(0).getMessage());
    }

    private void simulateWebSocketStartup()
        throws InterruptedException, ExecutionException, TimeoutException {
        webSocketMock = Mockito.mock(WebSocket.class);
        ArgumentCaptor<WebSocketListener> webSocketListenerArgumentCaptor = ArgumentCaptor
            .forClass(WebSocketListener.class);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        when(httpClientMock
            .newWebSocket(requestCaptor.capture(), webSocketListenerArgumentCaptor.capture()))
            .thenReturn(webSocketMock);

        CompletableFuture<Void> future = listener.open();

        WebSocketListener webSocketListener = webSocketListenerArgumentCaptor.getValue();

        Assertions.assertNotNull(webSocketListener);

        Assertions
            .assertEquals("http://nem.com:3000/ws", requestCaptor.getValue().url().toString());

        webSocketListener
            .onMessage(webSocketMock, jsonHelper.print(Collections.singletonMap("uid", wsId)));

        future.get(3, TimeUnit.SECONDS);
    }

    @Test
    public void shouldHandleStatus()
        throws InterruptedException, ExecutionException, TimeoutException {
        Account account1 = Account.generateNewAccount(NetworkType.MIJIN_TEST);

        AtomicReference<TransactionStatusError> reference = new AtomicReference<>();

        simulateWebSocketStartup();
        Assertions.assertNotNull(listener.status(account1.getAddress()).subscribe(reference::set));

        Map<String, Object> message = new HashMap<>();
        message.put("hash", "1234hash");
        message.put("address", account1.getAddress().encoded());
        message.put("code", "some error");
        message.put("deadline", 5555);
        listener.handle(message, null);

        Assertions.assertNotNull(reference.get());

        Assertions.assertEquals(message.get("hash"), reference.get().getHash());
        Assertions.assertEquals(message.get("code"), reference.get().getStatus());
        Assertions.assertEquals(account1.getAddress(), reference.get().getAddress());

        Mockito.verify(webSocketMock)
            .send(jsonHelper
                .print(new ListenerSubscribeMessage(this.wsId,
                    "status" + "/" + account1.getAddress().plain())));

    }

    @Test
    public void shouldFilterOutHandleStatus()
        throws InterruptedException, ExecutionException, TimeoutException {
        Account account1 = Account.generateNewAccount(NetworkType.MIJIN_TEST);
        Account account2 = Account.generateNewAccount(NetworkType.MIJIN_TEST);

        AtomicReference<TransactionStatusError> reference = new AtomicReference<>();

        simulateWebSocketStartup();
        Assertions.assertNotNull(listener.status(account2.getAddress()).subscribe(reference::set));

        Map<String, Object> message = new HashMap<>();
        message.put("hash", "1234hash");
        message.put("address", account1.getAddress().encoded());
        message.put("code", "some error");
        message.put("deadline", 5555);
        listener.handle(message, null);

        Assertions.assertNull(reference.get());

        Mockito.verify(webSocketMock)
            .send(jsonHelper
                .print(new ListenerSubscribeMessage(this.wsId,
                    "status" + "/" + account2.getAddress().plain())));
    }

}
