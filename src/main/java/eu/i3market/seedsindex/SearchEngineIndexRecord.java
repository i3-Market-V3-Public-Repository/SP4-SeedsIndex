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
import java.util.Arrays;

public class SearchEngineIndexRecord {
    private transient String id;
    private URI location;
    private DataCategory[] categories;

    public SearchEngineIndexRecord(URI location, String[] categories) {
        this.location = location;
        this.categories = Arrays.stream(categories).map(DataCategory::byLabel).toArray(DataCategory[]::new);
    }

    public SearchEngineIndexRecord(URI location, DataCategory[] categories) {
        this.location = location;
        this.categories = categories;
    }

    public URI getLocation() {
        return location;
    }

    public void setLocation(URI location) {
        this.location = location;
    }

    public DataCategory[] getCategories() {
        return categories;
    }

    public void setCategories(DataCategory[] categories) {
        this.categories = categories;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("SearchEngineIndexRecord {%s, %s, %s}", id, location, Arrays.toString(categories));
    }
}
