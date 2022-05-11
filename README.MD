# Federated Semantic Search Engine Index

Semantic Engine with Enhanced Discovery Services (SEEDS) Index enables the
discovery of data offerings in the i3-MARKET network via federated semantic
queries. When a SEEDS instance joins the i3-MARKET network, it publishes the URI
of its semantic search service endpoint along with available semantic data
categories in the SeedsIndexStorage smart contract deployed on the Besu
distributed ledger. In addition to the smart contract, the SEEDS Index solution
consists of a Java library SeedsIndex that provides wrappers for the smart
contract and utility functions for convenience. The SeedsIndex library is uses
Web3j library for accessing the Besu network.

Each SEEDS instance has a private key. Corresponding public key serves as an
identifier that can be assigned a SEEDS Index record. The private key is needed
to update corresponding index records.

SEEDS accounts must have enough resources to pay for update transactions.

The owner of the SeedsIndexStorage smart contract can assign administrator roles
to other keys that can update records stored under any public key.

## Getting started

When using Gradle, declare the i3-MARKET artifact repository in build.gradle file:

```
repositories {
    ...
    maven {
        url 'http://95.211.3.251:8081/repository/maven-snapshots/'
        allowInsecureProtocol true
        content {
            includeGroup 'eu.i3market.seedsindex'
        }
     }
}
```

Include the dependency:

```
dependencies {
    ...
    implementation 'eu.i3market.seedsindex:seedsindex:2.0.1-SNAPSHOT'
    ....
}
```

The following is a complete SeedsIndex library usage example:

```
package eu.i3market.example.seedsindex;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import eu.i3market.seedsindex.DataCategory;
import eu.i3market.seedsindex.SearchEngineIndexRecord;
import eu.i3market.seedsindex.SeedsIndex;

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
            seedsIndex.deleteMyIndexRecord();
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            seedsIndex.shutdown();
        }
    }
}
```
## How to build

This project is built using Gradle:

`./gradlew build`

## Further information

This project is used as a library in the Semantic Engine i3-MARKET component.

## License

This project is licensed under Apache 2.0 license, see LICENSE.TXT.

