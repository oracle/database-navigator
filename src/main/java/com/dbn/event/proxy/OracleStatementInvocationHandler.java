/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Statement;

public class OracleStatementInvocationHandler implements InvocationHandler {
  private final Statement delegate;
  private final Object dcr;

  private OracleStatementInvocationHandler(Statement delegate, Object dcr) {
    this.delegate = delegate;
    this.dcr = dcr;
  }

  /**
   * Creates a proxy for the driver’s OracleStatement interface.
   */
  public static Statement createProxy(Statement stmt, Object dcr, ClassLoader driverClassLoader) throws ClassNotFoundException {
    Class<?> oraStmtIfc = driverClassLoader.loadClass("oracle.jdbc.OracleStatement");
    return (Statement) java.lang.reflect.Proxy.newProxyInstance(
            driverClassLoader,
            new Class<?>[]{oraStmtIfc},
            new OracleStatementInvocationHandler(stmt, dcr)
    );
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String name = method.getName();
    if (name.startsWith("execute")) {
      // --- Begin change ---
      // Fetch the public OracleStatement interface (not the impl class)
      ClassLoader driverCL = delegate.getClass().getClassLoader();
      Class<?> oraStmtIfc = driverCL.loadClass("oracle.jdbc.OracleStatement");
      Class<?> dcrIfc = driverCL.loadClass("oracle.jdbc.dcn.DatabaseChangeRegistration");

      // Look up the interface method
      Method setReg = oraStmtIfc.getMethod("setDatabaseChangeRegistration", dcrIfc);
      setReg.invoke(delegate, dcr);
      // --- End change ---
    }
    // Forward all other calls
    return method.invoke(delegate, args);
  }
}