package org.apache.solr.search.facet;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.DocSet;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QueryContext;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.search.SyntaxError;


public abstract class FacetRequest {
  protected Map<String,AggValueSource> facetStats;  // per-bucket statistics
  protected Map<String,FacetRequest> subFacets;     // list of facets
  protected List<String> filters;
  protected boolean processEmpty;
  protected Domain domain;

  // domain changes
  public static class Domain {
    public List<String> excludeTags;
    public boolean toParent;
    public boolean toChildren;
    public String parents;
  }

  public FacetRequest() {
    facetStats = new LinkedHashMap<>();
    subFacets = new LinkedHashMap<>();
  }

  public Map<String, AggValueSource> getFacetStats() {
    return facetStats;
  }

  public Map<String, FacetRequest> getSubFacets() {
    return subFacets;
  }

  public void addStat(String key, AggValueSource stat) {
    facetStats.put(key, stat);
  }

  public void addSubFacet(String key, FacetRequest facetRequest) {
    subFacets.put(key, facetRequest);
  }

  @Override
  public String toString() {
    Map<String, Object> descr = getFacetDescription();
    String s = "facet request: { ";
    for (String key : descr.keySet()) {
      s += key + ":" + descr.get(key) + ",";
    }
    s += "}";
    return s;
  }
  
  public abstract FacetProcessor createFacetProcessor(FacetContext fcontext);

  public abstract FacetMerger createFacetMerger(Object prototype);
  
  public abstract Map<String, Object> getFacetDescription();
}


//public static class FacetContext {
//  // Context info for actually executing a local facet command
//  public static final int IS_SHARD=0x01;
//
//  public QueryContext qcontext;
//  public SolrQueryRequest req;  // TODO: replace with params?
//  public SolrIndexSearcher searcher;
//  public Query filter;  // TODO: keep track of as a DocSet or as a Query?
//  public DocSet base;
//  public FacetContext parent;
//  public int flags;
//  public FacetDebugInfo debugInfo;
//
//  public void setDebugInfo(FacetDebugInfo debugInfo) {
//    this.debugInfo = debugInfo;
//  }
//
//  public FacetDebugInfo getDebugInfo() {
//    return debugInfo;
//  }
//
//  public boolean isShard() {
//    return (flags & IS_SHARD) != 0;
//  }
//
//  /**
//   * @param filter The filter for the bucket that resulted in this context/domain.  Can be null if this is the root context.
//   * @param domain The resulting set of documents for this facet.
//   */
//  public FacetContext sub(Query filter, DocSet domain) {
//    FacetContext ctx = new FacetContext();
//    ctx.parent = this;
//    ctx.base = domain;
//    ctx.filter = filter;
//
//    // carry over from parent
//    ctx.flags = flags;
//    ctx.qcontext = qcontext;
//    ctx.req = req;
//    ctx.searcher = searcher;
//
//    return ctx;
//  }
//}

/*** not a separate type of parser for now...
class FacetBlockParentParser extends FacetParser<FacetBlockParent> {
  public FacetBlockParentParser(FacetParser parent, String key) {
    super(parent, key);
    facet = new FacetBlockParent();
  }

  @Override
  public FacetBlockParent parse(Object arg) throws SyntaxError {
    parseCommonParams(arg);

    if (arg instanceof String) {
      // just the field name...
      facet.parents = (String)arg;

    } else if (arg instanceof Map) {
      Map<String, Object> m = (Map<String, Object>) arg;
      facet.parents = getString(m, "parents", null);

      parseSubs( m.get("facet") );
    }

    return facet;
  }
}
***/

