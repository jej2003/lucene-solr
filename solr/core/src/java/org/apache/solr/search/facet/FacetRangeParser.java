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
import java.util.List;
import java.util.Map;

import org.apache.solr.common.params.FacetParams;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.search.SyntaxError;

class FacetRangeParser extends FacetParser<FacetRange> {
  public FacetRangeParser(FacetParser parent, String key) {
    super(parent, key);
    facet = new FacetRange();
  }

  public FacetRange parse(Object arg) throws SyntaxError {
    parseCommonParams(arg);

    if (!(arg instanceof Map)) {
      throw err("Missing range facet arguments");
    }

    Map<String, Object> m = (Map<String, Object>) arg;

    facet.field = getString(m, "field", null);

    facet.start = m.get("start");
    facet.end = m.get("end");
    facet.gap = m.get("gap");
    facet.hardend = getBoolean(m, "hardend", facet.hardend);
    facet.mincount = getLong(m, "mincount", 0);

    // TODO: refactor list-of-options code

    Object o = m.get("include");
    String[] includeList = null;
    if (o != null) {
      List lst = null;

      if (o instanceof List) {
        lst = (List)o;
      } else if (o instanceof String) {
        lst = StrUtils.splitSmart((String) o, ',');
      }

      includeList = (String[])lst.toArray(new String[lst.size()]);
    }
    facet.include = FacetParams.FacetRangeInclude.parseParam( includeList );

    facet.others = EnumSet.noneOf(FacetParams.FacetRangeOther.class);

    o = m.get("other");
    if (o != null) {
      List<String> lst = null;

      if (o instanceof List) {
        lst = (List)o;
      } else if (o instanceof String) {
        lst = StrUtils.splitSmart((String)o, ',');
      }

      for (String otherStr : lst) {
        facet.others.add( FacetParams.FacetRangeOther.get(otherStr) );
      }
    }


    Object facetObj = m.get("facet");
    parseSubs(facetObj);

    return facet;
  }

}
