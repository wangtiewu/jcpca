# jcpca
java版本用于从中国地址提取省、市、区（县）<br>
实现和使用主要参考：https://github.com/DQinYuan/chinese_province_city_area_mapper<br>
分词器工具：https://github.com/hankcs/AhoCorasickDoubleArrayTrie<br>
省市区数据来自数据来自国家统计局网站公开数据<br>

#Get Started<br>
CpcaExtractorImpl cpcaExtractor = new CpcaExtractorImpl("adcodes.csv");<br>
CpcaSeg cpcaSeg = cpcaExtractor.transform("浙江省杭州市拱墅区祥园路300号");<br>
System.out.println(cpcaExtractor.encodeJson(cpcaSeg));<br>

结果显示：<br>
{"provinceName":"浙江省","cityName":"杭州市","areaName":"拱墅区","cpcaCode":"330105","address":"祥园路300号","provinceNameIndex":{"beginIndex":0,"endIndex":3},"cityNameIndex":{"beginIndex":3,"endIndex":6},"areaNameIndex":{"beginIndex":6,"endIndex":9}}
