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
import java.nio.ByteBuffer;

/**
 * Interface to a dynamic bean proxy that gives us programmatic access
 * to NSTunnelConnection without statically linking to it.  This is needed
 * because not all versions of the JDBC driver in use by the design time
 * will contain this which will start in 23c and later versions of earlier drivers
 * by backport.
 * 
 * @author cbateman
 *
 */
public interface NSTunnelConnectionProxy {
    
	/**
	 * Close the tunnel.  Discard this instance after closing; don't reuse
	 */
    void close() throws IOException;
	boolean isOpen() throws IOException;
	/**
	 * @return get address of the tunnel endpoint.  This must be passed to the
	 * driver.
	 */
    String tunnelAddress();
	
	/**
	 * Read the next buffer worth of tunnel data into b. 
	 * @param b
	 * @return number of bytes read
	 */
    int read(ByteBuffer b) throws IOException;
	/**
	 * Writes the buffer using its internal position and length params.
	 * @param b
	 */
    void write(ByteBuffer b) throws IOException;
	
}
