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

import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SyntaxError;

public class FacetTopParser extends FacetParser<FacetQuery> {
  private SolrQueryRequest req;

  public FacetTopParser(SolrQueryRequest req) {
    super(null, "facet");
    this.facet = new FacetQuery();
    this.req = req;
  }

  @Override
  public FacetQuery parse(Object args) throws SyntaxError {
    parseSubs(args);
    return facet;
  }

  @Override
  public SolrQueryRequest getSolrRequest() {
    return req;
  }

  @Override
  public IndexSchema getSchema() {
    return req.getSchema();
  }
}