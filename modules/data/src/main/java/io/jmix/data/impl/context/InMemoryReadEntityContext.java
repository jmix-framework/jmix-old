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

package io.jmix.data.impl.context;

import com.google.common.collect.Iterables;
import io.jmix.core.Entity;
import io.jmix.core.context.AccessContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryReadEntityContext implements AccessContext {
    protected final List<Entity> entities;
    protected List<Entity> permittedEntities;

    public InMemoryReadEntityContext(List<Entity> entities) {
        this.entities = entities;
    }

    public InMemoryReadEntityContext(Entity entity) {
        this(Arrays.asList(entity));
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void addDeniedEntity(Entity entity) {
        if (permittedEntities == null) {
            permittedEntities = new ArrayList<>(entities.size());
        }
        permittedEntities.add(entity);
    }

    public List<Entity> getPermittedEntities() {
        return permittedEntities == null ? entities : permittedEntities;
    }

    public Entity getPermittedEntity() {
        return Iterables.getFirst(getPermittedEntities(), null);
    }
}
