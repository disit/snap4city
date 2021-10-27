package edu.unifi.disit.datamanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigIndexBean {
 
        private static String indexName;
        private static String kpiDefaultSaveOn;

        @Value("${elasticsearch.indexname}")
        private void setIndexNameStatic(String name){
            ConfigIndexBean.indexName = name;
        }
        
	public static final String getIndexName() {
		return indexName;
	}

	public static void setIndexName(String indexName) {
		ConfigIndexBean.indexName = indexName;
	}
	
        @Value("${config.kpi.defaultsaveon}")
        private void setKpiDefaultSaveOnStatic(String name){
            ConfigIndexBean.kpiDefaultSaveOn = name;
        }
        
	public static final String getKpiDefaultSaveOn() {
		return kpiDefaultSaveOn;
	}

	public static void setKpiDefaultSaveOn(String kpiDefaultSaveOn) {
		ConfigIndexBean.kpiDefaultSaveOn = kpiDefaultSaveOn;
	}
	
}