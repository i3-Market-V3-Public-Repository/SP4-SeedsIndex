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

import com.google.gson.annotations.SerializedName;

/**
 * Enumeration of i3-MARKET semantic data categories.
 * See: https://gitlab.com/i3-market/code/data-models/-/blob/master/Version-1/DataOfferingCategory.ttl
 */
public enum DataCategory {
    @SerializedName	("Agriculture")
    AGRICULTURE		("Agriculture", 	"Agriculture, fisheries, forestry and food"),
    @SerializedName	("Automotive")
    AUTOMOTIVE		("Automotive", 		"Automotive"),
    @SerializedName	("Culture")
    CULTURE		("Culture", 		"Culture and sport"),
    @SerializedName	("Economy")
    ECONOMY		("Economy", 		"Economy and finance"),
    @SerializedName	("Education")
    EDUCATION		("Education",		"Education"),
    @SerializedName	("Energy")
    ENERGY		("Energy",		"Energy"),
    @SerializedName	("Environment")
    ENVIRONMENT		("Environment",		"Environment"),
    @SerializedName	("Government")
    GOVERNMENT		("Government", 		"Government and public sector"),
    @SerializedName	("Health")
    HEALTH		("Health",		"Health"),
    @SerializedName	("International")
    INTERNATIONAL	("International",	"International issues"),
    @SerializedName	("Justice")
    JUSTICE		("Justice",		"Justice, legal system and public safety"),
    @SerializedName	("Manufacturing")
    MANUFACTURING	("Manufacturing",	"Manufacturing"),
    @SerializedName	("Regions")
    REGIONS		("Regions",		"Regions and cities"),
    @SerializedName	("Science")
    SCIENCE		("Science",		"Science and technology"),
    @SerializedName	("Transport")
    TRANSPORT		("Transport",		"Transport"),
    @SerializedName	("Wellbeing")
    WELLBEING		("Wellbeing",		"Wellbeing"),
    @SerializedName	("society")
    SOCIETY		("society",		"Population and society");

    private String label;
    private String description;

    private DataCategory(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return this.label;
    }

    public String description() {
        return this.description;
    }

    /**
     * Case insensitive lookup of data category by category label.
     * @param label
     * @return null if there is no matching data category
     */
    public static DataCategory byLabel(String label) {
        String lcLabel = label.toLowerCase();
        for (DataCategory c : DataCategory.values()) {
            if (c.label != null && c.label.toLowerCase().equals(lcLabel)) {
                return c;
            }
        }
        return null;
    }
}
