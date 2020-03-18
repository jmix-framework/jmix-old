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
package io.jmix.data.entity;

import io.jmix.core.Entity;
import io.jmix.core.metamodel.annotations.MetaClass;
import io.jmix.core.entity.annotation.UnavailableInSecurityConstraints;

import javax.persistence.*;

/**
 * Base class for entities with Long Identity identifier.
 */
@MappedSuperclass
@MetaClass(name = "sys$BaseIdentityIdEntity")
@UnavailableInSecurityConstraints
public abstract class BaseIdentityIdEntity implements Entity<Long> {

    private static final long serialVersionUID = 3083677558630811496L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    protected Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}