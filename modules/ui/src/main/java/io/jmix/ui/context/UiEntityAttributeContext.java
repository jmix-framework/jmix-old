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

package io.jmix.ui.context;

import io.jmix.core.context.AccessContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaPropertyPath;

//security.isEntityAttrUpdatePermitted(metaPropertyPath)
//security.isEntityAttrUpdatePermitted(metaClass, propertyId.toString()))

//security.isEntityAttrReadPermitted(metaPropertyPath)
//security.isEntityAttrReadPermitted(metaPropertyPath)
//security.isEntityAttrReadPermitted(masterContainer.getEntityMetaClass(), property)
//security.isEntityAttrReadPermitted(masterContainer.getEntityMetaClass(), property)
//isEntityAttrReadPermitted(metaClass, propertyPath.toString()
public class UiEntityAttributeContext implements AccessContext {
    public UiEntityAttributeContext(MetaPropertyPath metaPropertyPath) {

    }

    public UiEntityAttributeContext(MetaClass metaClass, String property) {

    }

    public boolean isModifyPermitted() {
        return false;
    }

    public boolean isViewPermitted() {
        return false;
    }
}
