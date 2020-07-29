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

package io.jmix.data.persistence;

import io.jmix.core.JmixEntity;
import io.jmix.core.MetadataTools;
import org.eclipse.persistence.expressions.ExpressionOperator;
import org.eclipse.persistence.internal.expressions.ExpressionSQLPrinter;
import org.eclipse.persistence.internal.expressions.QueryKeyExpression;
import org.eclipse.persistence.internal.helper.ClassConstants;
import org.eclipse.persistence.internal.helper.CubaUtil;

import java.io.IOException;
import java.util.Objects;
import java.util.Vector;

public class JmixIsNullExpressionOperator extends ExpressionOperator {

    private MetadataTools metadataTools;

    public JmixIsNullExpressionOperator(MetadataTools metadataTools) {
        setType(ExpressionOperator.ComparisonOperator);
        setSelector(ExpressionOperator.IsNull);
        Vector v = org.eclipse.persistence.internal.helper.NonSynchronizedVector.newInstance();
        v.add("(");
        v.add(" IS NULL)");
        printsAs(v);
        bePrefix();
        printsJavaAs(".isNull()");
        setNodeClass(ClassConstants.FunctionExpression_Class);
        this.metadataTools = metadataTools;
    }

    @Override
    public void printCollection(Vector items, ExpressionSQLPrinter printer) {
        if (items.size() == 1 && items.get(0) instanceof QueryKeyExpression && !CubaUtil.isSoftDeletion()) {
            //noinspection unchecked
            Class<? extends JmixEntity> clazz = ((QueryKeyExpression) items.get(0)).getContainingDescriptor().getJavaClass();

            String deletedDateFieldName = metadataTools.getDeletedDateProperty(clazz);
            if (Objects.equals(deletedDateFieldName, ((QueryKeyExpression) items.get(0)).getName())) {
                try {
                    printer.getWriter().write("(0=0)");
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        super.printCollection(items, printer);
    }
}