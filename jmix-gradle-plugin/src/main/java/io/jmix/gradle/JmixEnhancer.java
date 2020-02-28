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

package io.jmix.gradle;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enhances entity classes: setters fire propertyChange events, messages in BeanValidation annotations.
 */
public class JmixEnhancer {

    protected static final String ENHANCED_TYPE = "io.jmix.core.entity.JmixEnhanced";
    protected static final String ENHANCED_DISABLED_TYPE = "io.jmix.core.entity.JmixEnhancingDisabled";

    protected static final String METAPROPERTY_ANNOTATION = "io.jmix.core.metamodel.annotations.MetaProperty";
    protected static final String ENTITY_TYPE = "io.jmix.core.entity.Entity";

    protected Logger log;

    protected ClassPool pool;
    protected String outputDir;

    public JmixEnhancer(ClassPool pool, String outputDir) {
        this.pool = pool;
        this.outputDir = outputDir;
    }

    public void setLogger(Logger log) {
        this.log = log;
    }

    public void run(String className) {
        try {
            CtClass cc = pool.get(className);

            if (!isEntityClass(cc)) {
                log.info("[JmixEnhancer] " + className + " is not an Entity and should not be enhanced");
                return;
            }

            for (CtClass intf : cc.getInterfaces()) {
                if (intf.getName().equals(ENHANCED_TYPE)
                        || intf.getName().equals(ENHANCED_DISABLED_TYPE)) {
                    log.info("[JmixEnhancer] " + className + " has already been enhanced or should not be enhanced at all");
                    return;
                }
            }

            log.info("[JmixEnhancer] enhancing " + className);
            enhanceSetters(cc);

            enhanceBeanValidationMessages(cc);

            makeAutogeneratedAccessorsProtected(cc);

            cc.addInterface(pool.get(ENHANCED_TYPE));
            cc.writeFile(outputDir);
        } catch (NotFoundException | IOException | CannotCompileException e) {
            throw new RuntimeException("Error enhancing class " + className + ": " + e, e);
        }
    }

    protected void enhanceSetters(CtClass ctClass) throws NotFoundException, CannotCompileException {
        for (CtMethod ctMethod : ctClass.getDeclaredMethods()) {
            final String name = ctMethod.getName();
            if (Modifier.isAbstract(ctMethod.getModifiers())
                    || !name.startsWith("set")
                    || ctMethod.getReturnType() != CtClass.voidType
                    || ctMethod.getParameterTypes().length != 1)
                continue;

            String fieldName = StringUtils.uncapitalize(name.substring(3));

            // check if the setter is for a persistent or transient property
            CtMethod persistenceMethod = null;
            for (CtMethod method : ctClass.getDeclaredMethods()) {
                if (method.getName().equals("_persistence_set_" + fieldName)) {
                    persistenceMethod = method;
                    break;
                }
            }
            if (persistenceMethod == null) {
                // can be a transient property
                CtField ctField = null;
                CtField[] declaredFields = ctClass.getDeclaredFields();
                for (CtField field : declaredFields) {
                    if (field.getName().equals(fieldName)) {
                        ctField = field;
                        break;
                    }
                }
                if (ctField == null)
                    continue; // no field
                // check if the field is annotated with @MetaProperty
                // cannot use ctField.getAnnotation() because of problem with classpath in child projects
                AnnotationsAttribute annotationsAttribute =
                        (AnnotationsAttribute) ctField.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
                if (annotationsAttribute == null || annotationsAttribute.getAnnotation(METAPROPERTY_ANNOTATION) == null)
                    continue;
            }

            CtClass setterParamType = ctMethod.getParameterTypes()[0];

            if (setterParamType.isPrimitive()) {
                throw new IllegalStateException(
                        String.format("Unable to enhance field %s.%s with primitive type %s. Use type %s.",
                                ctClass.getName(), fieldName,
                                setterParamType.getSimpleName(), StringUtils.capitalize(setterParamType.getSimpleName())));
            }

            ctMethod.addLocalVariable("__prev", setterParamType);
            ctMethod.addLocalVariable("__new", setterParamType);

            ctMethod.insertBefore(
                    "__prev = this.get" + StringUtils.capitalize(fieldName) + "();"
            );

            ctMethod.insertAfter(
                    "__new = this.get" + StringUtils.capitalize(fieldName) + "();" +
                            "if (!io.jmix.core.metamodel.model.utils.InstanceUtils.propertyValueEquals(__prev, __new)) {" +
                            "  this.__getEntityEntry().firePropertyChanged(\"" + fieldName + "\", __prev, __new);" +
                            "}"
            );
        }
    }

    protected void enhanceBeanValidationMessages(CtClass ctClass) {
        ClassFile ccFile = ctClass.getClassFile();
        ConstPool constpool = ccFile.getConstPool();

        for (CtField field : ctClass.getDeclaredFields()) {

            if (field.getAttribute(AnnotationsAttribute.visibleTag) == null) {
                continue;
            }

            AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag,
                    field.getAttribute(AnnotationsAttribute.visibleTag));

            Annotation[] annotations = attr.getAnnotations();
            for (Annotation annotation : annotations) {

                if (annotation.getMemberNames() == null) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<String> names = new ArrayList<>(annotation.getMemberNames());
                for (String name : names) {
                    if (name.equals("message")) {
                        MemberValue memberValue = annotation.getMemberValue(name);
                        if (memberValue instanceof StringMemberValue) {
                            String messageValue = ((StringMemberValue) memberValue).getValue();
                            BeanValidationMessageTransformer transformer = new BeanValidationMessageTransformer();
                            String transformedMessage = transformer.transformAnnotationMessage(messageValue, ctClass.getPackageName());

                            if (!StringUtils.equals(messageValue, transformedMessage)) {
                                annotation.addMemberValue("message", new StringMemberValue(transformedMessage, constpool));
                                log.debug(String.format("Class: %s, field: %s, annotation: %s changed value from %s to %s",
                                        ctClass.getName(), field.getName(), annotation.getTypeName(), messageValue, transformedMessage));
                            }
                        }
                        break;
                    }
                }
            }
            attr.setAnnotations(annotations);
            field.setAttribute(AnnotationsAttribute.visibleTag, attr.get());
        }
    }

    protected void makeAutogeneratedAccessorsProtected(CtClass ctClass) {
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            if (method.getName().startsWith("_persistence_get_")
                    || method.getName().startsWith("_persistence_set_")) {
                method.setModifiers(Modifier.setProtected(method.getModifiers()));

                log.debug("Set protected modifier for " + method.getLongName());
            }
        }
    }

    protected boolean isEntityClass(CtClass ctClass) throws NotFoundException {
        for (CtClass ctInterface : ctClass.getInterfaces()) {
            if (Objects.equals(ctInterface.getName(), ENTITY_TYPE)) {
                return true;
            }
        }
        CtClass ctSuperclass = ctClass.getSuperclass();
        while (ctSuperclass != null) {
            for (CtClass ctInterface : ctSuperclass.getInterfaces()) {
                if (Objects.equals(ctInterface.getName(), ENTITY_TYPE)) {
                    return true;
                }
            }
            ctSuperclass = ctSuperclass.getSuperclass();
        }
        return false;
    }
}
