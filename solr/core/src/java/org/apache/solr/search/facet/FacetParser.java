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

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrException;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.FunctionQParserPlugin;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public abstract class FacetParser<FacetRequestT extends FacetRequest> {
  protected FacetRequestT facet;
  protected FacetParser parent;
  protected String key;

  public FacetParser(FacetParser parent,String key) {
    this.parent = parent;
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public String getPathStr() {
    if (parent == null) {
      return "/" + key;
    }
    return parent.getKey() + "/" + key;
  }

  protected RuntimeException err(String msg) {
    return new SolrException(SolrException.ErrorCode.BAD_REQUEST, msg + " ,path="+getPathStr());
  }

  public abstract FacetRequest parse(Object o) throws SyntaxError;

  // TODO: put the FacetRequest on the parser object?
  public void parseSubs(Object o) throws SyntaxError {
    if (o==null) return;
    if (o instanceof Map) {
      Map<String,Object> m = (Map<String, Object>) o;
      for (Map.Entry<String,Object> entry : m.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        if ("processEmpty".equals(key)) {
          facet.processEmpty = getBoolean(m, "processEmpty", false);
          continue;
        }

        // "my_prices" : { "range" : { "field":...
        // key="my_prices", value={"range":..

        Object parsedValue = parseFacetOrStat(key, value);

        // TODO: have parseFacetOrStat directly add instead of return?
        if (parsedValue instanceof FacetRequest) {
          facet.addSubFacet(key, (FacetRequest)parsedValue);
        } else if (parsedValue instanceof AggValueSource) {
          facet.addStat(key, (AggValueSource)parsedValue);
        } else {
          throw new RuntimeException("Huh? TODO: " + parsedValue);
        }
      }
    } else {
      // facet : my_field?
      throw err("Expected map for facet/stat");
    }
  }

  public Object parseFacetOrStat(String key, Object o) throws SyntaxError {

    if (o instanceof String) {
      return parseStringFacetOrStat(key, (String)o);
    }

    if (!(o instanceof Map)) {
      throw err("expected Map but got " + o);
    }

    // The type can be in a one element map, or inside the args as the "type" field
    // { "query" : "foo:bar" }
    // { "range" : { "field":... } }
    // { "type"  : range, field : myfield, ... }
    Map<String,Object> m = (Map<String,Object>)o;
    String type;
    Object args;

    if (m.size() == 1) {
      Map.Entry<String,Object> entry = m.entrySet().iterator().next();
      type = entry.getKey();
      args = entry.getValue();
      // throw err("expected facet/stat type name, like {range:{... but got " + m);
    } else {
      // type should be inside the map as a parameter
      Object typeObj = m.get("type");
      if (!(typeObj instanceof String)) {
        throw err("expected facet/stat type name, like {type:range, field:price, ...} but got " + typeObj);
      }
      type = (String)typeObj;
      args = m;
    }

    return parseFacetOrStat(key, type, args);
  }

  public Object parseFacetOrStat(String key, String type, Object args) throws SyntaxError {
    // TODO: a place to register all these facet types?

    if ("field".equals(type) || "terms".equals(type)) {
      return parseFieldFacet(key, args);
    } else if ("query".equals(type)) {
      return parseQueryFacet(key, args);
    } else if ("range".equals(type)) {
      return parseRangeFacet(key, args);
    }

    return parseStat(key, type, args);
  }



  FacetField parseFieldFacet(String key, Object args) throws SyntaxError {
    FacetFieldParser parser = new FacetFieldParser(this, key);
    return parser.parse(args);
  }

  FacetQuery parseQueryFacet(String key, Object args) throws SyntaxError {
    FacetQueryParser parser = new FacetQueryParser(this, key);
    return parser.parse(args);
  }

  FacetRange parseRangeFacet(String key, Object args) throws SyntaxError {
    FacetRangeParser parser = new FacetRangeParser(this, key);
    return parser.parse(args);
  }

  public Object parseStringFacetOrStat(String key, String s) throws SyntaxError {
    // "avg(myfield)"
    return parseStringStat(key, s);
    // TODO - simple string representation of facets
  }

  // parses avg(x)
  private AggValueSource parseStringStat(String key, String stat) throws SyntaxError {
    FunctionQParser parser = (FunctionQParser) QParser.getParser(stat, FunctionQParserPlugin.NAME, getSolrRequest());
    AggValueSource agg = parser.parseAgg(FunctionQParser.FLAG_DEFAULT);
    return agg;
  }

  public AggValueSource parseStat(String key, String type, Object args) throws SyntaxError {
    return null;
  }


  private FacetRequest.Domain getDomain() {
    if (facet.domain == null) {
      facet.domain = new FacetRequest.Domain();
    }
    return facet.domain;
  }

  protected void parseCommonParams(Object o) {
    if (o instanceof Map) {
      Map<String,Object> m = (Map<String,Object>)o;
      List<String> excludeTags = getStringList(m, "excludeTags");
      if (excludeTags != null) {
        getDomain().excludeTags = excludeTags;
      }

      Map<String,Object> domainMap = (Map<String,Object>) m.get("domain");
      if (domainMap != null) {
        excludeTags = getStringList(domainMap, "excludeTags");
        if (excludeTags != null) {
          getDomain().excludeTags = excludeTags;
        }

        String blockParent = (String)domainMap.get("blockParent");
        String blockChildren = (String)domainMap.get("blockChildren");

        if (blockParent != null) {
          getDomain().toParent = true;
          getDomain().parents = blockParent;
        } else if (blockChildren != null) {
          getDomain().toChildren = true;
          getDomain().parents = blockChildren;
        }

      }

    }
  }


  public String getField(Map<String,Object> args) {
    Object fieldName = args.get("field"); // TODO: pull out into defined constant
    if (fieldName == null) {
      fieldName = args.get("f");  // short form
    }
    if (fieldName == null) {
      throw err("Missing 'field'");
    }

    if (!(fieldName instanceof String)) {
      throw err("Expected string for 'field', got" + fieldName);
    }

    return (String)fieldName;
  }


  public Long getLongOrNull(Map<String,Object> args, String paramName, boolean required) {
    Object o = args.get(paramName);
    if (o == null) {
      if (required) {
        throw err("Missing required parameter '" + paramName + "'");
      }
      return null;
    }
    if (!(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte)) {
      throw err("Expected integer type for param '"+paramName + "' but got " + o);
    }

    return ((Number)o).longValue();
  }

  public long getLong(Map<String,Object> args, String paramName, long defVal) {
    Object o = args.get(paramName);
    if (o == null) {
      return defVal;
    }
    if (!(o instanceof Long || o instanceof Integer || o instanceof Short || o instanceof Byte)) {
      throw err("Expected integer type for param '"+paramName + "' but got " + o.getClass().getSimpleName() + " = " + o);
    }

    return ((Number)o).longValue();
  }

  public boolean getBoolean(Map<String,Object> args, String paramName, boolean defVal) {
    Object o = args.get(paramName);
    if (o == null) {
      return defVal;
    }
    // TODO: should we be more flexible and accept things like "true" (strings)?
    // Perhaps wait until the use case comes up.
    if (!(o instanceof Boolean)) {
      throw err("Expected boolean type for param '"+paramName + "' but got " + o.getClass().getSimpleName() + " = " + o);
    }

    return (Boolean)o;
  }

  public String getString(Map<String,Object> args, String paramName, String defVal) {
    Object o = args.get(paramName);
    if (o == null) {
      return defVal;
    }
    if (!(o instanceof String)) {
      throw err("Expected string type for param '"+paramName + "' but got " + o.getClass().getSimpleName() + " = " + o);
    }

    return (String)o;
  }

  public List<String> getStringList(Map<String,Object> args, String paramName) {
    Object o = args.get(paramName);
    if (o == null) {
      return null;
    }
    if (o instanceof List) {
      return (List<String>)o;
    }
    if (o instanceof String) {
      return StrUtils.splitSmart((String) o, ",", true);
    }

    throw err("Expected list of string or comma separated string values.");
  }

  public IndexSchema getSchema() {
    return parent.getSchema();
  }

  public SolrQueryRequest getSolrRequest() {
    return parent.getSolrRequest();
  }

}