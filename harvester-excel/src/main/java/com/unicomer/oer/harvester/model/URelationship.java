/**
 * 
 */
package com.unicomer.oer.harvester.model;

/**
 * @author carlosj_rodriguez
 *
 */
public class URelationship {
	
	private UnicomerEntityYaml relatedTo;
	private String name;
	private boolean useAsSourceEntity;

	public URelationship(UnicomerEntityYaml relatedTo, String name, boolean useAsSourceEntity)
	  {
	    this.relatedTo = relatedTo;
	    this.name = name;
	    this.useAsSourceEntity = useAsSourceEntity;
	  }

	public String getName() {
		return this.name;
	}

	public UnicomerEntityYaml getRelatedTo() {
		return this.relatedTo;
	}

	public boolean isUseAsSourceEntity() {
		return this.useAsSourceEntity;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(new StringBuilder().append(" name=").append(this.name).toString());
		if (this.relatedTo != null) {
			builder.append(new StringBuilder().append(" relatedTo=").append(this.relatedTo.getName()).toString());
		}

		return builder.toString();
	}

	public URelationship() {

	}
}
