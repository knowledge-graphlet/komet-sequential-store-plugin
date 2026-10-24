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
import dev.ikm.tinkar.common.service.DataServiceController;
import dev.ikm.tinkar.common.service.LoadDataFromFileController;
import dev.ikm.tinkar.common.service.ServiceLifecycle;
import dev.ikm.tinkar.entity.ChangeSetWriterService;
import dev.ikm.tinkar.provider.spinedarray.SpinedArrayProvider;

/**
 * The sequential spined-array store, the 32-bit store Komet used before the 64-bit spined array,
 * as a Komet plugin: staged into the runtime {@code plugins/} layer so a store on disk can still
 * be opened, to migrate it to the 64-bit store (IKE-Network/ike-issues#1313). The module keeps its
 * tinkar-core name, to which {@code dev.ikm.tinkar.common} exports its internal service package.
 * The spined maps it keeps its data in, once tinkar-core's {@code collection} module, are its own
 * and not exported.
 */
@SuppressWarnings("module")
        // 7 in HL7 is not a version reference
module dev.ikm.tinkar.provider.spinedarray {
    requires org.slf4j;
    requires dev.ikm.tinkar.common;
    requires dev.ikm.tinkar.component;
    requires dev.ikm.tinkar.entity;
    requires dev.ikm.tinkar.provider.search;
    requires org.eclipse.collections.api;
    requires org.eclipse.collections.impl;
    requires dev.ikm.jpms.activej.bytebuf;

    exports dev.ikm.tinkar.provider.spinedarray;
    exports dev.ikm.tinkar.provider.spinedarray.constants;

    provides DataServiceController
            with SpinedArrayProvider.OpenController, SpinedArrayProvider.NewController, SpinedArrayProvider.LoadController;
    provides ServiceLifecycle
            with SpinedArrayProvider.OpenController, SpinedArrayProvider.NewController, SpinedArrayProvider.LoadController;

    uses LoadDataFromFileController;
    uses ChangeSetWriterService;

}
