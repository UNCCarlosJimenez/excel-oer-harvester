/**
 * 
 */
package com.unicomer.oer.harvester.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.oracle.artifact.ArtifactAlgorithm;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.EntityBase;

/**
 * @author carlosj_rodriguez
 *
 */
@JsonPropertyOrder(alphabetic=true, value={ "id", "name", "description", "version", "assetType", "categorizations", "customData", "keywords",
		"relationships", "harvesterProperties", "relationshipsProcessed", "artifactAlgorithm", "namespace",
		"manifestEntries", "wsdlSummary", "summaryXML" })
public class UnicomerEntity extends EntityBase implements Entity {
	protected ArtifactAlgorithm artifactAlgorithm;
	
	public ArtifactAlgorithm getArtifactAlgorithm() {
		return artifactAlgorithm;
	}
	
	public void setArtifactAlgorithm(ArtifactAlgorithm artifactAlgorithm) {
		this.artifactAlgorithm = artifactAlgorithm;
	}

	public UnicomerEntity(){
		
	}
	
	/**
	 * Define la interfaz Entity para la implementacion de Unicomer
	 * 
	 * @param assetType: Tipo de activo
	 * @param id: ID del activo en el proceso
	 * @param name: Nombre del activo
	 * @param description: Descripcion
	 * @param version: Version
	 * @param artifactAlgorithm: Algoritmo para Harvest
	 * @throws Exception Error
	 */
	public UnicomerEntity(String assetType, String id, String name, String description, String version, ArtifactAlgorithm artifactAlgorithm) throws Exception {
		this.assetType = assetType;
		this.id = id;
		this.description = description;
		this.version = version;
		this.artifactAlgorithm = artifactAlgorithm;
		this.name = name;
		
		if (assetType == null) {
			this.assetType = "Application";
		}
	}
}
