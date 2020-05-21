/*
 * Copyright 2020 Haulmont.
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

package io.jmix.core;

import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;

import java.util.Collection;

/**
 * Provide instance name and instance name properties.
 */
public interface InstanceNameProvider {

    String NAME = "jmix_InstanceNameProvider";

    /**
     * Get entity instance name defined by {@link InstanceName} annotation
     *
     * @param instance   instance
     *
     * @return instance name
     */
    String getInstanceName(Entity instance);

    /**
     * Return a collection of properties included into entity's name pattern (see {@link InstanceName}).
     *
     * @param metaClass   entity metaclass
     * @param useOriginal if true, and if the given metaclass doesn't define a {@link InstanceName} and if it is an
     *                    extended entity, this method tries to find a name pattern in an original entity
     * @return collection of the name pattern properties
     */
    Collection<MetaProperty> getInstanceNameRelatedProperties(MetaClass metaClass, boolean useOriginal);
}
