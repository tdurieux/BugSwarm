/* This file is part of the OWL API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright 2014, The University of Manchester
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License. */
package org.semanticweb.owlapi.api.test.syntax;

import static org.junit.Assert.*;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.IRI;

import java.io.File;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.util.AutoIRIMapper;

@SuppressWarnings("javadoc")
public class ManchesterImportTestCase extends TestBase {

    @Nonnull private final String str = "http://owlapitestontologies.com/thesuperont";
    @Nonnull private final String superpath = "/imports/thesuperont.omn";
    @Nonnull private static final File RESOURCES;

    static {
        File f = new File("contract/src/test/resources/");
        if (f.exists()) {
            RESOURCES = f;
        } else {
            f = new File("src/test/resources/");
            if (f.exists()) {
                RESOURCES = f;
            } else {
                throw new OWLRuntimeException("ManchesterImportTestCase: NO RESOURCE FOLDER ACCESSIBLE");
            }
        }
    }

    @Test
    public void testManualImports() throws OWLOntologyCreationException {
        OWLOntologyManager manager = getManager();
        manager.loadOntologyFromOntologyDocument(new File(RESOURCES, superpath));
        assertNotNull(manager.getOntology(IRI(str)));
    }

    private static OWLOntologyManager getManager() {
        OWLOntologyManager manager = setupManager();
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        manager.getIRIMappers().add(mapper);
        return manager;
    }

    @Test
    public void testRemoteIsParseable() throws OWLOntologyCreationException {
        OWLOntologyManager manager = getManager();
        IRI iri = IRI(str);
        OWLOntology ontology = manager.loadOntology(iri);
        assertEquals(1, ontology.getAxioms().size());
        assertEquals(ontology.getOntologyID().getOntologyIRI().get(), iri);
        assertNotNull(manager.getOntology(iri));
    }

    @Test
    public void testEquivalentLoading() throws OWLOntologyCreationException {
        OWLOntologyManager managerStart = getManager();
        OWLOntology manualImport = managerStart.loadOntologyFromOntologyDocument(new File(RESOURCES, superpath));
        OWLOntologyManager managerTest = getManager();
        OWLOntology iriImport = managerTest.loadOntology(IRI(str));
        assertEquals(manualImport.getAxioms(), iriImport.getAxioms());
        assertEquals(manualImport.getOntologyID(), iriImport.getOntologyID());
    }

    @Test
    public void testImports() throws OWLOntologyCreationException {
        OWLOntologyManager manager = getManager();
        String subpath = "/imports/thesubont.omn";
        manager.loadOntologyFromOntologyDocument(new File(RESOURCES, subpath));
    }
}
