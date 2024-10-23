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
