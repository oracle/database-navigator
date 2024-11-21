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

package com.dbn.debugger.jdwp.process.tunnel;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class NSTunnelConnectionProxyHandler implements InvocationHandler {

	private final Object realNSTunnelConnection;
	private final Map<String, Method> methodsByName = new HashMap<>();
	
	public NSTunnelConnectionProxyHandler(Object realNSTunnelConnection) {
		this.realNSTunnelConnection = realNSTunnelConnection;
		for(Method method : this.realNSTunnelConnection.getClass().getDeclaredMethods()) {
            this.methodsByName.put(method.getName(), method);
        }
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO this assumes none of the methods are overloaded (i.e. same name but different parameters)
		Method realMethod = this.methodsByName.get(method.getName());
		realMethod.setAccessible(true);
		try {
		    return realMethod.invoke(this.realNSTunnelConnection, args);
		} catch (Throwable t) {
		    // for some reason, method.invoke doesn't recognize that NetException potentally
		    // thrown by the NSTunnelConnection as being an instance of IOException which
		    // is declared as a checked exception on some of its methods.  So we're going
		    // to unwrap the NetException and rethrow if it's a sub-class of IOException
		    // or IOException.  Else throw the InvocationTargetException thrown by invoke
		    if (IOException.class.isAssignableFrom(t.getCause().getClass()) ) {
	            throw t.getCause();
		    }
		    else {
		        throw t;
		    }
		}
	}

}
