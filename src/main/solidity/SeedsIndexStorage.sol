// SPDX-License-Identifier: Apache-2.0
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
 *    Vladimir Rogojin (Guardtime)
 */
/**
 * @title Semantic Search Engine Index storage
 
 * @notice Stores the list of known semantic search engine identifiers and
 * corresponding data category records.  Admin role  can modify/delete all
 * records.
 */

pragma solidity ^0.8.0;

import "@openzeppelin/contracts/access/AccessControlEnumerable.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/structs/EnumerableSet.sol";

contract SeedsIndexStorage is Ownable, AccessControlEnumerable {
	
    using EnumerableSet for EnumerableSet.Bytes32Set;

    EnumerableSet.Bytes32Set private keySet;

    mapping (bytes32 => string) values;

    event IndexUpdate(bytes32 key, string value);

    modifier onlyAdmin() {
	require(hasRole(DEFAULT_ADMIN_ROLE, msg.sender),
            "SeedIndexStorage: The caller must have admin role");
	_;
    }

    modifier authorizeEdit(bytes32 key) {
        require(key == sha256(abi.encode(msg.sender)) || hasRole(DEFAULT_ADMIN_ROLE, msg.sender),
            "SeedIndexStorage: The caller must be an admin or owner of the key");
	_;
    }

    constructor() {
	super._setupRole(DEFAULT_ADMIN_ROLE, msg.sender);
    }

    function grantAdminRole(address entity) external onlyOwner {
	super._setupRole(DEFAULT_ADMIN_ROLE, entity);
    }

    function revokeAdminRole(address entity) external onlyOwner {
	super.revokeRole(DEFAULT_ADMIN_ROLE, entity);
    }

    function isAdmin(address entity) external view returns (bool) {
	return hasRole(DEFAULT_ADMIN_ROLE, entity);
    }

    /**
     * @notice Set a search engine index record.
     * The caller must be the owner of the key or have the admin role.
     *
     * @param key     The identifier of the search engine
     * @param value   The index record value
     */
    function setValue(bytes32 key, string calldata value) public authorizeEdit(key) {
        keySet.add(key);
        values[key] = value;
        emit IndexUpdate(key, value);
    }

    /**
     * @notice Delete a search engine record from the index.
     * The caller must be the owner of the key or have the admin role.
     *
     * @param key   The identifier of the search engine
     */
    function deleteValue(bytes32 key) public authorizeEdit(key) {
        keySet.remove(key);
        delete values[key];
        emit IndexUpdate(key, "");
    }

    /**
     * @notice Get the index record of a specified search engine.
     * The caller must be the owner of the key or have the admin role.
     *
     * @param key The identifier of the search engine
     * @return The index value associated with the search engine
     */
    function getValue(bytes32 key) public view returns (string memory) {
	return values[key];
    }

    /**
     * @notice Get the list of search engine identifiers.
     *
     * @return The list of search engine identifiers
     */
    function getKeys() public view returns (bytes32[] memory) {
        return keySet._inner._values;
    }
}
