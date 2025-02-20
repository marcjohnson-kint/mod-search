package org.folio.search.service.browse;

import static java.util.Locale.ROOT;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.folio.search.model.types.ResponseGroupType.BROWSE;
import static org.opensearch.index.query.QueryBuilders.boolQuery;
import static org.opensearch.index.query.QueryBuilders.termQuery;
import static org.opensearch.index.query.QueryBuilders.termsQuery;
import static org.opensearch.search.builder.SearchSourceBuilder.searchSource;
import static org.opensearch.search.sort.SortBuilders.fieldSort;
import static org.opensearch.search.sort.SortOrder.ASC;
import static org.opensearch.search.sort.SortOrder.DESC;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.search.domain.dto.Authority;
import org.folio.search.domain.dto.AuthorityBrowseItem;
import org.folio.search.model.BrowseResult;
import org.folio.search.model.SearchResult;
import org.folio.search.model.service.BrowseContext;
import org.folio.search.model.service.BrowseRequest;
import org.folio.search.service.metadata.SearchFieldProvider;
import org.opensearch.index.query.TermsQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorityBrowseService extends AbstractBrowseServiceBySearchAfter<AuthorityBrowseItem, Authority> {

  private static final TermsQueryBuilder FILTER_QUERY = termsQuery("authRefType", List.of("Authorized", "Reference"));

  private final SearchFieldProvider searchFieldProvider;

  @Override
  protected BrowseResult<AuthorityBrowseItem> mapToBrowseResult(SearchResult<Authority> result, boolean isAnchor) {
    return BrowseResult.of(result).map(authority -> new AuthorityBrowseItem()
      .authority(authority)
      .headingRef(authority.getHeadingRef())
      .isAnchor(isAnchor ? true : null));
  }

  @Override
  protected AuthorityBrowseItem getEmptyBrowseItem(BrowseContext ctx) {
    return new AuthorityBrowseItem().isAnchor(true).headingRef(ctx.getAnchor());
  }

  @Override
  protected SearchSourceBuilder getSearchQuery(BrowseRequest request, BrowseContext ctx, boolean isBrowsingForward) {
    var boolQuery = boolQuery().filter(FILTER_QUERY);
    ctx.getFilters().forEach(boolQuery::filter);
    return searchSource().query(boolQuery)
      .searchAfter(new Object[] {ctx.getAnchor().toLowerCase(ROOT)})
      .sort(fieldSort(request.getTargetField()).order(isBrowsingForward ? ASC : DESC))
      .size(ctx.getLimit(isBrowsingForward) + 1)
      .fetchSource(getIncludedSourceFields(request), null)
      .from(0);
  }

  @Override
  protected SearchSourceBuilder getAnchorSearchQuery(BrowseRequest request, BrowseContext context) {
    var boolQuery = boolQuery().filter(FILTER_QUERY).must(termQuery(request.getTargetField(), context.getAnchor()));
    context.getFilters().forEach(boolQuery::filter);
    return searchSource().query(boolQuery).from(0).size(1).fetchSource(getIncludedSourceFields(request), null);
  }

  @Override
  protected String getValueForBrowsing(AuthorityBrowseItem browseItem) {
    return browseItem.getHeadingRef();
  }

  private String[] getIncludedSourceFields(BrowseRequest request) {
    return isFalse(request.getExpandAll()) ? searchFieldProvider.getSourceFields(request.getResource(), BROWSE) : null;
  }
}
