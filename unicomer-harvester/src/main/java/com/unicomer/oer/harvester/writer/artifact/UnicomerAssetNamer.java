/**
 * 
 */
package com.unicomer.oer.harvester.writer.artifact;

import java.util.HashSet;
import java.util.Set;

import com.flashline.registry.openapi.entity.AssetSummary;
import com.oracle.oer.sync.framework.MetadataIntrospectionException;
import com.oracle.oer.sync.oer.client.component.name.AssetNamer;
import com.unicomer.oer.harvester.writer.UnicomerOERWriter;

/**
 * @author carlosj_rodriguez
 *
 */
public class UnicomerAssetNamer implements AssetNamer {
	private UnicomerOERWriter writer;

	public UnicomerAssetNamer() {

	}

	public UnicomerAssetNamer(UnicomerOERWriter writer) {
		this.writer = writer;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.oracle.oer.sync.oer.client.component.name.AssetNamer#makeUnique(java.
	 * lang.String, java.lang.String)
	 */
	public String makeUnique(String name, String version) throws MetadataIntrospectionException {
		name = makeValid(null, name);

		String verName = null;

		if ((name == null) || (name.equals(""))) {
			throw new MetadataIntrospectionException("Name must not be null or empty.");
		}

		for (int i = 7; i < 20; i = Math.min(i + 4, name.length())) {
			try {
				verName = calcUniqueAssetName(name, version, i);
			} catch (SizeLimitExceededException slee) {
			}
		}
		if (verName == null) {
			throw new MetadataIntrospectionException(
					"No versioned name could be determined within max name length of 128");
		}
		return verName;
	}

	private String calcUniqueAssetName(String name, String version, int verSize)
			throws UnicomerAssetNamer.SizeLimitExceededException, MetadataIntrospectionException {
		int threshold = 128 - verSize;
		String baseName = name;
		String verName = name;

		if (name.length() > threshold) {
			baseName = name.substring(0, threshold);
		}

		Set<String> matchedNames = getAssetsByNameVersion(baseName, version);
		
		boolean lMatchFound = matchedNames.contains(name.toLowerCase());

		for (int i = 1; lMatchFound; i++) {
			verName = getVersionedName(baseName, i);
			if (verName.length() > 128) {
				throw new SizeLimitExceededException();
			}
			
			lMatchFound = matchedNames.contains(verName.toLowerCase());
		}

		return verName;
	}

	private String getVersionedName(String pName, int pVersion) {
		if (pVersion == 1) {
			return pName;
		}
		return pName + "-" + pVersion;
	}

	private Set<String> getAssetsByNameVersion(String name, String version) throws MetadataIntrospectionException {
		Set<String> results = new HashSet<String>();
		AssetSummary[] assets = this.writer.assetQueryByNameStartsWith(name, version);

		for (int i = 0; i < assets.length; i++) {
			results.add(assets[i].getName().toLowerCase());
		}

		return results;
	}

	private class SizeLimitExceededException extends Exception {
		private static final long serialVersionUID = 8949550741127822063L;

		private SizeLimitExceededException() {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.oracle.oer.sync.oer.client.component.name.AssetNamer#makeValid(java.
	 * lang.String, java.lang.String)
	 */
	public String makeValid(String namespace, String name) throws MetadataIntrospectionException {
		if (namespace == null) {
			namespace = "";
		}
		if (namespace.length() > 80) {
			namespace = namespace.substring(0, 79) + namespace.charAt(namespace.length() - 1);
		}

		name = namespace + name;
		if (name.length() > 128) {
			return name.substring(0, 128);
		}
		return name;
	}

}
