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

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.io.FileUtil;

import java.io.File;

/**
 * Starts and stops the persistent sequential store for the tests of that store, as tinkar-core's
 * integration suite did before the store left it (IKE-Network/ike-issues#1313).
 */
final class SequentialStore {

    private SequentialStore() {
    }

    /**
     * Starts a new store in {@code root}. A store an earlier run left there is removed first, so a
     * test never reads data it did not write.
     */
    static void start(File root) {
        if (root.exists()) {
            FileUtil.recursiveDelete(root);
        }
        open(root);
    }

    /**
     * Opens the store already in {@code root}.
     *
     * @throws IllegalStateException if there is no store there to reopen
     */
    static void reopen(File root) {
        if (!root.isDirectory()) {
            throw new IllegalStateException("No datastore to reopen at " + root.getAbsolutePath());
        }
        open(root);
    }

    static void stop() {
        PrimitiveData.stop();
    }

    private static void open(File root) {
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, root);
        PrimitiveData.selectControllerByName(SpinedArrayProvider.OpenController.CONTROLLER_NAME);
        PrimitiveData.start();
    }
}
