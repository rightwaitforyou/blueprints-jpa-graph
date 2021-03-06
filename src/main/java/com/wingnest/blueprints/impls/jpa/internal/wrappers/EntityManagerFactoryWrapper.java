/*
 * Copyright since 2014 Shigeru GOUGI (sgougi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wingnest.blueprints.impls.jpa.internal.wrappers;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wingnest.blueprints.impls.jpa.exceptions.BpJpaExceptionFactory;
import com.wingnest.blueprints.impls.jpa.internal.dampers.Damper;
import com.wingnest.blueprints.impls.jpa.internal.dampers.DamperFactory;

final public class EntityManagerFactoryWrapper {

	private static final String BLUEPRINTS_JPAGRAPH_PERSISTENCE_UNIT_PROPERTIES = "blueprints.jpagraph.persistence-unit-properties";

	private static Logger logger = LoggerFactory.getLogger(EntityManagerFactoryWrapper.class);
		
	private final EntityManagerFactory entityManagerFactory;

	private final Damper damper;
	 
	public EntityManagerFactoryWrapper() {
		this(null, null);
	}
		 
	public EntityManagerFactoryWrapper(String persistanceUnitName) {
		this(persistanceUnitName, null);
	}
	
	public EntityManagerFactoryWrapper(Map<String, Object> props) {
		this(null, props);
	}

	public EntityManagerFactoryWrapper(EntityManagerFactory entityManagerFactory) {
		super();
		if (entityManagerFactory == null) throw BpJpaExceptionFactory.cannotBeNull("entityManagerFactory");
		
		this.entityManagerFactory = entityManagerFactory;
		this.damper = DamperFactory.create(this.entityManagerFactory);
	}

	public EntityManagerFactoryWrapper(String persistanceUnitName, @SuppressWarnings("rawtypes") Map props)  {
		super();
		String pPersistanceUnitName = persistanceUnitName != null ? persistanceUnitName : System.getProperty("jpagraph.unit-name");
		if (pPersistanceUnitName == null) {
			ClassLoader cl = this.getClass().getClassLoader();
			try {
				cl.loadClass("com.objectdb.jpa.EMF");
				pPersistanceUnitName = "ObjectDbUnit";
			} catch (ClassNotFoundException e){}
			try {
				cl.loadClass("org.eclipse.persistence.internal.jpa.EntityManagerFactoryImpl");
				pPersistanceUnitName = "EclipseLinkUnit";
			} catch ( ClassNotFoundException e ) {}
			try {
				cl.loadClass("org.hibernate.jpa.internal.EntityManagerFactoryImpl");
				pPersistanceUnitName = "HibernateUnit";
			} catch ( ClassNotFoundException e ) {}
		}

		if (pPersistanceUnitName == null) throw BpJpaExceptionFactory.cannotBeNull("pPersistanceUnitName");
		if (pPersistanceUnitName.length() == 0) throw BpJpaExceptionFactory.cannotBeEmpty("pPersistanceUnitName");

		logger.debug(String.format("persistanceUnitName = %s", pPersistanceUnitName));
		try {
			this.entityManagerFactory = Persistence.createEntityManagerFactory(pPersistanceUnitName, props);
			logger.debug("EntityManagerFactory class : " + entityManagerFactory.getClass().getName());
			this.damper = DamperFactory.create(this.entityManagerFactory);
		} catch (RuntimeException e) {
			logger.error(String.format("Called Persistence.createEntityManagerFactory: persistanceUnitName = '%s'", pPersistanceUnitName), e);
			throw e;
		}
	}
	
	public EntityManagerFactoryWrapper(Configuration configuration) {	
		this(getUnitName(configuration), getProperties(configuration));	
	}
	
	private static String getUnitName(Configuration configuration) {
		return configuration.getString("blueprints.jpagraph.unit-name");
	}
	
	@SuppressWarnings("rawtypes")
	private static Map getProperties(Configuration configuration) {
		Iterator<String> it = configuration.getKeys(BLUEPRINTS_JPAGRAPH_PERSISTENCE_UNIT_PROPERTIES);
		Properties props = new Properties();
		while (it.hasNext()) {
			String key = it.next();
			String newkey = key.substring(BLUEPRINTS_JPAGRAPH_PERSISTENCE_UNIT_PROPERTIES.length() + 1).replaceAll("\\.\\.", ".");
			String value = configuration.getString(key);
			props.put(newkey, value);
			logger.debug("EntityManagerFactory getProperties : key = " + newkey + ", value = " + value);
		}
		return props;
	}	

	public EntityManager createEntityManager(@SuppressWarnings("rawtypes") Map props) {
		EntityManager entityManager = entityManagerFactory.createEntityManager(props);
		return entityManager;		
	}

	public EntityManager createEntityManager() {
		return createEntityManager(null);
	}
	
	public EntityManagerFactory getEntityManagerFactory() {
		return this.entityManagerFactory;
	}
	
	public void close() {
		this.entityManagerFactory.close();
	}

	public Damper getDamper() {
		return this.damper;
	}
}
