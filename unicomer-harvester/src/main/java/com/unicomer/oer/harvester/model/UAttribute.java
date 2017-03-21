/**
 * 
 */
package com.unicomer.oer.harvester.model;

import java.util.Collection;

import com.oracle.oer.sync.oer.client.component.attributes.Attribute;
/**
 * @author carlosj_rodriguez
 *
 */
public class UAttribute extends Attribute {
	
	public UAttribute() {
		super("", "", true);
	}

	/**
	 * @param that
	 */
	public UAttribute(Attribute that) {
		super(that);
	}

	/**
	 * @param name
	 * @param values
	 * @param visible
	 */
	public UAttribute(String name, Collection<String> values, boolean visible) {
		super(name, values, visible);
	}

	/**
	 * @param name
	 * @param value
	 * @param visible
	 */
	public UAttribute(String name, String value, boolean visible) {
		super(name, value, visible);
	}
	
	
}
