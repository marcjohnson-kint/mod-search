package org.folio.search.integration;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.search.domain.dto.ResourceEventType.CREATE;
import static org.folio.search.domain.dto.ResourceEventType.UPDATE;
import static org.folio.search.model.client.CqlQuery.exactMatchAny;
import static org.folio.search.model.service.ResultList.asSinglePage;
import static org.folio.search.utils.JsonConverter.MAP_TYPE_REFERENCE;
import static org.folio.search.utils.JsonUtils.LIST_OF_MAPS_TYPE_REFERENCE;
import static org.folio.search.utils.SearchUtils.INSTANCE_RESOURCE;
import static org.folio.search.utils.TestConstants.RESOURCE_NAME;
import static org.folio.search.utils.TestUtils.OBJECT_MAPPER;
import static org.folio.search.utils.TestUtils.mapOf;
import static org.folio.search.utils.TestUtils.randomId;
import static org.folio.search.utils.TestUtils.resourceEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;
import org.folio.search.client.InventoryViewClient;
import org.folio.search.client.InventoryViewClient.InstanceView;
import org.folio.search.domain.dto.Holding;
import org.folio.search.domain.dto.Instance;
import org.folio.search.domain.dto.Item;
import org.folio.search.domain.dto.ResourceEvent;
import org.folio.search.service.TenantScopedExecutionService;
import org.folio.search.utils.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ResourceFetchServiceTest {

  @InjectMocks
  private ResourceFetchService resourceFetchService;
  @Mock
  private TenantScopedExecutionService executionService;
  @Mock
  private InventoryViewClient inventoryClient;

  @Test
  void fetchInstancesById_positive() {
    var events = resourceEvents();
    var instanceId1 = events.get(0).getId();
    var instanceId2 = events.get(1).getId();

    var instance1 = instanceView(new Instance().id(instanceId1).title("inst1"), null);
    var instance2 = instanceView(new Instance().id(instanceId2).title("inst2")
      .holdings(List.of(new Holding().id("holdingId"))).items(List.of(new Item().id("itemId"))), true);

    when(executionService.executeTenantScoped(any(), any())).thenAnswer(inv -> inv.<Callable<?>>getArgument(1).call());
    when(inventoryClient.getInstances(exactMatchAny("id", List.of(instanceId1, instanceId2)), 2))
      .thenReturn(asSinglePage(List.of(instance1, instance2)));

    var actual = resourceFetchService.fetchInstancesByIds(events);

    assertThat(actual).isEqualTo(List.of(
      resourceEvent(instanceId1, INSTANCE_RESOURCE, mapOf("id", instanceId1, "title", "inst1", "isBoundWith", null)),
      resourceEvent(instanceId2, INSTANCE_RESOURCE, UPDATE,
        mapOf("id", instanceId2, "title", "inst2",
          "holdings", List.of(mapOf("id", "holdingId")), "items", List.of(mapOf("id", "itemId")), "isBoundWith", true),
        mapOf("id", instanceId2, "title", "old"))
    ));
  }

  @Test
  void fetchInstancesById_negative_oneOfResourcesByIdIsNotFound() {
    var events = resourceEvents();
    var instanceId1 = events.get(0).getId();
    var instanceId2 = events.get(1).getId();
    var instanceView = instanceView(new Instance().id(instanceId1).title("inst1"), null);

    when(executionService.executeTenantScoped(any(), any())).thenAnswer(inv -> inv.<Callable<?>>getArgument(1).call());
    when(inventoryClient.getInstances(exactMatchAny("id", List.of(instanceId1, instanceId2)), 2))
      .thenReturn(asSinglePage(List.of(instanceView)));

    var actual = resourceFetchService.fetchInstancesByIds(events);
    assertThat(actual).isEqualTo(List.of(
      resourceEvent(instanceId1, INSTANCE_RESOURCE, mapOf("id", instanceId1, "title", "inst1", "isBoundWith", null))
    ));
  }

  @Test
  void fetchInstancesById_negative_resourceReturnedWithInvalidId() {
    var id = randomId();
    var resourceEvent = resourceEvent(id, INSTANCE_RESOURCE, UPDATE,
      mapOf("id", id, "title", "new"), mapOf("id", id, "title", "old"));
    var invalidId = randomId();
    var instanceView = instanceView(new Instance().id(invalidId).title("inst1"), null);

    when(executionService.executeTenantScoped(any(), any())).thenAnswer(inv -> inv.<Callable<?>>getArgument(1).call());
    when(inventoryClient.getInstances(exactMatchAny("id", List.of(id)), 1))
      .thenReturn(asSinglePage(List.of(instanceView)));

    var actual = resourceFetchService.fetchInstancesByIds(List.of(resourceEvent));
    assertThat(actual).isEqualTo(List.of(
      resourceEvent(invalidId, INSTANCE_RESOURCE, mapOf("id", invalidId, "title", "inst1", "isBoundWith", null))
    ));
  }

  @Test
  void fetchInstancesByIds_positive_emptyListOfIds() {
    var actual = resourceFetchService.fetchInstancesByIds(emptyList());
    assertThat(actual).isEmpty();
  }

  private static List<ResourceEvent> resourceEvents() {
    var updateEventId = randomId();
    return List.of(
      resourceEvent(randomId(), INSTANCE_RESOURCE, CREATE),
      resourceEvent(updateEventId, INSTANCE_RESOURCE, UPDATE,
        mapOf("id", updateEventId, "title", "new"),
        mapOf("id", updateEventId, "title", "old")),
      resourceEvent(randomId(), RESOURCE_NAME, CREATE));
  }

  private static InstanceView instanceView(Instance instance, Boolean isBoundWith) {
    var instanceMap = OBJECT_MAPPER.convertValue(instance, MAP_TYPE_REFERENCE);
    var itemsMap = OBJECT_MAPPER.convertValue(instance.getItems(), LIST_OF_MAPS_TYPE_REFERENCE);
    var holdingsMap = OBJECT_MAPPER.convertValue(instance.getHoldings(), LIST_OF_MAPS_TYPE_REFERENCE);
    return new InstanceView(instanceMap, holdingsMap, itemsMap, isBoundWith);
  }
}
