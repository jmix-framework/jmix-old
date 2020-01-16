/*
 * Copyright 2019 Haulmont.
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

package io.jmix.core.metamodel.model;

import java.util.Collection;

/**
 * Container and entry point for metadata objects
 *
 */
public interface MetaModel extends MetadataObject {

    /**
     * Get MetaClass by its unique name
     * @return MetaClass instance, null if not found
     */
    MetaClass getClass(String name);

    /**
     * Get MetaClass by corresponding entity's Java class
     * @return MetaClass instance, null if not found
     */
    MetaClass getClass(Class<?> clazz);

    /**
     * All meta classes
     */
    Collection<MetaClass> getClasses();
}