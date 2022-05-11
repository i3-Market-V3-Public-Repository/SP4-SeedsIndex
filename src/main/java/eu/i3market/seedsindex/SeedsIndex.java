/*
 * Copyright (c) 2020-2022 in alphabetical order:
 * Guardtime
 *
 * This program and the accompanying materials are made
 * available under the terms of the Apache 2.0 license
 * which is available at https://www.apache.org/licenses/LICENSE-2.0
 *
 * License-Identifier: Apache-2.0
 *
 * Contributors:
 *    Andres Ojamaa (Guardtime)
 */
package eu.i3market.seedsindex;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import com.google.gson.Gson;

import eu.i3market.seedsindex.besu.SeedsIndexStorage;
import eu.i3market.seedsindex.besu.SeedsIndexStorage.IndexUpdateEventResponse;
import io.reactivex.disposables.Disposable;

public class SeedsIndex {
    private static final Logger log = LoggerFactory.getLogger(SeedsIndex.class);

    private BigInteger gasLimit = BigInteger.valueOf(12500000);
    private BigInteger gasPrice = BigInteger.valueOf(20000000000L);

    private String contractAddress = "0x7136fdeb20b12ec08ef3f9e0bdd7186e7a6d774f";

    private Web3j web3j;
    private SeedsIndexStorage indexStorage;
    private String besuNodeAddress;
    private String nodePrivateKey;
    private Credentials nodeCredentials;
    private ContractGasProvider gasProvider;
    private byte[] myNodeId;
    private Disposable updateSubscription;
    private ConcurrentMap<String, SearchEngineIndexRecord> index;

    /**
     * Constructor for SeedsIndex instance.
     * @param besuNodeAddress the address of the Besu node to connect to
     * @param nodePrivateKey the private key of this node as hex encoded string
     */
    public SeedsIndex(String besuNodeAddress, String nodePrivateKey) {
        this.besuNodeAddress = besuNodeAddress;
        this.nodePrivateKey = nodePrivateKey;
    }

    /**
     * Initialize the SeedsIndex by connecting to the network and fetching current index.
     * The method blocks during the update process and the index is ready for use when the
     * method returns.
     * 
     * If the method throws an exception, the instance should be shutdown() and discarded.
     * @throws Exception in case the SeedsIndex instance is unable to perform initialization.
     */
    public void init() throws Exception {
        index = new ConcurrentHashMap<>();

        log.info("Validating private key...");
        if (WalletUtils.isValidPrivateKey(nodePrivateKey)) {
            log.info("Private key is valid.");
            this.nodeCredentials = Credentials.create(nodePrivateKey);
        } else {
            throw new RuntimeException("Invalid private key");
        }

        byte[] myNodeId = new byte[32];
        byte[] addressBytes = Numeric.hexStringToByteArray(nodeCredentials.getAddress());
        for (int i = 0; i < addressBytes.length; i++) {
            myNodeId[i + 12] = addressBytes[i];
        }
        this.myNodeId = Hash.sha256(myNodeId);
        log.info("Local node identifier (sha256(address)): {}", Numeric.toHexString(myNodeId));

        log.info("Connecting to Besu node: {}", besuNodeAddress);
        web3j = Web3j.build(new HttpService(besuNodeAddress));
        log.info("Connected to Besu network: chainId = {}, block = {}",
                web3j.ethChainId().send().getChainId(), web3j.ethBlockNumber().send().getBlockNumber());

        this.gasProvider = new StaticGasProvider(gasPrice, gasLimit);
        log.info("Created GasProvider with gasPrice = {}, gasLimit = {}", gasPrice, gasLimit);

        log.info("Loading storage smart contract at address: {}", contractAddress);
        indexStorage = SeedsIndexStorage.load(contractAddress, web3j, nodeCredentials, gasProvider);
        log.info("Loaded storage smart contract {} at address: {}",
                indexStorage.getClass().getSimpleName(),
                indexStorage.getContractAddress());

        log.info("Subscribing to index update events...");
        updateSubscription = indexStorage.indexUpdateEventFlowable(DefaultBlockParameterName.LATEST,
                DefaultBlockParameterName.LATEST).subscribe(this::consumeEvent);

        log.info("Fetching the list of known identifiers from the storage...");
        List<?> ids = indexStorage.getKeys().send();
        for (Object id : ids) {
            if (id instanceof byte[]) {
                byte[] idArray = (byte[]) id;
                String idStr = Numeric.toHexString(idArray);
                
                try {
                    String v = indexStorage.getValue(idArray).send();
                    updateIndexRecord(idArray, v);
                    log.info("Fetched index record for: {}", idStr);
                } catch (Exception e) {
                    log.error("Fetching index record for {} failed: {}", idStr, e);
                }
            }
        }
    }

    /**
     * Shut down the SeedsIndex instance.
     * Event notification subscriptions are cancelled, connections closed and internal thread
     * pools terminated.
     * The instance must not be used after shutting down and should be discarded.
     */
    public void shutdown() {
        if (updateSubscription != null) {
            updateSubscription.dispose();
        }
        if (web3j != null) {
            web3j.shutdown();
        }
        updateSubscription = null;
        indexStorage = null;
        web3j = null;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    /**
     * Sets the SeedsIndex smart contract address.
     * Default correct value is hardcoded, set only when alternative address is needed for testing etc.
     * @param contractAddress storage smart contract address as hex encoded string
     */
    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    private void consumeEvent(IndexUpdateEventResponse ev) {
        if (ev == null || ev.key == null) {
            log.warn("Null event received, ignoring.");
            return;
        }
        String id = Numeric.toHexString(ev.key);
        log.trace("Update event: {} : {}", id, ev.value);
        try {
            updateIndexRecord(ev.key, ev.value);
            log.info("Updated index record for: {}", id);
        } catch (Exception e) {
            log.warn("Processing index update event from {} failed", id, e);
        }
    }
    
    private void updateIndexRecord(byte[] key, String value) throws Exception {
        String id = Numeric.toHexString(key);

        if (value != null && value.length() > 0) {
            Gson gson = new Gson();
            SearchEngineIndexRecord r = gson.fromJson(value, SearchEngineIndexRecord.class);
            r.setId(id);
            index.put(id, r);
        } else {
            index.remove(id);
        }
    }

    public ContractGasProvider getGasProvider() {
        return this.gasProvider;
    }
    
    /**
     * Returns a hex string representation of the public key hash of this node.
     * The value is computed during init().
     * @return this node's identifier string
     */
    public String getMyNodeId() {
        return Numeric.toHexString(myNodeId);
    }
 
    /**
     * Update the search engine index record this node is advertising to the network.
     * The call to this method blocks until a transaction is sent to the Besu network.
     * @param location semantic search engine's endpoint address of this node
     * @param categories data categories available at this node
     * @throws Exception if update fails
     */
    public void setMyIndexRecord(URI location, DataCategory[] categories) throws Exception {
        SearchEngineIndexRecord r = new SearchEngineIndexRecord(location, categories);
        Gson gson = new Gson();
        String json = gson.toJson(r);
        indexStorage.setValue(myNodeId, json).send();
        r.setId(getMyNodeId());
        index.put(Numeric.toHexString(myNodeId), r);
    }

    /**
     * Delete this node's entry from the index.
     * @throws Exception when an error occurs
     */
    public void deleteMyIndexRecord() throws Exception {
        indexStorage.deleteValue(myNodeId).send();
    }

    /**
     * Look up a search engine index record by node identifier.
     * @param nodeId node identifier
     * @return corresponding search engine index record or empty if no such record exists
     */
    public Optional<SearchEngineIndexRecord> getIndexRecordByNodeId(String nodeId) {
        return Optional.ofNullable(index.get(nodeId));
    }
    
    /**
     * Returns a collection of search engine index records that contain the specified category.
     * @param category the category used for filtering, null is treated specially so that it matches any category
     * @return collection of matching records, could be empty
     */
    public Collection<SearchEngineIndexRecord> findByDataCategory(DataCategory category) {
        ArrayList<SearchEngineIndexRecord> a = new ArrayList<>();
        for (SearchEngineIndexRecord r : index.values()) {
            for (DataCategory c : r.getCategories()) {
                if (category == null || category.equals(c)) {
                    a.add(r);
                    break;
                }
            }
        }
        return a;
    }
}
