/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
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
package dev.ikm.tinkar.provider.spinedarray;

import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.util.io.FileUtil;
import dev.ikm.tinkar.entity.ConceptEntity;
import dev.ikm.tinkar.entity.ConceptRecord;
import dev.ikm.tinkar.entity.EntityHandle;
import dev.ikm.tinkar.entity.EntityService;
import dev.ikm.tinkar.entity.SemanticEntity;
import dev.ikm.tinkar.entity.SemanticRecord;
import dev.ikm.tinkar.entity.StampEntity;
import dev.ikm.tinkar.entity.StampRecord;
import dev.ikm.tinkar.entity.load.LoadEntitiesFromProtobufFile;
import dev.ikm.tinkar.entity.transform.EntityToTinkarSchemaTransformer;
import dev.ikm.tinkar.fixtures.ForkedJvm;
import dev.ikm.tinkar.fixtures.WithKeyValueProvider;
import dev.ikm.tinkar.fixtures.TestConstants;
import dev.ikm.tinkar.schema.TinkarMsg;
import dev.ikm.tinkar.terms.KernelConcept;
import dev.ikm.tinkar.terms.KernelPattern;
import dev.ikm.tinkar.terms.State;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One pass over a change set with a forward reference, on the sequential store: a description
 * semantic written before the concept it references imports in a single pass, because the
 * sequential store mints a nid for a UUID before it knows the component's kind. A store whose
 * nids name their pattern cannot, and asks for the identity pass instead
 * ({@code requiresMultiPassImport}); tinkar-core's {@code ForwardReferenceChangeSetIT} keeps the
 * multi-pass import on the 64-bit store. Moved here with the store (IKE-Network/ike-issues#1313).
 *
 * <p>The change set is written first, by the {@link Generate} stage in a JVM and store of its
 * own, so that its concept and semantic do not exist in this suite's store when the import
 * begins. It holds the STAMP both versions cite, the description semantic, and the concept the
 * semantic references, written after it.
 */
@WithKeyValueProvider(controllerClass = SpinedArrayProvider.OpenController.class, cleanOnStart = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SequentialForwardReferenceIT {
    private static final Logger LOG = LoggerFactory.getLogger(SequentialForwardReferenceIT.class);

    private static final File WORK = TestConstants.createFilePathInTargetFromClassName.apply(SequentialForwardReferenceIT.class);

    /** Deterministic, so the generating stage and the import tests agree without sharing anything else. */
    static final UUID CONCEPT_UUID = UUID.nameUUIDFromBytes("forward-ref-test-concept".getBytes());
    static final UUID SEMANTIC_UUID = UUID.nameUUIDFromBytes("forward-ref-test-semantic".getBytes());

    private static final String STORE = "store";
    private static final String CHANGESET_FILE = "changeset.file";

    private File changesetFile;
    private final PublicId newConceptPublicId = PublicIds.of(CONCEPT_UUID);
    private final PublicId descriptionSemanticPublicId = PublicIds.of(SEMANTIC_UUID);

    @BeforeAll
    void generateChangeSet() {
        FileUtil.recursiveDelete(WORK);
        assertTrue(WORK.mkdirs(), "Could not create " + WORK);
        Properties in = new Properties();
        in.setProperty(STORE, new File(WORK, "generate-store").getPath());
        in.setProperty(CHANGESET_FILE, new File(WORK, "forward-reference-changeset.zip").getPath());
        Properties out = ForkedJvm.run(Generate.class, in);
        changesetFile = new File(out.getProperty(CHANGESET_FILE));
        assertTrue(changesetFile.exists(), "Changeset file should be created");
    }

    @BeforeEach
    void beforeEach() {
        new LoadEntitiesFromProtobufFile(TestConstants.PB_STARTER_DATA_REASONED).compute();
    }

    /**
     * The semantic is written before the concept it references, and the sequential store mints
     * the concept's nid when the semantic refers to it, without requiring the entity to exist.
     */
    @Test
    @DisplayName("1-pass import should succeed with forward reference")
    void testOnePassImportSucceedsWithForwardReference() {
        LOG.info("Testing 1-pass import with forward reference - expecting success");

        // Create loader with 1-pass mode (useTwoPassImport = false)
        LoadEntitiesFromProtobufFile loader = new LoadEntitiesFromProtobufFile(changesetFile, false);

        // Should succeed - Entity.nid() assigns NIDs without requiring entity to exist
        var summary = loader.compute();
        LOG.info("1-pass import succeeded: {}", summary);
        assertNotNull(summary);

        // Verify both entities were loaded
        ConceptEntity loadedConcept = EntityHandle.get(newConceptPublicId).expectConcept();
        SemanticEntity loadedSemantic = EntityHandle.get(descriptionSemanticPublicId).expectSemantic();
        assertNotNull(loadedConcept, "New concept should be loaded");
        assertNotNull(loadedSemantic, "Description semantic should be loaded");
    }

    /**
     * Writes a protobuf changeset with a forward reference: the description semantic
     * BEFORE the concept it references. Runs in a store of its own, with the starter data
     * loaded so the transformer can resolve the semantic's references.
     */
    static class Generate implements ForkedJvm.Stage {
        @Override
        public void run(Properties in, Properties out) throws Exception {
            out.putAll(in);
            SequentialStore.start(new File(in.getProperty(STORE)));
            try {
                new LoadEntitiesFromProtobufFile(TestConstants.PB_STARTER_DATA_REASONED).compute();
                StampEntity testStamp = StampRecord.make(
                        UUID.randomUUID(),
                        State.ACTIVE,
                        System.currentTimeMillis(),
                        KernelConcept.USER.publicId(),
                        KernelConcept.PRIMORDIAL_MODULE.publicId(),
                        KernelConcept.DEVELOPMENT_PATH.publicId()
                );
                EntityService.get().putEntity(testStamp);
                File changesetFile = new File(in.getProperty(CHANGESET_FILE));
                createChangeSetWithForwardReference(changesetFile, testStamp);
                LOG.info("Created changeset with forward reference at: {}", changesetFile.getAbsolutePath());
            } finally {
                SequentialStore.stop();
            }
        }

        private static void createChangeSetWithForwardReference(File changesetFile, StampEntity testStamp)
                throws IOException {
            try (FileOutputStream fos = new FileOutputStream(changesetFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                writeManifest(zos);

                ZipEntry dataEntry = new ZipEntry("changeset.pb");
                zos.putNextEntry(dataEntry);

                EntityToTinkarSchemaTransformer transformer = EntityToTinkarSchemaTransformer.getInstance();

                ConceptRecord newConcept = ConceptRecord.build(PublicIds.of(CONCEPT_UUID), testStamp.lastVersion());
                SemanticRecord descriptionSemantic = createDescriptionSemantic(testStamp);

                // Put entities into EntityService so transformer can resolve references.
                // They are written to the changeset in forward reference order (semantic before concept).
                EntityService.get().putEntity(newConcept);
                EntityService.get().putEntity(descriptionSemantic);

                // The STAMP both versions cite, so the import holds everything they reference.
                transformer.transform(testStamp).writeDelimitedTo(zos);

                // FORWARD REFERENCE: Write description semantic BEFORE the concept
                TinkarMsg semanticMsg = transformer.transform(descriptionSemantic);
                semanticMsg.writeDelimitedTo(zos);
                LOG.info("Wrote description semantic BEFORE concept (forward reference)");

                // Write the new concept AFTER the semantic that references it
                TinkarMsg conceptMsg = transformer.transform(newConcept);
                conceptMsg.writeDelimitedTo(zos);
                LOG.info("Wrote concept AFTER description semantic");

                zos.closeEntry();
            }
        }

        /**
         * A description semantic that references the new concept, using the
         * DESCRIPTION_PATTERN from starter data.
         */
        private static SemanticRecord createDescriptionSemantic(StampEntity testStamp) {
            // Description pattern fields: language, text, case significance, description type
            ImmutableList<Object> fieldValues = org.eclipse.collections.api.factory.Lists.immutable.of(
                    KernelConcept.ENGLISH_LANGUAGE.nid(),
                    "Test Description for Forward Reference",
                    KernelConcept.DESCRIPTION_NOT_CASE_SENSITIVE.nid(),
                    KernelConcept.REGULAR_NAME_DESCRIPTION_TYPE.nid()
            );
            // The nid of a concept that is written AFTER this semantic
            long conceptNid = EntityService.get().nidForPublicId(PublicIds.of(CONCEPT_UUID));
            return SemanticRecord.build(
                    SEMANTIC_UUID,
                    KernelPattern.DESCRIPTION_PATTERN.nid(),
                    conceptNid,
                    testStamp.lastVersion(),
                    fieldValues
            );
        }

        private static void writeManifest(ZipOutputStream zos) throws IOException {
            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().putValue("Total-Count", "3"); // 1 STAMP + 1 semantic + 1 concept
            ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
            zos.putNextEntry(manifestEntry);
            manifest.write(zos);
            zos.closeEntry();
        }
    }
}
