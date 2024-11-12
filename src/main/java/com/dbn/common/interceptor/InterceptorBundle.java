/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.common.interceptor;

import com.dbn.common.routine.Consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Unsafe.cast;

/**
 * Collection of {@link Interceptor} entities grouped by {@link InterceptorType} and sorted by {@link com.dbn.common.Priority}
 * Allows invoking interceptors of a given type in their order of priority
 *
 * @author Dan Cioca (Oracle)
 */
public class InterceptorBundle {
    public static final InterceptorBundle VOID = new InterceptorBundle();

    private final Map<InterceptorType<?>, List<Interceptor<?>>> entries = new ConcurrentHashMap<>();

    public void register(Interceptor<?> interceptor) {
        InterceptorType<?> type = interceptor.getType();
        List<Interceptor<?>> interceptors = this.entries.computeIfAbsent(type, t -> new ArrayList<>());

        interceptors.add(interceptor);
        Collections.sort(interceptors);
    }

    public final void before(InterceptorType<?> type, InterceptorContext context) {
        invoke(type, context, i -> i.before(context));
    }

    public final void after(InterceptorType<?> type, InterceptorContext context) {
        invoke(type, context, i -> i.after(context));
    }

    private <C extends InterceptorContext> void invoke(InterceptorType<?> type, C context, Consumer<Interceptor<C>> consumer) {
        List<Interceptor<C>> interceptors = cast(entries.get(type));
        if (interceptors == null) return;

        interceptors.stream().filter(i -> i.supports(context)).forEach(consumer);
    }

}
