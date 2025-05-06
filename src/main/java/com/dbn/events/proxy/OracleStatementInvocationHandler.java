package com.dbn.events.proxy;

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