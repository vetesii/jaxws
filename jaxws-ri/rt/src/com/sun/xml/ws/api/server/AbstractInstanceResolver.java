package com.sun.xml.ws.api.server;

import com.sun.istack.Nullable;
import com.sun.xml.ws.resources.ServerMessages;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.util.localization.Localizable;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Partial implementation of {@link InstanceResolver} with
 * convenience methods to do the resource injection.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class AbstractInstanceResolver<T> extends InstanceResolver<T> {

    /**
     * Encapsulates which field/method the injection is done,
     * and performs the injection.
     */
    protected static interface InjectionPlan<T,R> {
        void inject(T instance,R resource);
    }

    /**
     * Injects to a field.
     */
    private static class FieldInjectionPlan<T,R> implements InjectionPlan<T,R> {
        private final Field field;

        public FieldInjectionPlan(Field field) {
            this.field = field;
        }

        public void inject(final T instance, final R resource) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        field.set(instance,resource);
                        return null;
                    } catch (IllegalAccessException e) {
                        throw new ServerRtException("server.rt.err",e);
                    }
                }
            });
        }
    }

    /**
     * Injects to a method.
     */
    private static class MethodInjectionPlan<T,R> implements InjectionPlan<T,R> {
        private final Method method;

        public MethodInjectionPlan(Method method) {
            this.method = method;
        }

        public void inject(T instance, R resource) {
            invokeMethod(method, instance, resource);
        }
    }

    /**
     * Combines multiple {@link InjectionPlan}s into one.
     */
    private static class Compositor<T,R> implements InjectionPlan<T,R> {
        private final InjectionPlan<T,R>[] children;

        public Compositor(Collection<InjectionPlan<T,R>> children) {
            this.children = children.toArray(new InjectionPlan[children.size()]);
        }

        public void inject(T instance, R res) {
            for (InjectionPlan<T,R> plan : children)
                plan.inject(instance,res);
        }
    }

    /**
     * Helper for invoking a method with elevated privilege.
     */
    protected static void invokeMethod(final @Nullable Method method, final Object instance, final Object... args) {
        if(method==null)    return;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(instance,args);
                } catch (IllegalAccessException e) {
                    throw new ServerRtException("server.rt.err",e);
                } catch (InvocationTargetException e) {
                    throw new ServerRtException("server.rt.err",e);
                }
                return null;
            }
        });
    }

    /**
     * Finds the method that has the given annotation, while making sure that
     * there's only at most one such method.
     */
    protected final @Nullable Method findAnnotatedMethod(Class clazz, Class<? extends Annotation> annType) {
        boolean once = false;
        Method r = null;
        for(Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(annType) != null) {
                if (once)
                    throw new ServerRtException(ServerMessages.ANNOTATION_ONLY_ONCE(annType));
                if (method.getParameterTypes().length != 0)
                    throw new ServerRtException(ServerMessages.NOT_ZERO_PARAMETERS(method));
                r = method;
                once = true;
            }
        }
        return r;
    }

    /**
     * Creates an {@link InjectionPlan} that injects the given resource type to the given class.
     *
     * @param isStatic
     *      Only look for static field/method
     *
     */
    protected final <R> InjectionPlan<T,R> buildInjectionPlan(Class clazz, Class<R> resourceType, boolean isStatic) {
        List<InjectionPlan<T,R>> plan = new ArrayList<InjectionPlan<T,R>>();

        for(Field field: clazz.getDeclaredFields()) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                if(isInjectionPoint(resource, field.getType(),
                    ServerMessages.localizableWRONG_FIELD_TYPE(field.getName()),resourceType)) {

                    if(isStatic && !Modifier.isStatic(field.getModifiers()))
                        throw new WebServiceException(ServerMessages.STATIC_RESOURCE_INJECTION_ONLY(resourceType,field));

                    plan.add(new FieldInjectionPlan<T,R>(field));
                }
            }
        }

        for(Method method : clazz.getDeclaredMethods()) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource != null) {
                Class[] paramTypes = method.getParameterTypes();
                if (paramTypes.length != 1)
                    throw new ServerRtException(ServerMessages.WRONG_NO_PARAMETERS(method));
                if(isInjectionPoint(resource,paramTypes[0],
                    ServerMessages.localizableWRONG_PARAMETER_TYPE(method.getName()),resourceType)) {

                    if(isStatic && !Modifier.isStatic(method.getModifiers()))
                        throw new WebServiceException(ServerMessages.STATIC_RESOURCE_INJECTION_ONLY(resourceType,method));

                    plan.add(new MethodInjectionPlan<T,R>(method));
                }
            }
        }

        return new Compositor<T,R>(plan);
    }

    /**
     * Returns true if the combination of {@link Resource} and the field/method type
     * are consistent for {@link WebServiceContext} injection.
     */
    private boolean isInjectionPoint(Resource resource, Class fieldType, Localizable errorMessage, Class resourceType ) {
        Class t = resource.type();
        if (t.equals(Object.class)) {
            return fieldType.equals(resourceType);
        } else if (t.equals(resourceType)) {
            if (fieldType.isAssignableFrom(resourceType)) {
                return true;
            } else {
                // type compatibility error
                throw new ServerRtException(errorMessage);
            }
        }
        return false;
    }
}
