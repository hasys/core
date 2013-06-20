/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.interceptor.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.interceptor.spi.model.InterceptionType;
import org.jboss.weld.serialization.MethodHolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * This impl is immutable provided the type of the intercetped entity is immutable as well.
 *
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 * @author Martin Kouba
 *
 * @param <T> the type of the intercepted entity
 */
class InterceptionModelImpl<T> implements InterceptionModel<T> {

    private final Map<InterceptionType, List<InterceptorMetadata<?>>> globalInterceptors;

    private final Map<InterceptionType, Map<MethodHolder, List<InterceptorMetadata<?>>>> methodBoundInterceptors;

    private final Set<MethodHolder> methodsIgnoringGlobalInterceptors;

    private final Set<InterceptorMetadata<?>> allInterceptors;

    private final T interceptedEntity;

    private final boolean hasTargetClassInterceptors;

    private final boolean hasExternalNonConstructorInterceptors;

    /**
     *
     * @param builder
     */
    InterceptionModelImpl(InterceptionModelBuilder<T> builder) {
        this.interceptedEntity = builder.getInterceptedEntity();
        this.hasTargetClassInterceptors = builder.isHasTargetClassInterceptors();
        this.hasExternalNonConstructorInterceptors = builder.isHasExternalNonConstructorInterceptors();
        this.globalInterceptors = ImmutableMap.<InterceptionType, List<InterceptorMetadata<?>>>copyOf(builder.getGlobalInterceptors());
        this.methodBoundInterceptors = ImmutableMap.<InterceptionType, Map<MethodHolder,List<InterceptorMetadata<?>>>>copyOf(builder.getMethodBoundInterceptors());
        this.methodsIgnoringGlobalInterceptors = ImmutableSet.<MethodHolder>copyOf(builder.getMethodsIgnoringGlobalInterceptors());
        this.allInterceptors = ImmutableSet.<InterceptorMetadata<?>>copyOf(builder.getAllInterceptors());
    }

    public List<InterceptorMetadata<?>> getInterceptors(InterceptionType interceptionType, Method method) {
        if (InterceptionType.AROUND_CONSTRUCT.equals(interceptionType)) {
            throw new IllegalStateException("Cannot use getInterceptors() for @AroundConstruct interceptor lookup. Use getConstructorInvocationInterceptors() instead.");
        }
        if (interceptionType.isLifecycleCallback() && method != null) {
            throw new IllegalArgumentException("On a lifecycle callback, the associated method must be null");
        }

        if (!interceptionType.isLifecycleCallback() && method == null) {
            throw new IllegalArgumentException("Around-invoke and around-timeout interceptors are defined for a given method");
        }

        if (interceptionType.isLifecycleCallback()) {
            if (globalInterceptors.containsKey(interceptionType)) {
                return globalInterceptors.get(interceptionType);
            }
        } else {
            MethodHolder methodHolder = MethodHolder.of(method);
            ArrayList<InterceptorMetadata<?>> returnedInterceptors = new ArrayList<InterceptorMetadata<?>>();
            if (!methodsIgnoringGlobalInterceptors.contains(methodHolder) && globalInterceptors.containsKey(interceptionType)) {
                returnedInterceptors.addAll(globalInterceptors.get(interceptionType));
            }
            Map<MethodHolder, List<InterceptorMetadata<?>>> map = methodBoundInterceptors.get(interceptionType);
            if (map != null) {
                List<InterceptorMetadata<?>> list = map.get(methodHolder);
                if (list != null) {
                    returnedInterceptors.addAll(list);
                }
            }
            return returnedInterceptors;
        }
        return Collections.emptyList();
    }

    public Set<InterceptorMetadata<?>> getAllInterceptors() {
        return Collections.unmodifiableSet(allInterceptors);
    }

    public T getInterceptedEntity() {
        return this.interceptedEntity;
    }

    @Override
    public List<InterceptorMetadata<?>> getConstructorInvocationInterceptors() {
        if (globalInterceptors.containsKey(InterceptionType.AROUND_CONSTRUCT)) {
            return globalInterceptors.get(InterceptionType.AROUND_CONSTRUCT);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasExternalConstructorInterceptors() {
        return !getConstructorInvocationInterceptors().isEmpty();
    }

    @Override
    public boolean hasExternalNonConstructorInterceptors() {
        return hasExternalNonConstructorInterceptors;
    }

    @Override
    public boolean hasTargetClassInterceptors() {
        return hasTargetClassInterceptors;
    }
}
