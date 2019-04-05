/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.ejb.HibernateEntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.jpa.domain.sample.QRole;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

/**
 * Integratio test for lock support.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(MockitoJUnitRunner.class)
public class CrudMethodMetadataUnitTests {

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock CriteriaBuilder builder;
	@Mock CriteriaQuery<Role> criteriaQuery;
	@Mock JpaEntityInformation<Role, Integer> information;
	@Mock TypedQuery<Role> typedQuery;
	@Mock javax.persistence.Query query;

	RoleRepository repository;

	@Before
	public void setUp() {

		when(information.getJavaType()).thenReturn(Role.class);

		when(em.getDelegate()).thenReturn(em);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);

		JpaRepositoryFactory factory = new JpaRepositoryFactory(em) {
			@Override
			@SuppressWarnings("unchecked")
			public <T, ID extends Serializable> JpaEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
				return (JpaEntityInformation<T, ID>) information;
			}
		};

		repository = factory.getRepository(RoleRepository.class);
	}

	/**
	 * @see DATAJPA-73, DATAJPA-173
	 */
	@Test
	public void usesLockInformationAnnotatedAtRedeclaredMethod() {

		when(em.getCriteriaBuilder()).thenReturn(builder);
		when(builder.createQuery(Role.class)).thenReturn(criteriaQuery);
		when(em.createQuery(criteriaQuery)).thenReturn(typedQuery);
		when(typedQuery.setLockMode(any(LockModeType.class))).thenReturn(typedQuery);

		repository.findAll();

		verify(typedQuery).setLockMode(LockModeType.READ);
		verify(typedQuery).setHint("foo", "bar");
	}

	/**
	 * @see DATAJPA-359, DATAJPA-173
	 */
	@Test
	public void usesMetadataAnnotatedAtRedeclaredFindOne() {

		repository.findOne(1);

		Map<String, Object> expectedLinks = Collections.singletonMap("foo", (Object) "bar");
		LockModeType expectedLockModeType = LockModeType.READ;

		verify(em).find(Role.class, 1, expectedLockModeType, expectedLinks);
	}

	/**
	 * @see DATAJPA-574
	 */
	@Test
	public void appliesLockModeAndQueryHintsToQuerydslQuery() {

		when(em.getDelegate()).thenReturn(mock(HibernateEntityManager.class));
		when(em.createQuery(anyString())).thenReturn(query);

		repository.findOne(QRole.role.name.eq("role"));

		verify(query).setLockMode(LockModeType.READ);
		verify(query).setHint("foo", "bar");
	}
}
