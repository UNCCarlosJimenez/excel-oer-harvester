/**
 * 
 */
package com.unicomer.oer.harvester.model;

import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.Relationship;

/**
 * @author carlosj_rodriguez
 *
 */
public class URelationship extends Relationship {

	public URelationship(Entity relatedTo, String name, boolean useAsSourceEntity) {
		super(relatedTo, name, useAsSourceEntity);
	}
	
	public URelationship() {
		super(null, null, false);
	}	
}
