package org.apache.solr.search.facet;

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.QueryContext;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.facet.FacetDebugInfo;

public class FacetContext {
  // Context info for actually executing a local facet command
  public static final int IS_SHARD=0x01;

  public QueryContext qcontext;
  public SolrQueryRequest req;  // TODO: replace with params?
  public SolrIndexSearcher searcher;
  public org.apache.lucene.search.Query filter;  // TODO: keep track of as a DocSet or as a Query?
  public DocSet base;
  public FacetContext parent;
  public int flags;
  public FacetDebugInfo debugInfo;

  public void setDebugInfo(FacetDebugInfo debugInfo) {
    this.debugInfo = debugInfo;
  }

  public FacetDebugInfo getDebugInfo() {
    return debugInfo;
  }

  public boolean isShard() {
    return (flags & IS_SHARD) != 0;
  }

  /**
   * @param filter The filter for the bucket that resulted in this context/domain.  Can be null if this is the root context.
   * @param domain The resulting set of documents for this facet.
   */
  public FacetContext sub(org.apache.lucene.search.Query filter, DocSet domain) {
    FacetContext ctx = new FacetContext();
    ctx.parent = this;
    ctx.base = domain;
    ctx.filter = filter;

    // carry over from parent
    ctx.flags = flags;
    ctx.qcontext = qcontext;
    ctx.req = req;
    ctx.searcher = searcher;

    return ctx;
  }
}