package org.folio.search.controller;

import static org.folio.search.model.service.CqlResourceIdsRequest.INSTANCE_ID_PATH;
import static org.folio.search.utils.SearchUtils.INSTANCE_RESOURCE;

import lombok.RequiredArgsConstructor;
import org.folio.search.domain.dto.CqlFacetRequest;
import org.folio.search.domain.dto.CqlSearchRequest;
import org.folio.search.domain.dto.FacetResult;
import org.folio.search.domain.dto.SearchResult;
import org.folio.search.mapper.SearchRequestMapper;
import org.folio.search.model.service.CqlResourceIdsRequest;
import org.folio.search.rest.resource.InstancesApi;
import org.folio.search.service.FacetService;
import org.folio.search.service.ResourceIdsStreamHelper;
import org.folio.search.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller with set of endpoints for manipulating with Elasticsearch search API.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/search")
public class SearchController implements InstancesApi {

  private final FacetService facetService;
  private final SearchService searchService;
  private final ResourceIdsStreamHelper resourceIdsStreamHelper;
  private final SearchRequestMapper searchRequestMapper;

  @Override
  public ResponseEntity<SearchResult> searchInstances(CqlSearchRequest request, String tenantId) {
    var searchRequest = searchRequestMapper.convert(request, INSTANCE_RESOURCE, tenantId);
    return ResponseEntity.ok(searchService.search(searchRequest));
  }

  @Override
  public ResponseEntity<FacetResult> getFacets(CqlFacetRequest request, String tenantId) {
    var facetRequest = searchRequestMapper.convert(request, INSTANCE_RESOURCE, tenantId);
    return ResponseEntity.ok(facetService.getFacets(facetRequest));
  }

  @Override
  public ResponseEntity<Void> getInstanceIds(String query, String tenantId) {
    var request = CqlResourceIdsRequest.of(query, INSTANCE_RESOURCE, tenantId, INSTANCE_ID_PATH);
    return resourceIdsStreamHelper.streamResourceIds(request);
  }
}
