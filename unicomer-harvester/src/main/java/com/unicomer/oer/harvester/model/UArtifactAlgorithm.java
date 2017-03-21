/**
 * 
 */
package com.unicomer.oer.harvester.model;

import com.oracle.artifact.fingerprint.ArtifactFingerprinter;
import com.oracle.artifact.fingerprint.DefaultArtifactFingerprinter;
import com.oracle.artifact.fingerprint.FingerprintException;
import com.oracle.artifact.fingerprint.WSDLAbstractingFingerprinter;
import com.oracle.artifact.fingerprint.WSDLFingerprinter;
import com.oracle.artifact.fingerprint.XMLFingerprinter;
import com.oracle.artifact.fingerprint.XSDFingerprinter;
import com.oracle.artifact.transformer.ArtifactTransformer;
import com.oracle.artifact.transformer.DefaultArtifactTransformer;
import com.oracle.artifact.transformer.TransformException;
import com.oracle.artifact.transformer.WSDLAbstractingTransformer;

/**
 * @author carlosj_rodriguez
 *
 */
public class UArtifactAlgorithm {
	private final Class fingerprinterClass;
	private final Class transformerClass;
	public static final UArtifactAlgorithm DEFAULT = new UArtifactAlgorithm(DefaultArtifactTransformer.class,
			DefaultArtifactFingerprinter.class);

	public static final UArtifactAlgorithm XML = new UArtifactAlgorithm(DefaultArtifactTransformer.class,
			XMLFingerprinter.class);

	public static final UArtifactAlgorithm XSD = new UArtifactAlgorithm(DefaultArtifactTransformer.class,
			XSDFingerprinter.class);

	public static final UArtifactAlgorithm WSDL = new UArtifactAlgorithm(DefaultArtifactTransformer.class,
			WSDLFingerprinter.class);

	public static final UArtifactAlgorithm WSDL_ABSTRACTING = new UArtifactAlgorithm(WSDLAbstractingTransformer.class,
			WSDLAbstractingFingerprinter.class);

	private UArtifactAlgorithm(Class transformerClass, Class fingerprinterClass) {
		this.transformerClass = transformerClass;
		this.fingerprinterClass = fingerprinterClass;
	}
	
	public UArtifactAlgorithm() {
		this.transformerClass = null;
		this.fingerprinterClass = null;
	}

	public static UArtifactAlgorithm forName(String name) {
		if (name.equalsIgnoreCase("XML"))
			return XML;
		if (name.equalsIgnoreCase("XSD"))
			return XSD;
		if (name.equalsIgnoreCase("WSDL"))
			return WSDL;
		if (name.equalsIgnoreCase("WSDL_ABSTRACTING"))
			return WSDL_ABSTRACTING;
		if (name.equalsIgnoreCase("DEFAULT")) {
			return DEFAULT;
		}
		return null;
	}

	public static UArtifactAlgorithm forAssetType(String assetTypeName) {
		String[] nameParts = assetTypeName.split(":");
		String name = nameParts[(nameParts.length - 1)].trim();
		UArtifactAlgorithm result = forName(name);
		if (result == null) {
			result = DEFAULT;
		}
		return result;
	}

	public Class getTransformerClass() {
		return this.transformerClass;
	}

	public Class getFingerprinterClass() {
		return this.fingerprinterClass;
	}

	public ArtifactTransformer newTransformer() throws TransformException {
		try {
			return (ArtifactTransformer) this.transformerClass.newInstance();
		} catch (InstantiationException e) {
			throw new TransformException(e);
		} catch (IllegalAccessException e) {
			throw new TransformException(e);
		}
	}

	public ArtifactFingerprinter newFingerprinter() throws FingerprintException {
		try {
			return (ArtifactFingerprinter) this.fingerprinterClass.newInstance();
		} catch (InstantiationException e) {
			throw new FingerprintException(e);
		} catch (IllegalAccessException e) {
			throw new FingerprintException(e);
		}
	}

}
