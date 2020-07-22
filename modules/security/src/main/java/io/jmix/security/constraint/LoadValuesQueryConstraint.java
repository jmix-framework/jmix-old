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

package io.jmix.security.constraint;

import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.PermissionType;
import io.jmix.data.impl.context.LoadValuesQueryContext;
import io.jmix.security.model.EntityPolicyAction;
import org.springframework.stereotype.Component;

@Component(LoadValuesQueryConstraint.NAME)
public class LoadValuesQueryConstraint extends AbstractEntityOperationConstraint<LoadValuesQueryContext> {
    public static final String NAME = "sec_LoadValuesQueryConstraint";

    public LoadValuesQueryConstraint(CurrentAuthentication currentAuthentication) {
        super(currentAuthentication);
    }

    @Override
    public Class<LoadValuesQueryContext> getContextType() {
        return LoadValuesQueryContext.class;
    }

    @Override
    public void applyTo(LoadValuesQueryContext context) {
        for (MetaClass entityClass : context.getEntityClasses()) {
            if (!isEntityOpPermitted(entityClass, EntityPolicyAction.READ)) {
                context.setDenied();
                return;
            }
        }

        for (MetaPropertyPath propertyPath : context.getAllPropertyPaths()) {
            if (!isEntityAttrViewPermitted(propertyPath)) {
                throw new AccessDeniedException(PermissionType.ENTITY_ATTR, propertyPath.getMetaClass() + "." + propertyPath.toPathString());
            }
        }

        for (MetaPropertyPath propertyPath : context.getSelectedPropertyPaths()) {
            if (!isEntityAttrViewPermitted(propertyPath)) {
                context.addDeniedSelectedIndex(context.getSelectedIndex(propertyPath));
            }
        }
    }
}
