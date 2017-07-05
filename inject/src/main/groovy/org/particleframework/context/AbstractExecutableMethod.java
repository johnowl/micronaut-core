/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.particleframework.context;

import org.particleframework.core.annotation.AnnotationUtil;
import org.particleframework.core.annotation.Internal;
import org.particleframework.core.reflect.ReflectionUtils;
import org.particleframework.inject.Argument;
import org.particleframework.inject.BeanDefinition;
import org.particleframework.inject.ExecutableMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>Abstract base class for generated {@link ExecutableMethod} classes to implement. The generated classes should implement
 * the {@link ExecutableMethod#invoke(Object, Object...)} method at compile time providing direct dispatch of the target method</p>
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Internal
public abstract class AbstractExecutableMethod implements ExecutableMethod {

    private final String methodName;
    private final Argument[] arguments;
    private final Class declaringType;

    protected AbstractExecutableMethod(Method method,
                                       Map<String, Class> arguments,
                                       Map<String, Annotation> qualifiers,
                                       Map<String, List<Class>> genericTypes) {
        this.methodName = method.getName();
        this.declaringType = method.getDeclaringClass();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        this.arguments = DefaultArgument.from(arguments, qualifiers, genericTypes, index -> {
            if (index < parameterAnnotations.length) {
                return parameterAnnotations[index];
            }
            return AnnotationUtil.ZERO_ANNOTATIONS;
        });
    }

    @Override
    public Class[] getArgumentTypes() {
        return Arrays.stream(arguments)
                .map(Argument::getType)
                .toArray(Class[]::new);
    }

    @Override
    public Class getDeclaringType() {
        return declaringType;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public Argument[] getArguments() {
        return arguments;
    }

    @Override
    public final Object invoke(Object instance, Object... arguments) {
        validateArguments(arguments);
        return invokeInternal(instance, arguments);
    }

    private void validateArguments(Object[] argArray) {
        int requiredCount = this.arguments.length;
        int actualCount = argArray == null ? 0 : argArray.length;
        if(requiredCount != actualCount) {
            throw new IllegalArgumentException("Wrong number of arguments to method: " + methodName);
        }
        if(requiredCount > 0) {
            for (int i = 0; i < arguments.length; i++) {
                Argument argument = arguments[i];
                Class type = ReflectionUtils.getWrapperType(argument.getType());
                Object value = argArray[i];
                if(value != null && !type.isInstance(value)) {
                    throw new IllegalArgumentException("Invalid type ["+argArray[i].getClass().getName()+"] for argument ["+argument+"] of method: " + methodName);
                }
            }
        }
    }

    protected abstract Object invokeInternal(Object instance, Object[] arguments);
}
