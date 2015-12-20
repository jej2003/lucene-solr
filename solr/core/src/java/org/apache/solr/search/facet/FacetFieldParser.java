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

import java.util.Map;

import org.apache.solr.search.SyntaxError;

class FacetFieldParser extends FacetParser<FacetField> {
  public FacetFieldParser(FacetParser parent, String key) {
    super(parent, key);
    facet = new FacetField();
  }

  public FacetField parse(Object arg) throws SyntaxError {
    parseCommonParams(arg);
    if (arg instanceof String) {
      // just the field name...
      facet.field = (String)arg;
      parseSort( null );  // TODO: defaults

    } else if (arg instanceof Map) {
      Map<String, Object> m = (Map<String, Object>) arg;
      facet.field = getField(m);
      facet.offset = getLong(m, "offset", facet.offset);
      facet.limit = getLong(m, "limit", facet.limit);
      if (facet.limit == 0) facet.offset = 0;  // normalize.  an offset with a limit of non-zero isn't useful.
      facet.mincount = getLong(m, "mincount", facet.mincount);
      facet.missing = getBoolean(m, "missing", facet.missing);
      facet.numBuckets = getBoolean(m, "numBuckets", facet.numBuckets);
      facet.prefix = getString(m, "prefix", facet.prefix);
      facet.allBuckets = getBoolean(m, "allBuckets", facet.allBuckets);
      facet.method = FacetField.FacetMethod.fromString(getString(m, "method", null));
      facet.cacheDf = (int)getLong(m, "cacheDf", facet.cacheDf);

      facet.perSeg = (Boolean)m.get("perSeg");

      // facet.sort may depend on a facet stat...
      // should we be parsing / validating this here, or in the execution environment?
      Object o = m.get("facet");
      parseSubs(o);

      parseSort( m.get("sort") );
    }

    return facet;
  }


  // Sort specification is currently
  // sort : 'mystat desc'
  // OR
  // sort : { mystat : 'desc' }
  private void parseSort(Object sort) {
    if (sort == null) {
      facet.sortVariable = "count";
      facet.sortDirection = FacetField.SortDirection.desc;
    } else if (sort instanceof String) {
      String sortStr = (String)sort;
      if (sortStr.endsWith(" asc")) {
        facet.sortVariable = sortStr.substring(0, sortStr.length()-" asc".length());
        facet.sortDirection = FacetField.SortDirection.asc;
      } else if (sortStr.endsWith(" desc")) {
        facet.sortVariable = sortStr.substring(0, sortStr.length()-" desc".length());
        facet.sortDirection = FacetField.SortDirection.desc;
      } else {
        facet.sortVariable = sortStr;
        facet.sortDirection = "index".equals(facet.sortVariable) ? FacetField.SortDirection.asc : FacetField.SortDirection.desc;  // default direction for "index" is ascending
      }
    } else {
      // sort : { myvar : 'desc' }
      Map<String,Object> map = (Map<String,Object>)sort;
      // TODO: validate
      Map.Entry<String,Object> entry = map.entrySet().iterator().next();
      String k = entry.getKey();
      Object v = entry.getValue();
      facet.sortVariable = k;
      facet.sortDirection = FacetField.SortDirection.valueOf(v.toString());
    }

  }
}