/* Data Manager (DM).
   Copyright (C) 2015 DISIT Lab http://www.disit.org - University of Florence
   This program is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License
   as published by the Free Software Foundation; either version 2
   of the License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA. */
package edu.unifi.disit.datamanager.datamodel.elasticdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram.Bucket;
import org.elasticsearch.search.aggregations.bucket.histogram.ParsedDateHistogram;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unifi.disit.datamanager.config.ConfigIndexBean;

@Repository
@Transactional(readOnly = true)
public class KPIElasticValueDAOImpl implements KPIElasticValueDAOCustom {

	private static final Logger logger = LogManager.getLogger();

	@Autowired
	RestHighLevelClient elasticSearchClient;

	@Override
	public List<String> getElasticValuesDates(String sensorID, boolean checkCoordinates) {
		SearchResponse response = null;
		List<String> listDates = new ArrayList<>();

		BoolQueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery("sensorID", sensorID));
		if (checkCoordinates) {
			qb.must(QueryBuilders.existsQuery("latitude")).must(QueryBuilders.existsQuery("longitude"));
		}
		try {
			response = elasticSearchClient.search(new SearchRequest(ConfigIndexBean.getIndexName()).source(new SearchSourceBuilder()
					.query(qb).aggregation(AggregationBuilders.dateHistogram("distinctDates").field("date_time")
							.dateHistogramInterval(DateHistogramInterval.DAY).format("yyyy-MM-dd").minDocCount(1)

					).size(0)), RequestOptions.DEFAULT);

			for (Bucket bucket : ((ParsedDateHistogram) response.getAggregations().get("distinctDates")).getBuckets()) {
				listDates.add(bucket.getKeyAsString()); // bucket key
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return listDates;
	}

	@Override
	public List<KPIElasticValue> findBySensorIdNoPagesWithLimit(String sensorID, Date from, Date to, Integer first,
			Integer last) {
		SearchResponse searchResponse = null;
		List<KPIElasticValue> listKpiElasticValues = new ArrayList<>();

		BoolQueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery("sensorID", sensorID));
		if (from != null || to != null) {
			RangeQueryBuilder rqb = QueryBuilders.rangeQuery("date_time");
			if (from != null) {
				rqb.gte(from);
			}
			if (to != null) {
				rqb.lte(to);
			}

			qb.must(rqb);
		}

		SortOrder sortOrder = SortOrder.ASC;
		if (last != null) {
			sortOrder = SortOrder.DESC;
		}
		try {
			if (first == null && last == null) {
				searchResponse = elasticSearchClient.search(
						new SearchRequest(ConfigIndexBean.getIndexName()).source(new SearchSourceBuilder().query(qb)
								.sort("date_time", sortOrder).size(countDocumentsBySensorId(sensorID).intValue())),
						RequestOptions.DEFAULT);
			} else if (first != null) {
				searchResponse = elasticSearchClient.search(
						new SearchRequest(ConfigIndexBean.getIndexName())
								.source(new SearchSourceBuilder().query(qb).sort("date_time", sortOrder).size(first)),
						RequestOptions.DEFAULT);
			} else {
				searchResponse = elasticSearchClient.search(
						new SearchRequest(ConfigIndexBean.getIndexName())
								.source(new SearchSourceBuilder().query(qb).sort("date_time", sortOrder).size(last)),
						RequestOptions.DEFAULT);
			}

			SearchHit[] searchHits = searchResponse.getHits().getHits();
			for (SearchHit searchHit : searchHits) {
				String hitJson = searchHit.getSourceAsString();
				ObjectMapper objectMapper = new ObjectMapper();
				KPIElasticValue kpiElasticValue = objectMapper.readValue(hitJson, KPIElasticValue.class);
				listKpiElasticValues.add(kpiElasticValue);
			}
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		return listKpiElasticValues;
	}

	@Override
	public Long countDocumentsBySensorId(String sensorID) {

		SearchResponse searchResponse = null;

		BoolQueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchPhraseQuery("sensorID", sensorID));

		try {
			searchResponse = elasticSearchClient.search(
					new SearchRequest(ConfigIndexBean.getIndexName()).source(new SearchSourceBuilder().query(qb)),
					RequestOptions.DEFAULT);
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}

		if (searchResponse != null) {
			return searchResponse.getHits().getTotalHits().value;
		}
		return 0L;

	}

}