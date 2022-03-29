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

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

/**
 * This class contains a usage example of SeedsIndex.
 */
public class Example {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: <Example> <besuNodeAddress> <privateKey>");
            System.exit(1);
        }

        String besuNodeAddress = args[0];
        String privateKey = args[1];
        
        SeedsIndex seedsIndex = new SeedsIndex(besuNodeAddress, privateKey);

        try {
            // Initialization is synchronous and will take time. The SeedsIndex instance can be used only
            // after initialization is done.
            seedsIndex.init(); 

            // Look up information about myself.
            Optional<SearchEngineIndexRecord> myRecord 
                = seedsIndex.getIndexRecordByNodeId(seedsIndex.getMyNodeId());
       
            // If my record is missing, publish it.
            if (myRecord.isEmpty()) {
                // Publish my data categories and service address. 
                seedsIndex.setMyIndexRecord(new URI("https://example.org/semantic-search/"),
                        new DataCategory[] {
                                DataCategory.AGRICULTURE,       // use Enum value
                                DataCategory.byLabel("JuStIcE"), // use case insensitive string lookup
                                DataCategory.EDUCATION
                });
            }
            
            // Look up semantic engines advertising data category Education".
            // This method returns quickly as it uses local cache.
            // The cache is kept up to date in background.
            Collection<SearchEngineIndexRecord> rs = seedsIndex.findByDataCategory(DataCategory.EDUCATION);
            for (var se : rs) {
                // String searchEngineId = se.getId();
                // URI searchEngineLocation = se.getLocation();
                // Collection<DataCategory> dataCategories = List.of(se.getCategories());
                
                System.err.println(se);
            }
            
            // Remove my index record
            // seedsIndex.deleteMyIndexRecord();
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            seedsIndex.shutdown();
        }
    }
}
