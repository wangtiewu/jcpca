package org.han.cpca;

import java.util.Map;

/**
 * 中国省、市、区提取器
 * 主要参考 https://github.com/DQinYuan/chinese_province_city_area_mapper/
 *
 */
public interface CpcaExtractor {
	default CpcaSeg transform(String location) {
		return transform(location, true);
	}
	
	default CpcaSeg transform(String location, boolean strictlyMatch) {
		return strictlyMatch ? transform(location, null, true) : transform(location, null, false);
	}
	
	default CpcaSeg transform(String location, Map<String, String> umap) {
		return transform(location, umap, umap != null && !umap.isEmpty() ? true : false);
	}
	
	/**
	 * 
	 * @param location
	 * @param umap 当只有区的信息时， 且该区存在同名时， 指定该区具体是哪一个，字典的 key 为区名，value 为 adcode， 比如 {"朝阳区": "110105"}
	 * @param strictlyMatch 严格匹配，存在同名区县时当作未匹配处理
	 * @return
	 */
	public CpcaSeg transform(String location, Map<String, String> umap, boolean strictlyMatch);
}
