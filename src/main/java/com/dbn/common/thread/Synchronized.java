/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.common.thread;

import com.dbn.common.Pair;
import com.dbn.common.routine.ParametricCallable;
import com.dbn.common.routine.ParametricRunnable;
import com.dbn.common.util.Unsafe;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;


@UtilityClass
public class Synchronized {
	static final Map<Object, SyncObject> LOCKS = new HashMap<>(100);

	public static <O, E extends Throwable> void on(@NonNls O owner, ParametricRunnable<O, E> runnable) throws E{
		SyncObject<O> lock = acquire(owner);
		try {
			lock.execute(owner, runnable);
		} finally {
			release(owner, lock);
		}
	}

	public static <O, R, E extends Throwable> R on(@NonNls O owner, ParametricCallable<O, R, E> callable) throws E{
		SyncObject<R> lock = acquire(owner);
		try {
			return lock.execute(owner, callable);
		} finally {
			release(owner, lock);
		}
	}

	public static <O, E extends Throwable> void on(@NonNls O owner, Object discriminator, ParametricRunnable<O, E> runnable) throws E {
		Pair<Object, Object> qualifiedOwner = Pair.of(owner, discriminator);
		SyncObject<O> lock = acquire(qualifiedOwner);
		try {
			lock.execute(owner, runnable);
		} finally {
			release(qualifiedOwner, lock);
		}
	}

	public static <O, R, E extends Throwable> R on(@NonNls O owner, Object discriminator, ParametricCallable<O, R, E> callable) throws E {
		Pair<Object, Object> qualifiedOwner = Pair.of(owner, discriminator);
		SyncObject<R> lock = acquire(qualifiedOwner);
		try {
			return lock.execute(owner, callable);
		} finally {
			release(qualifiedOwner, lock);
		}
	}

	@NotNull
    public static <O, T> T ensure(
			@NonNls O owner,
			Object discriminator,
			Supplier<T> current,
			Supplier<T> initializer,
			Consumer<T> setter) {
		T value = current.get();
		if (value != null) return value;

		return on(owner, discriminator, ignored -> {
			T currentValue = current.get();
			if (currentValue != null) return currentValue;

			T initializedValue = initializer.get();
			setter.accept(initializedValue);
			return initializedValue;
		});
	}

	private static synchronized <R> SyncObject<R> acquire(Object owner) {
		SyncObject<R> lock = Unsafe.cast(LOCKS.computeIfAbsent(owner, o -> new SyncObject()));
		lock.invokers++;
		return lock;
	}

	private synchronized static void release(Object owner, SyncObject<?> lock) {
		if (--lock.invokers == 0) LOCKS.remove(owner, lock);
	}

	private static class SyncObject<R> {
		private final Lock lock = new ReentrantLock();
		private int invokers;

		public <O, E extends Throwable> R execute(O owner, ParametricCallable<O, R, E> callable) throws E{
			lock.lock();
			try {
				return callable.call(owner);
			} finally {
				lock.unlock();
			}
		}

		public <O, E extends Throwable> void execute(O owner, ParametricRunnable<O, E> runnable) throws E{
			lock.lock();
			try {
				runnable.run(owner);
			} finally {
				lock.unlock();
			}
		}
	}
}
