/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.outbox;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.SERVICE_ACCOUNT_ENTITY;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;
import static io.harness.ng.core.utils.NGYamlUtils.getYamlString;
import static io.harness.rule.OwnerRule.SOWMYA;

import static io.serializer.HObjectMapper.NG_DEFAULT_OBJECT_MAPPER;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.Action;
import io.harness.audit.ResourceTypeConstants;
import io.harness.audit.beans.AuditEntry;
import io.harness.audit.client.api.AuditClientService;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.ng.core.dto.ServiceAccountRequest;
import io.harness.ng.core.events.ServiceAccountCreateEvent;
import io.harness.ng.core.events.ServiceAccountDeleteEvent;
import io.harness.ng.core.events.ServiceAccountUpdateEvent;
import io.harness.outbox.OutboxEvent;
import io.harness.rule.Owner;
import io.harness.security.SourcePrincipalContextData;
import io.harness.security.dto.UserPrincipal;
import io.harness.serviceaccount.ServiceAccountDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

@OwnedBy(PL)
public class ServiceAccountEventHandlerTest extends CategoryTest {
  private ObjectMapper objectMapper;
  private Producer producer;
  private AuditClientService auditClientService;
  private ServiceAccountEventHandler serviceAccountEventHandler;

  @Before
  public void setup() {
    objectMapper = NG_DEFAULT_OBJECT_MAPPER;
    producer = mock(Producer.class);
    auditClientService = mock(AuditClientService.class);
    serviceAccountEventHandler = spy(new ServiceAccountEventHandler(producer, auditClientService));
  }

  private ServiceAccountDTO getServiceAccountDTO(
      String accountIdentifier, String projectIdentifier, String orgIdentifier, String identifier) {
    return ServiceAccountDTO.builder()
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .accountIdentifier(accountIdentifier)
        .identifier(identifier)
        .name(randomAlphabetic(10))
        .description(randomAlphabetic(50))
        .build();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testCreate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ServiceAccountDTO serviceAccountDTO =
        getServiceAccountDTO(accountIdentifier, projectIdentifier, orgIdentifier, identifier);
    ServiceAccountCreateEvent serviceAccountCreateEvent = new ServiceAccountCreateEvent(serviceAccountDTO);
    String eventData = objectMapper.writeValueAsString(serviceAccountCreateEvent);
    GlobalContext globalContext = new GlobalContext();
    SourcePrincipalContextData sourcePrincipalContextData =
        SourcePrincipalContextData.builder()
            .principal(new UserPrincipal(
                randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10), randomAlphabetic(10)))
            .build();
    globalContext.setGlobalContextRecord(sourcePrincipalContextData);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("ServiceAccountCreated")
                                  .globalContext(globalContext)
                                  .eventData(eventData)
                                  .resourceScope(serviceAccountCreateEvent.getResourceScope())
                                  .resource(serviceAccountCreateEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    String newYaml = getYamlString(ServiceAccountRequest.builder().serviceAccount(serviceAccountDTO).build());

    final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    verifyMethodInvocation(outboxEvent, messageArgumentCaptor, auditEntryArgumentCaptor);

    Message message = messageArgumentCaptor.getValue();
    assertMessage(message, accountIdentifier, CREATE_ACTION);

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.CREATE, auditEntry.getAction());
    assertNull(auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testUpdate() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ServiceAccountDTO serviceAccountDTO =
        getServiceAccountDTO(accountIdentifier, projectIdentifier, orgIdentifier, identifier);
    ServiceAccountDTO newServiceAccountDTO =
        getServiceAccountDTO(accountIdentifier, projectIdentifier, orgIdentifier, identifier);
    ServiceAccountUpdateEvent serviceAccountUpdateEvent =
        new ServiceAccountUpdateEvent(serviceAccountDTO, newServiceAccountDTO);
    String eventData = objectMapper.writeValueAsString(serviceAccountUpdateEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("ServiceAccountUpdated")
                                  .eventData(eventData)
                                  .resourceScope(serviceAccountUpdateEvent.getResourceScope())
                                  .resource(serviceAccountUpdateEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    String oldYaml = getYamlString(ServiceAccountRequest.builder().serviceAccount(serviceAccountDTO).build());
    String newYaml = getYamlString(ServiceAccountRequest.builder().serviceAccount(newServiceAccountDTO).build());

    final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    verifyMethodInvocation(outboxEvent, messageArgumentCaptor, auditEntryArgumentCaptor);

    Message message = messageArgumentCaptor.getValue();
    assertMessage(message, accountIdentifier, UPDATE_ACTION);

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.UPDATE, auditEntry.getAction());
    assertEquals(oldYaml, auditEntry.getOldYaml());
    assertEquals(newYaml, auditEntry.getNewYaml());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testDelete() throws JsonProcessingException {
    String accountIdentifier = randomAlphabetic(10);
    String orgIdentifier = randomAlphabetic(10);
    String projectIdentifier = randomAlphabetic(10);
    String identifier = randomAlphabetic(10);
    ServiceAccountDTO serviceAccountDTO =
        getServiceAccountDTO(accountIdentifier, projectIdentifier, orgIdentifier, identifier);
    ServiceAccountDeleteEvent serviceAccountDeleteEvent = new ServiceAccountDeleteEvent(serviceAccountDTO);
    String eventData = objectMapper.writeValueAsString(serviceAccountDeleteEvent);
    OutboxEvent outboxEvent = OutboxEvent.builder()
                                  .id(randomAlphabetic(10))
                                  .blocked(false)
                                  .eventType("ServiceAccountDeleted")
                                  .eventData(eventData)
                                  .resourceScope(serviceAccountDeleteEvent.getResourceScope())
                                  .resource(serviceAccountDeleteEvent.getResource())
                                  .createdAt(Long.parseLong(randomNumeric(5)))
                                  .build();

    String oldYaml = getYamlString(ServiceAccountRequest.builder().serviceAccount(serviceAccountDTO).build());

    final ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    final ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor = ArgumentCaptor.forClass(AuditEntry.class);
    verifyMethodInvocation(outboxEvent, messageArgumentCaptor, auditEntryArgumentCaptor);

    Message message = messageArgumentCaptor.getValue();
    assertMessage(message, accountIdentifier, DELETE_ACTION);

    AuditEntry auditEntry = auditEntryArgumentCaptor.getValue();
    assertAuditEntry(accountIdentifier, orgIdentifier, projectIdentifier, identifier, auditEntry, outboxEvent);
    assertEquals(Action.DELETE, auditEntry.getAction());
    assertNull(auditEntry.getNewYaml());
    assertEquals(oldYaml, auditEntry.getOldYaml());
  }

  private void verifyMethodInvocation(OutboxEvent outboxEvent, ArgumentCaptor<Message> messageArgumentCaptor,
      ArgumentCaptor<AuditEntry> auditEntryArgumentCaptor) {
    when(producer.send(any())).thenReturn("");
    when(auditClientService.publishAudit(any(), any())).thenReturn(true);

    serviceAccountEventHandler.handle(outboxEvent);

    verify(producer, times(1)).send(messageArgumentCaptor.capture());
    verify(auditClientService, times(1)).publishAudit(auditEntryArgumentCaptor.capture(), any());
  }

  private void assertMessage(Message message, String accountIdentifier, String action) {
    assertNotNull(message.getMetadataMap());
    Map<String, String> metadataMap = message.getMetadataMap();
    assertEquals(accountIdentifier, metadataMap.get("accountId"));
    assertEquals(SERVICE_ACCOUNT_ENTITY, metadataMap.get(ENTITY_TYPE));
    assertEquals(action, metadataMap.get(ACTION));
  }

  private void assertAuditEntry(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier, AuditEntry auditEntry, OutboxEvent outboxEvent) {
    assertNotNull(auditEntry);
    assertEquals(outboxEvent.getId(), auditEntry.getInsertId());
    assertEquals(ResourceTypeConstants.SERVICE_ACCOUNT, auditEntry.getResource().getType());
    assertEquals(identifier, auditEntry.getResource().getIdentifier());
    assertEquals(accountIdentifier, auditEntry.getResourceScope().getAccountIdentifier());
    assertEquals(orgIdentifier, auditEntry.getResourceScope().getOrgIdentifier());
    assertEquals(projectIdentifier, auditEntry.getResourceScope().getProjectIdentifier());
    assertEquals(ModuleType.CORE, auditEntry.getModule());
    assertEquals(outboxEvent.getCreatedAt().longValue(), auditEntry.getTimestamp());
    assertNull(auditEntry.getEnvironment());
  }
}
