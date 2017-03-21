package com.unicomer.oer.harvester.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class PropertiesLoader {
	private static final String CONF_SERVICE_PROPERTIES = "./unicomer-harvester.properties";
	private static PropertiesLoader instance;
	private static Properties properties = new Properties();

	public PropertiesLoader() {
		load();
	}
	
	public PropertiesLoader(String location) {
		load(location);
	}
	
	public static PropertiesLoader getInstance() {
		if (instance == null)
			synchronized (PropertiesLoader.class) {
				if (instance == null)
					instance = new PropertiesLoader();
			}
		return instance;
	}
	
	public static PropertiesLoader getInstance(String location) {
		if (instance == null)
			synchronized (PropertiesLoader.class) {
				if (instance == null)
					instance = new PropertiesLoader(location);
			}
		return instance;
	}

	public Properties load() {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(CONF_SERVICE_PROPERTIES);
			if (in == null)
				properties.load(new FileInputStream(CONF_SERVICE_PROPERTIES));
			else
				properties.load(in);
		} catch (IOException e) {
			System.err.println("unicomer-harvester: Error cargando archivo de propiedades " + e.getMessage());
			e.printStackTrace();
		}
		return properties;
	}
	
	public Properties load(String location) {
		try {
			InputStream in = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream(location);
			if (in == null)
				properties.load(new FileInputStream(location));
			else
				properties.load(in);
		} catch (IOException e) {
			System.err.println("unicomer-harvester: Error cargando archivo de propiedades " + e.getMessage());
			e.printStackTrace();
		}
		return properties;
	}

	public String getProperty(String prop) {
		String value = "";
		try {
			value = properties.getProperty(prop);
			if (value == null) {
				System.err.println("unicomer-harvester:  Error propiedad no encontrada " + prop);
			}
		} catch (Exception e) {
			System.err.println("unicomer-harvester:  Error propiedad no encontrada " + prop + " " + e);
		}
		return value;
	}

	public boolean putProperty(String prop, String valor) {
		boolean setValue = true;
		try {
			properties.put(prop, valor);
		} catch (Exception e) {
			System.err.println("unicomer-harvester:  Error propiedad no encontrada " + prop + " " + e);
			setValue = false;
		}
		return setValue;
	}

	public String getProperty(String prop, String vDefalult) {
		System.err.println("unicomer-harvester: [getProperty] prop=" + prop + "vDefalult=" + vDefalult);
		String value = "";
		try {
			value = properties.getProperty(prop, vDefalult);
			if (value == null) {
				System.out.println("unicomer-harvester: Asignando variable por defecto a la propiedad " + prop
						+ " vDefalult=" + vDefalult);
			}
		} catch (Exception e) {
			System.err.println("unicomer-harvester: Asignando variable por defecto a la propiedad " + prop + " vDefalult="
					+ vDefalult);
		}
		return value;
	}

	public Enumeration<?> getPropertyNames() {
		return properties.propertyNames();
	}
}
