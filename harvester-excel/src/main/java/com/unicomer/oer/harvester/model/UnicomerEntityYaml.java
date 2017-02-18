/**
 * 
 */
package com.unicomer.oer.harvester.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.flashline.util.StringUtils;
import com.oracle.bpelModel.WsdlSummaryDocument;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.framework.MetadataLogger;
import com.oracle.oer.sync.framework.MetadataManager;
import com.oracle.oer.sync.model.ArtifactEntity;
import com.oracle.oer.sync.model.Entity;
import com.oracle.oer.sync.model.ManifestEntry;
import com.unicomer.oer.harvester.model.URelationship;
import com.unicomer.oer.harvester.model.UAttribute;
import com.oracle.oer.sync.oer.client.component.name.AssetNamerImpl;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerEntityYaml {
	private MetadataLogger logger = MetadataManager.getLogger(UnicomerEntityYaml.class);
	protected AssetNamerImpl namer = new AssetNamerImpl();
	
	protected List<URelationship> relationships = new ArrayList<URelationship>();
	protected List<UAttribute> harvesterProperties = new ArrayList<UAttribute>();
	protected List<UAttribute> categorizations = new ArrayList<UAttribute>();
	protected Map<String, String> customData = new LinkedHashMap<String, String>();
	protected WsdlSummaryDocument.WsdlSummary wsdlSummary;
	protected String summaryXML;
	protected List<ManifestEntry> manifestEntries = new ArrayList<ManifestEntry>();
	protected String id;
	protected String name;
	protected String namespace;
	protected String description;
	protected String version;
	protected String assetType;
	protected boolean relationshipsProcessed = false;
	protected String[] mKeywords;
	protected UArtifactAlgorithm artifactAlgorithm;
	
	public UnicomerEntityYaml(){
		
	}
	
	/**
	 * 
	 * @param assetType: Tipo de activo
	 * @param id: ID del activo en el proceso
	 * @param name: Nombre del activo
	 * @param description: Descripcion
	 * @param version: Version
	 * @param artifactAlgorithm: Algoritmo para Harvest
	 * @throws Exception Error
	 */
	public UnicomerEntityYaml(String assetType, String id, String name, String description, String version, UArtifactAlgorithm artifactAlgorithm) throws Exception {
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
	

	public String getAssetType() {
		return this.assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public Map<String, String> getCustomData() {
		return this.customData;
	}

	public String getCustomDataValue(String key) {
		return (String) this.customData.get(key);
	}

	public String[] getKeywords() {
		return this.mKeywords;
	}

	public void addCustomData(String key, String value) {
		this.customData.put(key, value);
	}

	public String getDescription() {
		return this.description;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void addManifestEntry(ManifestEntry entry) {
		this.manifestEntries.add(entry);
	}

	public List<ManifestEntry> getManifestEntries() {
		return this.manifestEntries;
	}
	
	public UArtifactAlgorithm getArtifactAlgorithm() {
		return artifactAlgorithm;
	}

	public void setArtifactAlgorithm(UArtifactAlgorithm artifactAlgorithm) {
		this.artifactAlgorithm = artifactAlgorithm;
	}
	
	public String getName() {
		return this.name;
	}

	public String getNamespace() {
		return this.namespace;
	}

	public List<UAttribute> getHarvesterProperties() {
		return this.harvesterProperties;
	}

	public void addHarvesterPropery(UAttribute a) {
		addHarvesterProperty(a, false);
	}

	public void addHarvesterProperty(UAttribute  a, boolean pReplaceExisting) {
		UAttribute found = getHarvesterProperty(a.getName());
		if (found == null)
			this.harvesterProperties.add(a);
		else
			for (String value : a.getValues())
				if (pReplaceExisting)
					found.setValue(value);
				else
					found.addValue(value);
	}

	public void addHarvesterProperty(String propName, String value, boolean visible, boolean pReplaceExisting) {
		addHarvesterProperty(new UAttribute(propName, value, visible), pReplaceExisting);
	}

	public void addHarvesterProperty(String propName, String value, boolean visible) {
		addHarvesterProperty(new UAttribute(propName, value, visible), false);
	}

	public void addHarvesterProperty(String propName, boolean pReplaceExisting, String value) {
		addHarvesterProperty(propName, value, true, pReplaceExisting);
	}

	public void addHarvesterProperty(String propName, String value) {
		addHarvesterProperty(propName, value, true, false);
	}

	public String getHarvesterPropertyValue(String string) {
		UAttribute result = getHarvesterProperty(string);
		if (result != null) {
			return result.getValue();
		}
		return null;
	}

	public UAttribute getHarvesterProperty(String string) {
		for (UAttribute attribute : this.harvesterProperties) {
			if (attribute.getName().equals(string)) {
				return attribute;
			}
		}
		return null;
	}

	private void addCategorization(UAttribute a) {
		UAttribute found = getCategorization(a.getName());
		if (found == null)
			this.categorizations.add(a);
		else
			for (String value : a.getValues())
				found.addValue(value);
	}

	public void addCategorization(String propName, String value) {
		addCategorization(new UAttribute(propName, value, true));
	}

	public String getCategorizationValue(String string) {
		UAttribute result = getCategorization(string);
		if (result != null) {
			return result.getValue();
		}
		return null;
	}

	private UAttribute getCategorization(String string) {
		for (UAttribute attribute : this.categorizations) {
			if (attribute.getName().equals(string)) {
				return attribute;
			}
		}
		return null;
	}

	public List<UAttribute> getCategorizations() {
		return this.categorizations;
	}

	public List<URelationship> getRelationships() {
		return this.relationships;
	}

	public void addRelationship(URelationship r) {
		this.relationships.add(r);
	}

	public String getVersion() {
		return this.version;
	}

	public WsdlSummaryDocument.WsdlSummary getWsdlSummary() {
		return this.wsdlSummary;
	}

	public void setWsdlSummary(WsdlSummaryDocument.WsdlSummary summary) {
		this.wsdlSummary = summary;
	}

	public String getSummaryXML() {
		return this.summaryXML;
	}

	public void setSummaryXML(String summary) {
		this.summaryXML = summary;
	}

	public boolean getRelationshipsProcessed() {
		return this.relationshipsProcessed;
	}

	public void setRelationshipsProcessed(boolean relationshipsProcessed) {
		this.relationshipsProcessed = relationshipsProcessed;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setKeywords(String[] pKeywords) {
		this.mKeywords = pKeywords;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	protected String wrap(String artifactNS) {
		if (StringUtils.isEmpty(artifactNS))
			artifactNS = "";
		else {
			artifactNS = "{" + artifactNS.trim() + "}";
		}
		return artifactNS;
	}

	protected static String cleanupTypeName(String string) {
		if (string == null) {
			return "";
		}

		return string.replaceAll("[ -]", "");
	}

	public void addRelationship(Entity childEntity, String relType, boolean isThisSource) {
		URelationship rel = new URelationship(childEntity, relType, isThisSource);
		addRelationship(rel);

		this.logger.debug("Added relationship from : [" + getName() + "] to: [" + childEntity.getName() + " ] ");
	}

	public void addManifestEntry(ArtifactEntity childEntity, String parentURI, URI childURI, String relativeURI)
			throws Exception {
		if (relativeURI == null) {
			relativeURI = StringUtils.relativize(childURI.toString(), parentURI);
		}

		childEntity.setRelativePath(relativeURI);
		String childName = childEntity.getName();

		String childSfid = MetadataManager.getInstance().getFingerPrint(childURI, childEntity.getArtifactAlgorithm());
		ManifestEntry entry = new ManifestEntry(childSfid, relativeURI, childName, parentURI);

		addManifestEntry(entry);
	}
	
	public List<Entity> getRelatedEntities(String relType) {
		List<Entity> results = new ArrayList<Entity>();
		for (URelationship rel : getRelationships()) {
			if ((relType.equals(rel.getName())) && (rel.getRelatedTo() != null)) {
				results.add(rel.getRelatedTo());
			}
		}
		return results;
	}

	public Entity getRelatedEntity(String relType) throws MetadataIntrospectionException {
		List<Entity> results = getRelatedEntities(relType);
		if (results.isEmpty()) {
			return null;
		}
		if (results.size() > 1) {
			throw new MetadataIntrospectionException(
					"Found more than one related entity from " + getName() + " with type " + relType);
		}
		return (Entity) results.get(0);
	}

	public List<Entity> getChildEntities(String childAssetType) {
		List<Entity> results = new ArrayList<Entity>();
		List<URelationship> rels = getRelationships();
		for (URelationship rel : rels) {
			Entity child = rel.getRelatedTo();
			if (child.getAssetType().equals(childAssetType)) {
				results.add(child);
			}
		}
		return results;
	}

	public Entity getChildEntity(String childAssetType) throws MetadataIntrospectionException {
		List<Entity> results = getChildEntities(childAssetType);
		if (results.isEmpty()) {
			return null;
		}
		if (results.size() > 1) {
			throw new MetadataIntrospectionException(
					"Found more than one related entity from " + getName() + " with type " + results);
		}
		return (Entity) results.get(0);
	}

	public void removeRelatedEntity(Entity child) {
		Iterator<URelationship> iter = getRelationships().iterator();
		while (iter.hasNext()) {
			URelationship rel = iter.next();
			if (child.equals(rel.getRelatedTo()))
				iter.remove();
		}
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		builder.append(" assetType=" + this.assetType);
		builder.append(" name=" + this.name);

		return builder.toString();
	}
}
