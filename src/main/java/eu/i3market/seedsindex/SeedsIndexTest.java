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

import java.io.PrintStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import eu.i3market.seedsindex.besu.SeedsIndexStorage;

/**
 * This is a program to test the following use cases in a private Besu node:
 *   - deployment of contract
 *   - creating/deleting index record
 *   - querying index
 */
public class SeedsIndexTest {
    
    // We do not want to import a logger implementation for just this test
    private static PrintStream log = System.err;

    public static void main(String[] args) throws Exception {
        
        if (args.length != 1) {
            log.println("Usage: SeedsIndexTest <besuNodeAddress>");
            System.exit(1);
        }

        String besuNodeAddress = args[0];

        ECKeyPair adminKeyPair = Keys.createEcKeyPair();
        ECKeyPair userKeyPair  = Keys.createEcKeyPair();

        SeedsIndex seedsIndex = null;
        try {
            Credentials adminCreds = Credentials.create(adminKeyPair);
            
            String contractAddress = deployContract(besuNodeAddress, adminCreds);
        
            log.println("Creating SeedsIndex instance...");
            seedsIndex = new SeedsIndex(besuNodeAddress, Numeric.toHexStringNoPrefix(userKeyPair.getPrivateKey()));

            seedsIndex.setContractAddress(contractAddress);
            seedsIndex.setGasPrice(BigInteger.ZERO);
            
            log.println("Initializing SeedsIndex instance...");
            seedsIndex.init();

            log.println("Looking up instance's (" + seedsIndex.getMyNodeId() + ") record...");
            Optional<SearchEngineIndexRecord> indexRecord 
                = seedsIndex.getIndexRecordByNodeId(seedsIndex.getMyNodeId());
       
            if (indexRecord.isEmpty()) {
                log.println("Index record not found, adding new...");
                seedsIndex.setMyIndexRecord(new URI("https://example.org/semantic-search/"),
                        new DataCategory[] {
                                DataCategory.AGRICULTURE,        // use Enum value
                                DataCategory.byLabel("JuStIcE"), // use case insensitive string lookup
                                DataCategory.EDUCATION
                });
            } else {
                log.println("Found record: " + indexRecord.get().toString());
            }
            
            log.println("Looking up records by category...");
            Collection<SearchEngineIndexRecord> rs = seedsIndex.findByDataCategory(DataCategory.EDUCATION);
            for (var se : rs) {
                log.println("Found: " + se.toString());
            }
            
            log.println("Deleting my record...");
            seedsIndex.deleteMyIndexRecord();
            log.println("Success.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            seedsIndex.shutdown();
        }
    }
    
    private static String deployContract(String besuNodeAddress, Credentials creds) throws Exception {
        log.println("Deploying contract to node at: " + besuNodeAddress + "...");
        Web3j web3j = Web3j.build(new HttpService(besuNodeAddress));
        ContractGasProvider gasProvider = new StaticGasProvider(BigInteger.ZERO, BigInteger.valueOf(12500000));
        String contractAddress = SeedsIndexStorage.deploy(web3j, creds, gasProvider)
                .send().getContractAddress();
        log.println("Contract deployed at: " + contractAddress);
        return contractAddress;
    }
}
