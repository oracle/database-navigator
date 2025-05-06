//package com.dbn.events.model;
//
//import com.dbn.events.RegistrationManager;
//import com.dbn.events.service.EventHistoryService;
//import com.dbn.events.service.RegistrationService;
//
//import java.sql.SQLException;
//
//public class ConnectionRegistrationsContext {
//  private final String connectionId;          // e.g. JDBC URL or unique name
//  private final RegistrationManager dcnManager;
//  private final EventHistoryService history;
//  private final RegistrationService registrationService;
//
//  public ConnectionRegistrationsContext(String connectionId) throws SQLException {
//    this.connectionId        = connectionId;
//    this.dcnManager          = new DCNListenerManager(/* pass connectionId or Connection*/);
//    this.history             = new EventHistoryService();
//    this.registrationService = new RegistrationService(/* pass connectionId*/);
//  }
//}
