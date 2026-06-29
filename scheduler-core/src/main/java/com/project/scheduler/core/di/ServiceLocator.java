package com.project.scheduler.core.di;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ServiceLocator {

	private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();
	private final Map<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();

	public <T> void registerSingleton(Class<T> serviceType, T instance) {
		singletons.put(serviceType, instance);
	}

	public <T> void registerFactory(Class<T> serviceType, Supplier<? extends T> factory) {
		factories.put(serviceType, factory);
	}

	public <T> T resolve(Class<T> serviceType) {
		Object singleton = singletons.get(serviceType);
		if (singleton != null) {
			return serviceType.cast(singleton);
		}
		Supplier<?> factory = factories.get(serviceType);
		if (factory == null) {
			throw new IllegalStateException("No service registered: " + serviceType.getName());
		}
		return serviceType.cast(factory.get());
	}
}
