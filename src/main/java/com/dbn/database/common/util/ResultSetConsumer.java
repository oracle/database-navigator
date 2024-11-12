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

package com.dbn.database.common.util;

import com.dbn.common.routine.ThrowableCallable;
import com.dbn.connection.Resources;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Stateless general-purpose consumer implementation for reading a {@link ResultSet} into an element of a given type
 * Manages the lifecycle of the ResultSet by closing it irrespective if the read was successful or not
 *
 * @param <T> the type of the resulting element
 * @author Dan Cioca (Oracle)
 */
public abstract class ResultSetConsumer<T> {

    /**
     * Consumer utility accepting a ResultSet supplier as parameter
     * @param resultSetSupplier function returning a result set
     * @return The resulting element
     * @throws SQLException if the invocation of the supplier or reading the ResultSet fails
     */
    public final T consume(ThrowableCallable<ResultSet, SQLException> resultSetSupplier) throws SQLException {
        return consume(resultSetSupplier.call());
    }

    /**
     * Consumer utility accepting a ResultSet as parameter
     * @param resultSet the {@link ResultSet} to be consumed
     * @return The resulting element
     * @throws SQLException if the invocation of the supplier or reading the ResultSet fails
     */    public final T consume(ResultSet resultSet) throws SQLException {
        try {
            return read(resultSet);
        } catch (SQLException e) {
            conditionallyLog(e);
            throw e;
        } finally {
            Resources.close(resultSet);
        }
    }

    /**
     * Conversion utility that must be implemented by the actual consumer
     * @param resultSet the {@link ResultSet} to be read
     * @return the return element
     * @throws SQLException if reading the ResultSet fails
     */
    protected abstract T read(ResultSet resultSet) throws SQLException;
}
