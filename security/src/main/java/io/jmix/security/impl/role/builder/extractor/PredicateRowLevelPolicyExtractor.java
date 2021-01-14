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

package io.jmix.security.impl.role.builder.extractor;

import io.jmix.core.Metadata;
import io.jmix.core.common.util.ReflectionHelper;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.security.model.RowLevelPolicy;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

@Component("sec_InMemoryRowLevelPolicyExtractor")
public class PredicateRowLevelPolicyExtractor implements RowLevelPolicyExtractor {

    protected Metadata metadata;
    protected ConcurrentMap<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();

    @Autowired
    public PredicateRowLevelPolicyExtractor(Metadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public Collection<RowLevelPolicy> extractRowLevelPolicies(Method method) {
        Set<RowLevelPolicy> policies = new HashSet<>();
        //todo MG check parameter types
        PredicateRowLevelPolicy[] annotations = method.getAnnotationsByType(PredicateRowLevelPolicy.class);
        for (PredicateRowLevelPolicy annotation : annotations) {
            for (RowLevelPolicyAction action : annotation.actions()) {
                Class<?> entityClass = annotation.entityClass();
                MetaClass metaClass = metadata.getClass(entityClass);
                Predicate<Object> predicate;
                try {
                    if (Modifier.isStatic(method.getModifiers())) {
                        //noinspection unchecked
                        predicate = (Predicate<Object>) method.invoke(null);
                    } else {
                        Object proxyObject = proxyCache.computeIfAbsent(method.getDeclaringClass(), this::createProxy);
                        //noinspection unchecked
                        predicate = (Predicate<Object>) method.invoke(proxyObject);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException("Cannot evaluate row level policy predicate", e);
                }
                RowLevelPolicy rowLevelPolicy = new RowLevelPolicy(metaClass.getName(),
                        action,
                        predicate,
                        Collections.singletonMap("uniqueKey", UUID.randomUUID().toString()));
                policies.add(rowLevelPolicy);
            }
        }
        return policies;
    }

    protected Object createProxy(Class<?> ownerClass) {
        if (ownerClass.isInterface()) {
            ClassLoader classLoader = ownerClass.getClassLoader();
            return Proxy.newProxyInstance(classLoader, new Class[]{ownerClass},
                    (proxy, method, args) -> invokeProxyMethod(ownerClass, proxy, method, args));
        } else {
            try {
                return ReflectionHelper.newInstance(ownerClass);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(String.format("Cannot create Role [%s] proxy", ownerClass), e);
            }
        }
    }

    @Nullable
    protected Object invokeProxyMethod(Class<?> ownerClass, Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            try {
                if (SystemUtils.IS_JAVA_1_8) {
                    // hack to invoke default method of an interface reflectively
                    Constructor<MethodHandles.Lookup> lookupConstructor =
                            MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
                    if (!lookupConstructor.isAccessible()) {
                        lookupConstructor.setAccessible(true);
                    }
                    return lookupConstructor.newInstance(ownerClass, MethodHandles.Lookup.PRIVATE)
                            .unreflectSpecial(method, ownerClass)
                            .bindTo(proxy)
                            .invokeWithArguments(args);
                } else {
                    return MethodHandles.lookup()
                            .findSpecial(ownerClass, method.getName(), MethodType.methodType(method.getReturnType(),
                                    method.getParameterTypes()), ownerClass)
                            .bindTo(proxy)
                            .invokeWithArguments(args);
                }
            } catch (Throwable throwable) {
                throw new RuntimeException("Error invoking default method of Role interface", throwable);
            }
        } else {
            return null;
        }
    }
}
