package org.han.cpca;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie;
import com.hankcs.algorithm.AhoCorasickDoubleArrayTrie.Hit;

public class CpcaExtractorImpl implements CpcaExtractor {
	private final static Logger logger = LoggerFactory.getLogger(CpcaExtractorImpl.class);
	private final static int CPCA_CODE_SIZE = 12;
	private final static int CPCA_CODE_PROV_SIZE = 2;
	private final static int CPCA_CODE_CITY_SIZE = 4;
	private final static int CPCA_CODE_COUNTY_SIZE = 6;
	private static ObjectMapper mapper = new ObjectMapper();
	static {
		// 忽略在JSON字符串中存在但Java对象实际没有的属
		// mapper.getSerializationConfig().
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}
	/**
	 * 文件格式： adcode,name,longitude,latitude 500233000000,忠县,108.039002,30.299559
	 * 410506000000,龙安区,114.301331,36.076225 . . .
	 * 441700000000,阳江市,111.982589,21.857887
	 */
	private String cpcaCvsFile;
	private Map<String, AddressInfo> addressInfoMap = new HashMap<>(3500);
	// 停用词包括: 省, 市, 特别行政区, 自治区。之所以 区 和 县 不作为停用词，是因为 区县 数目太多, 去掉 "区" 字 或者 "县" 字后很容易误配
	private Pattern stopKey = Pattern.compile("([省市]|特别行政区|自治区)$");
	// 自治区简写
	private Map<String, String> zzqSimplify = new HashMap<>();
	// 道路带省或市的
	private List<String> loadNameSuffixs_2 = Arrays.asList("大路", "大道", "大街", "东路", "南路", "西路", "北路", "东街", "南街", "西街",
			"北街", "街道", "胡同");
	private List<String> loadNameSuffixs_1 = Arrays.asList("路", "街", "道");
	// 村
	private List<String> villageNameSuffixs_1 = Arrays.asList("村", "屯", "庄", "家", "山", "河", "沟", "湾", "坪", "塘", "坝",
			"岗", "场", "湖", "岭", "堡", "坡", "峪", "岩", "溪", "凼", "岛");
	//
	AhoCorasickDoubleArrayTrie<MatchAddressInfo> acdat = new AhoCorasickDoubleArrayTrie<>();

	/**
	 * 
	 * @param cpcaCvsFile 
	 * 文件内容来源：
	 * https://github.com/DQinYuan/chinese_province_city_area_mapper/blob/master/cpca/resources/adcodes.csv
	 * https://github.com/Vonng/adcode
	 * http://www.stats.gov.cn/tjsj/tjbz/tjyqhdmhcxhfdm/
	 */
	public CpcaExtractorImpl(String cpcaCvsFile) {
		this.cpcaCvsFile = cpcaCvsFile;
		zzqSimplify.put("内蒙古自治区", "内蒙古");
		zzqSimplify.put("广西壮族自治区", "广西");
		zzqSimplify.put("西藏自治区", "西藏");
		zzqSimplify.put("新疆维吾尔自治区", "新疆");
		zzqSimplify.put("宁夏回族自治区", "宁夏");
		zzqSimplify.put("本溪满族自治县", "本溪县");
		zzqSimplify.put("乳源瑶族自治县", "乳源县");
		zzqSimplify.put("宁蒗彝族自治县", "宁蒗县");
		zzqSimplify.put("伊通满族自治县", "伊通县");
		zzqSimplify.put("威宁彝族回族苗族自治县", "威宁县");
		zzqSimplify.put("长白朝鲜族自治县", "长白县");
		zzqSimplify.put("互助土族自治县", "互助县");
		zzqSimplify.put("围场满族蒙古族自治县", "围场县");
		zzqSimplify.put("紫云苗族布依族自治县", "紫云县");
		zzqSimplify.put("湘西土家族苗族自治州", "湘西州");
		zzqSimplify.put("紫云苗族布依族自治县", "紫云县");
		zzqSimplify.put("杜尔伯特蒙古族自治县", "杜尔伯特县");
		zzqSimplify.put("寻甸回族彝族自治县", "寻甸县");
		zzqSimplify.put("峨山彝族自治县", "峨山县");
		zzqSimplify.put("景东彝族自治县", "景东县");
		zzqSimplify.put("清原满族自治县", "清原县");
		zzqSimplify.put("长阳土家族自治县", "长阳县");
		zzqSimplify.put("白沙黎族自治县", "白沙县");
		zzqSimplify.put("维西傈僳族自治县", "维西县");
		zzqSimplify.put("肃南裕固族自治县", "肃南县");
		zzqSimplify.put("元江哈尼族彝族傣族自治县", "元江县");
		zzqSimplify.put("都安瑶族自治县", "都安县");
		zzqSimplify.put("双江拉祜族佤族布朗族傣族自治县", "双江县");
		zzqSimplify.put("岫岩满族自治县", "岫岩县");
		zzqSimplify.put("化隆回族自治县", "化隆县");
		zzqSimplify.put("喀喇沁左翼蒙古族自治县", "喀喇沁左翼县");
		zzqSimplify.put("桓仁满族自治县", "桓仁县");
		zzqSimplify.put("木垒哈萨克自治县", "木垒县");
		zzqSimplify.put("三江侗族自治县", "三江县");
		zzqSimplify.put("关岭布依族苗族自治县", "关岭县");
		zzqSimplify.put("焉耆回族自治县", "焉耆县");
		zzqSimplify.put("北川羌族自治县", "北川县");
		zzqSimplify.put("宽城满族自治县", "宽城县");
		zzqSimplify.put("镇沅彝族哈尼族拉祜族自治县", "镇沅县");
		zzqSimplify.put("孟连傣族拉祜族佤族自治县", "孟连县");
		zzqSimplify.put("琼中黎族苗族自治县", "琼中县");
		zzqSimplify.put("务川仡佬族苗族自治县", "务川县");
		zzqSimplify.put("贡山独龙族怒族自治县", "贡山县");
		zzqSimplify.put("孟村回族自治县", "孟村县");
		zzqSimplify.put("天祝藏族自治县", "天祝县");
		zzqSimplify.put("宽甸满族自治县", "宽甸县");
		zzqSimplify.put("峨边彝族自治县", "峨边县");
		zzqSimplify.put("河南蒙古族自治县", "河南蒙旗");
		zzqSimplify.put("河南蒙古族自治县", "河南蒙旗县");
		zzqSimplify.put("通道侗族自治县", "通道县");
		zzqSimplify.put("澜沧拉祜族自治县", "澜沧县");
		zzqSimplify.put("陵水黎族自治县", "陵水县");
		zzqSimplify.put("马边彝族自治县", "马边县");
		zzqSimplify.put("张家川回族自治县", "张家川县");
		zzqSimplify.put("民和回族土族自治县", "民和县");
		zzqSimplify.put("漾濞彝族自治县", "漾濞县");
		zzqSimplify.put("前郭尔罗斯蒙古族自治县", "前郭县");
		zzqSimplify.put("玉屏侗族自治县", "玉屏县");
		zzqSimplify.put("连山壮族瑶族自治县", "连山县");
		zzqSimplify.put("禄劝彝族苗族自治县", "禄劝县");
		zzqSimplify.put("彭水苗族土家族自治县", "彭水县");
		zzqSimplify.put("循化撒拉族自治县", "循化县");
		zzqSimplify.put("河口瑶族自治县", "河口县");
		zzqSimplify.put("融水苗族自治县", "融水县");
		try {
			loadCpca();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
	}

	@Override
	public CpcaSeg transform(String location, Map<String, String> umap, boolean strictlyMatch) {
		if (location == null || location.trim().equals("")) {
			return CpcaSeg.none();
		}
		CpcaSeg cpcaSeg = CpcaSeg.none();
		AddressInfo lastAddressInfo = null;
		String cpcaCode = null;
		int endIndex = 0;
		Hit<MatchAddressInfo> provMatchAddressInfo = null;
		Hit<MatchAddressInfo> cityMatchAddressInfo = null;
		boolean provMatched = false;
		boolean cityMatched = false;
		boolean provFullMatched = false;
		boolean cityFullMatched = false;
		boolean areaFullMatched = false;
		List<Hit<MatchAddressInfo>> hits1 = acdat.parseText(location);
		List<Hit<MatchAddressInfo>> hits2 = new ArrayList<>(hits1.size());
		for (int i = 0; i < hits1.size(); i++) {
			// 省、市、区 可能会两次匹配到，比如杭州市xxxxxx这个地址，会先后匹配到杭州、杭州市，这里只取杭州市这次匹配
			Hit<MatchAddressInfo> hit = hits1.get(i);
			if (hits2.contains(hit)) {
				continue;
			}
			boolean exists = false;//区域已经存在，过滤到重复匹配到的
			for (Hit<MatchAddressInfo> hitTmp : hits2) {
				if (hitTmp.value.getAddressInfos() == hit.value.getAddressInfos()) {
					exists = true;
					break;
				}
			}
			if (exists) {
				continue;
			}
			if (i < (hits1.size() - 1)) {
				Hit<MatchAddressInfo> hit2 = hits1.get(i + 1);
				if (hit2.value.getAddressInfos() == hit.value.getAddressInfos()) {
					hit2.value.setFullName(true);
					hits2.add(hit2);
					continue;
				}
			}
			hits2.add(hit);
		}
		try {
			for (Hit<MatchAddressInfo> hit : hits2) {
				// 按省、市、区顺序来匹配和处理
				MatchAddressInfo matchAddressInfo = hit.value;
				boolean thisTurnProvMatched = !provMatched && matchAddressInfo.getAddressInfos().stream()
						.map(AddressInfo::getRank).filter(k -> k == Rank.RANK_PROVINCE).findAny().isPresent();
				provMatched = provMatched ? provMatched : thisTurnProvMatched;
				provMatchAddressInfo = provMatchAddressInfo != null ? provMatchAddressInfo
						: (thisTurnProvMatched ? hit : null);
				boolean thisTurnCityMatched = !thisTurnProvMatched && !cityMatched && matchAddressInfo.getAddressInfos()
						.stream().map(AddressInfo::getRank).filter(k -> k == Rank.RANK_CITY).findAny().isPresent();
				cityMatched = cityMatched ? cityMatched : thisTurnCityMatched;
				cityMatchAddressInfo = cityMatchAddressInfo != null ? cityMatchAddressInfo
						: (thisTurnCityMatched ? hit : null);
				if (thisTurnProvMatched) {
					updateCpcaSegIndex(cpcaSeg, Rank.RANK_PROVINCE, hit.begin, hit.end);
				} else if (thisTurnCityMatched) {
					updateCpcaSegIndex(cpcaSeg, Rank.RANK_CITY, hit.begin, hit.end);
				}
				boolean thisTurnProvFullMatched = !provFullMatched && matchAddressInfo.isFullName()
						&& matchAddressInfo.getAddressInfos().stream().map(AddressInfo::getRank)
								.filter(k -> k == Rank.RANK_PROVINCE).findAny().isPresent();
				provFullMatched = provFullMatched ? provFullMatched : thisTurnProvFullMatched;
				boolean thisTurnCityFullMatched = !thisTurnProvFullMatched && !cityFullMatched
						&& matchAddressInfo.isFullName() && matchAddressInfo.getAddressInfos().stream()
								.map(AddressInfo::getRank).filter(k -> k == Rank.RANK_CITY).findAny().isPresent();
				cityFullMatched = cityFullMatched ? cityFullMatched : thisTurnCityFullMatched;
				areaFullMatched = areaFullMatched ? areaFullMatched
						: (!thisTurnCityFullMatched && !areaFullMatched && matchAddressInfo.isFullName()
								&& matchAddressInfo.getAddressInfos().stream().map(AddressInfo::getRank)
										.filter(k -> k == Rank.RANK_COUNTY).findAny().isPresent());
				String matchName = matchAddressInfo.getMatchName();
				String firstCpca = umap == null ? null : umap.get(matchName);
				AddressInfo currentAddressInfo = matchAddressInfo.matchAddressInfo(lastAddressInfo, firstCpca,
						strictlyMatch);
				if (currentAddressInfo == null && !provFullMatched && !cityFullMatched) {
					currentAddressInfo = matchAddressInfo.matchAddressInfo(null, firstCpca, strictlyMatch);
				}
				if (currentAddressInfo != null) {
					cpcaCode = currentAddressInfo.getCpcaCode();
					endIndex = hit.end;
					if (currentAddressInfo.rank == Rank.RANK_COUNTY) {
						// 匹配到区县就停止
						if (!provFullMatched && !cityFullMatched && !areaFullMatched) {
							boolean isUnit = isUnit(endIndex, location);
							if (isUnit) {
								cpcaSeg.reset();
								cpcaSeg.setAddress(location);
								return cpcaSeg;
							}
						}
						if (lastAddressInfo != null && !currentAddressInfo.belongTo(lastAddressInfo)) {
							cpcaSeg.reset();
						}
						if (hit.begin > 0 && !provMatched && !cityMatched) {
							// 区县前面有内容，但不是省和市
							cpcaSeg.setAddress(location);
							return cpcaSeg;
						}
						updateCpcaSeg(cpcaSeg, cpcaCode);
						cpcaSeg.setCpcaCode(cpcaCode);
						cpcaSeg.setAddress(substrLocation(location, hit.end));
						updateCpcaSegIndex(cpcaSeg, matchAddressInfo.getAddressInfos().get(0).rank, hit.begin, hit.end);
						return cpcaSeg;
					}
					lastAddressInfo = currentAddressInfo;
				} else if (provFullMatched || cityFullMatched) {
					;
				} else if (matchAddressInfo.getAddressInfos().get(0).rank == Rank.RANK_COUNTY && (cityMatched)
						&& matchName.startsWith(simplifyName(lastAddressInfo.getName()))) {
					// 存在市、县同名情况 && 县同名情况，当作未匹配处理
					cpcaCode = null;
				} else if (cityMatched && isArea(cityMatchAddressInfo.end, location)) {
					// 处理如杭州区
					cpcaCode = null;
				} else if (provMatched && isArea(provMatchAddressInfo.end, location)) {
					// 处理如杭州区
					cpcaCode = null;
				}
			}
			if (cpcaCode != null) {
				// 匹配到省或市
				if (provFullMatched || cityFullMatched) {
					updateCpcaSeg(cpcaSeg, cpcaCode);
					cpcaSeg.setCpcaCode(cpcaCode);
					cpcaSeg.setAddress(location.substring(endIndex));
					return cpcaSeg;
				} else {
					if (provMatched && cityMatched) {
						if (cpcaSeg.getCityNameIndex().getBeginIndex()
								- cpcaSeg.getProvinceNameIndex().getEndIndex() == 0) {
							updateCpcaSeg(cpcaSeg, cpcaCode);
							cpcaSeg.setCpcaCode(cpcaCode);
							cpcaSeg.setAddress(location.substring(endIndex));
							return cpcaSeg;
						} else if (cpcaSeg.getProvinceNameIndex().getEndIndex() < cpcaSeg.getCityNameIndex()
								.getBeginIndex()
								&& location.substring(cpcaSeg.getProvinceNameIndex().getEndIndex(),
										cpcaSeg.getCityNameIndex().getBeginIndex()).trim().equals("")) {
							updateCpcaSeg(cpcaSeg, cpcaCode);
							cpcaSeg.setCpcaCode(cpcaCode);
							cpcaSeg.setAddress(location.substring(endIndex));
							return cpcaSeg;
						} else {
							cpcaSeg.reset();
							cpcaSeg.setAddress(location);
							return cpcaSeg;
						}
					}
					int beginIdx = -1;
					int endIdx = -1;
					if (provMatched) {
						beginIdx = cpcaSeg.getProvinceNameIndex().getBeginIndex();
						endIdx = cpcaSeg.getProvinceNameIndex().getEndIndex();
					} else if (cityMatched) {
						beginIdx = cpcaSeg.getCityNameIndex().getBeginIndex();
						endIdx = cpcaSeg.getCityNameIndex().getEndIndex();
					}
					if (beginIdx >= 0) {
						boolean isUnit = isUnit(endIdx, location);
						if (isUnit) {
							// 如果是单位（如镇、村、大厦、中学等）
							cpcaSeg.reset();
							cpcaSeg.setAddress(location);
							return cpcaSeg;
						}
						if (beginIdx > 0) {
							// 未匹配到省、市全称的，匹配到省或市，认为是地址
							cpcaSeg.reset();
							cpcaSeg.setAddress(location);
							return cpcaSeg;
						}
						updateCpcaSeg(cpcaSeg, cpcaCode);
						cpcaSeg.setCpcaCode(cpcaCode);
						cpcaSeg.setAddress(location.substring(endIndex));
						return cpcaSeg;
					} else {
						cpcaSeg.reset();
						cpcaSeg.setAddress(location);
						return cpcaSeg;
					}
				}
			} else {
				cpcaSeg.reset();
				cpcaSeg.setAddress(location);
			}
			return cpcaSeg;
		} finally {
			hits1.stream().forEach(k->{
				k.value.removeFullName();
			});
		}
	}

	private boolean isUnit(int endIdx, String location) {
		// 处理如xx镇
		boolean isTown = isTown(endIdx, location);
		if (isTown) {
			return true;
		}
		boolean isRoad = isRoad(endIdx, location);
		if (isRoad) {
			return true;
		}
		// 处理如上海大厦
		boolean isBuilding = isBuilding(endIdx, location);
		if (isBuilding) {
			return true;
		}
		// 处理如温州村
		boolean isVillage = isVillage(endIdx, location);
		if (isVillage) {
			return true;
		}
		// 处理如杭州中学
		boolean isSchool = isSchool(endIdx, location);
		if (isSchool) {
			return true;
		}
		return false;
	}

	private String substrLocation(String location, int end) {
		int len = location.length();
		return end >= len ? null : location.substring(end);
	}

	private boolean isTown(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 1) {
			String str = location.substring(endIndex, endIndex + 1);
			return (str.equals("镇") || str.equals("乡"));
		}
		return false;
	}

	/**
	 * 是否道路
	 * 
	 * @param endIndex
	 * @param location
	 * @return
	 */
	private boolean isRoad(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 2) {
			String str = location.substring(endIndex, endIndex + 2);
			if (loadNameSuffixs_2.contains(str)) {
				return true;
			}
			List<String> nums = Arrays.asList("一", "二", "三", "四", "五", "六", "七", "八", "九", "十");
			for (String num : nums) {
				for (String loadName : loadNameSuffixs_1) {
					if (str.equals(num + loadName)) {
						return true;
					}
				}
			}
		}
		if (len - endIndex >= 1) {
			String str = location.substring(endIndex, endIndex + 1);
			return (loadNameSuffixs_1.contains(str));
		}
		return false;
	}

	private boolean isBuilding(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 2) {
			String str = location.substring(endIndex, endIndex + 2);
			if (str.equals("大厦")) {
				return true;
			}
		}
		return false;
	}

	private boolean isVillage(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 1) {
			String str = location.substring(endIndex, endIndex + 1);
			return (villageNameSuffixs_1.contains(str));
		}
		return false;
	}

	private boolean isSchool(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 2) {
			String str = location.substring(endIndex, Math.min(endIndex + 6, len));
			return str.contains("幼儿园") || str.contains("小学") || str.contains("中学") || str.contains("学院")
					|| str.contains("大学");
		}
		return false;
	}

	private boolean isArea(int endIndex, String location) {
		int len = location.length();
		if (len - endIndex >= 1) {
			String str = location.substring(endIndex, endIndex + 1);
			return (str.equals("县") || str.equals("区"));
		}
		return false;
	}

	private void updateCpcaSeg(CpcaSeg cpcaSeg, String cpcaCode) {
		if (cpcaCode.endsWith("0000")) {
			cpcaSeg.setProvinceName(getCpcaName(cpcaCode.substring(0, CPCA_CODE_PROV_SIZE)));
			return;
		}
		if (cpcaCode.endsWith("00")) {
			cpcaSeg.setProvinceName(getCpcaName(cpcaCode.substring(0, CPCA_CODE_PROV_SIZE)));
			cpcaSeg.setCityName(getCpcaName(cpcaCode.substring(0, CPCA_CODE_CITY_SIZE)));
			return;
		}
		cpcaSeg.setProvinceName(getCpcaName(cpcaCode.substring(0, CPCA_CODE_PROV_SIZE)));
		cpcaSeg.setCityName(getCpcaName(cpcaCode.substring(0, CPCA_CODE_CITY_SIZE)));
		cpcaSeg.setAreaName(getCpcaName(cpcaCode));
	}

	private void updateCpcaSegIndex(CpcaSeg cpcaSeg, Rank rank, int begin, int end) {
		switch (rank) {
		case RANK_PROVINCE:
			cpcaSeg.setProvinceNameIndex(new CpcaIndex(begin, end));
			break;
		case RANK_CITY:
			cpcaSeg.setCityNameIndex(new CpcaIndex(begin, end));
			break;
		case RANK_COUNTY:
			cpcaSeg.setAreaNameIndex(new CpcaIndex(begin, end));
			break;
		default:
			break;
		}
	}

	private String getCpcaName(String partyCpcaCode) {
		String cpcaCode = partyCpcaCode + String.format("%0" + (CPCA_CODE_SIZE - partyCpcaCode.length()) + "d", 0);
		AddressInfo addressInfo = addressInfoMap.get(cpcaCode);
		return addressInfo == null ? null : addressInfo.getName();
	}

	private void loadCpca() throws IOException {
		String fileName;
		if (cpcaCvsFile == null || cpcaCvsFile.equals("")) {
			throw new IOException("input file is null");
		} else {
			if (new File(cpcaCvsFile).isFile()) {
				fileName = cpcaCvsFile;
			} else {
				URL url = CpcaExtractorImpl.class.getResource("/META-INF/" + cpcaCvsFile);
				if (url == null) {
					url = CpcaExtractorImpl.class.getResource("/eet/evar/" + cpcaCvsFile);
				}
				if (url == null) {
					url = CpcaExtractorImpl.class.getResource('/' + cpcaCvsFile);
				}
				if (url == null) {
					throw new IOException("File not found: " + cpcaCvsFile);
				} else {
					fileName = URLDecoder.decode(url.getFile(), "UTF-8");
				}
			}
		}
		InputStream in = null;
		if (fileName.contains("BOOT-INF/classes")) {
			in = CpcaExtractorImpl.class.getResourceAsStream("/" + cpcaCvsFile);
		} else {
			in = new FileInputStream(new File(fileName));
		}
		TreeMap<String, MatchAddressInfo> acMap = new TreeMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = reader.readLine();
		// 第一行 adcode,name,longitude,latitude 跳过处理
		line = reader.readLine();
		while (line != null) {
			String[] fields = split(line, ",");
			String name = fields[1];
			String cpcaCode = fields[0];
			AddressInfo addressInfo = new AddressInfo(name, cpcaCode);
			addressInfoMap.put(cpcaCode, addressInfo);
			MatchAddressInfo matchAddressInfo = acMap.get(name);
			if (matchAddressInfo != null) {
				matchAddressInfo.addAddressInfo(addressInfo);
			} else {
				List<AddressInfo> addressInfos = new ArrayList<>(Arrays.asList(addressInfo));
				acMap.put(name, new MatchAddressInfo(name, addressInfos));
				String simplifyName = simplifyName(name);
				if (!simplifyName.equals(name)) {
					acMap.put(simplifyName, new MatchAddressInfo(simplifyName, addressInfos));
				}
			}
			line = reader.readLine();
		}
		reader.close();
		acdat.build(acMap);
		logger.info("cpca字典导入完成，共  {} 条记录", addressInfoMap.size());
	}

	private String simplifyName(String name) {
		return this.zzqSimplify.getOrDefault(name, this.stopKey.matcher(name).replaceFirst(""));
	}

	class MatchAddressInfo {
		private String matchName;
		private List<AddressInfo> addressInfos;
		private ThreadLocal<Boolean> isFullName = new ThreadLocal<>();

		public MatchAddressInfo(String matchName, List<AddressInfo> addressInfos) {
			this.matchName = matchName;
			this.addressInfos = addressInfos;
		}

		public AddressInfo matchAddressInfo(AddressInfo parentAddressInfo, String firstCpca) {
			return matchAddressInfo(parentAddressInfo, firstCpca, false);
		}

		public AddressInfo matchAddressInfo(AddressInfo parentAddressInfo, String firstCpca, boolean strictlyMatch) {
			if (parentAddressInfo != null) {
				AddressInfo addressInfo = this.addressInfos.stream().filter(p -> p.belongTo(parentAddressInfo))
						.findFirst().orElse(null);
				if (addressInfo != null) {
					return addressInfo;
				}
				// 存在市、县简称相同的情况，会先匹配到市，实际应该匹配到县
				if (parentAddressInfo.rank == Rank.RANK_CITY
						&& this.matchName.startsWith(simplifyName(parentAddressInfo.getName()))) {
					return matchAddressInfo(null, firstCpca, strictlyMatch);
				}
				return null;
			} else if (firstCpca != null) {
				return this.addressInfos.stream().filter(p -> p.getCpcaCode().equals(firstCpca)).findFirst()
						.orElse(null);
			} else {
				return strictlyMatch ? (addressInfos.size() == 1 ? addressInfos.get(0)
						: (addressInfos.stream().map(AddressInfo::getRank).distinct().count() > 1 ? addressInfos.get(0)
								: null))
						: addressInfos.get(0);
			}
		}

		public void addAddressInfo(AddressInfo addressInfo) {
			if (this.addressInfos == null) {
				this.addressInfos = new ArrayList<>(Arrays.asList(addressInfo));

			} else {
				if (this.addressInfos.isEmpty()) {
					this.addressInfos.add(addressInfo);
				} else {
					AddressInfo prevAddressInfo = this.addressInfos.get(this.addressInfos.size() - 1);
					if (prevAddressInfo.rank.value > addressInfo.rank.value) {
						this.addressInfos.add(this.addressInfos.size() - 1, addressInfo);
					} else {
						this.addressInfos.add(addressInfo);
					}
				}
			}
		}

		public String getMatchName() {
			return matchName;
		}

		public void setMatchName(String matchName) {
			this.matchName = matchName;
		}

		public List<AddressInfo> getAddressInfos() {
			return addressInfos;
		}

		public void setAddressInfos(List<AddressInfo> addressInfos) {
			this.addressInfos = addressInfos;
		}

		public boolean isFullName() {
			return isFullName.get() == null ? false : isFullName.get();
		}

		public void setFullName(boolean isFullName) {
			this.isFullName.set(isFullName);
		}
		
		public void removeFullName() {
			this.isFullName.remove();
		}
	}

	class CpcaCode {
		private String cpcaCode;
		private Rank rank;

		public CpcaCode(String cpcaCode) {
			this.cpcaCode = cpcaCode.substring(0, CPCA_CODE_COUNTY_SIZE);// 前6位代表省、市、县
			rank = this.cpcaCode.endsWith("0000") ? Rank.RANK_PROVINCE
					: (this.cpcaCode.endsWith("00") ? Rank.RANK_CITY : Rank.RANK_COUNTY);
		}

		public String getCpcaCode() {
			return cpcaCode;
		}

		public void setCpcaCode(String cpcaCode) {
			this.cpcaCode = cpcaCode;
		}

		public Rank getRank() {
			return rank;
		}

		public void setRank(Rank rank) {
			this.rank = rank;
		}
	}

	class AddressInfo {
		private String name;
		private String cpcaCode;
		private Rank rank;

		public AddressInfo(String name, String cpcaCode) {
			this.name = name;
			CpcaCode cpcaCodeObj = new CpcaCode(cpcaCode);
			this.cpcaCode = cpcaCodeObj.getCpcaCode();
			rank = cpcaCodeObj.getRank();
		}

		public boolean belongTo(AddressInfo other) {
			// cpca 编码 省、市、县 长度各为 2、4、6
			return this.cpcaCode.startsWith(other.cpcaCode.substring(0, (other.rank.value + 1) * 2));
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCpcaCode() {
			return cpcaCode;
		}

		public void setCpcaCode(String cpcaCode) {
			this.cpcaCode = cpcaCode;
		}

		public Rank getRank() {
			return rank;
		}
	}

	enum Rank {
		RANK_PROVINCE(CPCA_CODE_PROV_SIZE / 2 - 1, "省"), // 0
		RANK_CITY(CPCA_CODE_CITY_SIZE / 2 - 1, "市"), // 1
		RANK_COUNTY(CPCA_CODE_COUNTY_SIZE / 2 - 1, "县");// 2

		private int value;
		private String name;

		public String getName() {
			return name;
		}

		Rank(int value, String name) {
			this.value = value;
			this.name = name;
		}
	}
	
	String encodeJson(Object obj) {
		if (obj == null) {
			return null;
		}
		if (obj instanceof String) {
			return (String) obj;
		}
		try {
			return mapper.writeValueAsString(obj);
		} catch (JsonGenerationException e) {
			logger.error("encode(Object)", e); //$NON-NLS-1$
		} catch (JsonMappingException e) {
			logger.error("encode(Object)", e); //$NON-NLS-1$
		} catch (IOException e) {
			logger.error("encode(Object)", e); //$NON-NLS-1$
		}
		return null;
	}
	
	private String[] split(String source, String div) {
		if (source == null || div == null) {
			return null;
		}
		int arynum = 0, intIdx = 0, intIdex = 0, div_length = div.length();
		if (source.compareTo("") != 0) {
			if (source.indexOf(div) != -1) {
				intIdx = source.indexOf(div);
				for (int intCount = 1;; intCount++) {
					if (source.indexOf(div, intIdx + div_length) != -1) {
						intIdx = source.indexOf(div, intIdx + div_length);
						arynum = intCount;
					} else {
						arynum += 2;
						break;
					}
				}
			} else
				arynum = 1;
		} else
			arynum = 0;

		intIdx = 0;
		intIdex = 0;
		String[] returnStr = new String[arynum];

		if (source.compareTo("") != 0) {
			if (source.indexOf(div) != -1) {
				intIdx = (int) source.indexOf(div);
				returnStr[0] = (String) source.substring(0, intIdx);
				for (int intCount = 1;; intCount++) {
					if (source.indexOf(div, intIdx + div_length) != -1) {
						intIdex = (int) source.indexOf(div, intIdx + div_length);
						returnStr[intCount] = (String) source.substring(intIdx + div_length, intIdex);
						intIdx = (int) source.indexOf(div, intIdx + div_length);
					} else {
						returnStr[intCount] = (String) source.substring(intIdx + div_length, source.length());
						break;
					}
				}
			} else {
				returnStr[0] = (String) source.substring(0, source.length());
				return returnStr;
			}
		} else {
			return returnStr;
		}
		return returnStr;
	}

	public static void main(String[] args) {
		CpcaExtractorImpl cpcaExtractor = new CpcaExtractorImpl("adcodes.csv");
		CpcaSeg cpcaSeg = cpcaExtractor.transform("浙江省杭州市拱墅区祥园路300号");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("浙江杭州市拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("杭州市拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("杭州拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("拱墅区");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals(null, cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("浙江省祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		cpcaSeg = cpcaExtractor.transform("浙江省路祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("路祥园路300号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("上海路990号");
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("上海路990号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店");
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("朝阳区汉庭酒店大山子店", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		Map<String, String> umap = new HashMap<>();
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店", umap);
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("长春市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("汉庭酒店大山子店", cpcaSeg.getAddress());
		System.out.println(cpcaSeg.noPca());
		umap.put("朝阳区", "110105");
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店", umap);
		assertEquals("北京市", cpcaSeg.getProvinceName());
		assertEquals("北京市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("汉庭酒店大山子店", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("朝阳区朝阳区汉庭酒店大山子店", umap);
		assertEquals("北京市", cpcaSeg.getProvinceName());
		assertEquals("北京市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("朝阳区汉庭酒店大山子店", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("徐汇区虹漕路461号58号楼5楼", umap);
		assertEquals("上海市", cpcaSeg.getProvinceName());
		assertEquals("上海市", cpcaSeg.getCityName());
		assertEquals("徐汇区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("上海市上海市徐汇区虹漕路461号58号楼5楼", umap);
		assertEquals("上海市", cpcaSeg.getProvinceName());
		assertEquals("上海市", cpcaSeg.getCityName());
		assertEquals("徐汇区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("吉林省吉林市龙潭区虹漕路461号58号楼5楼", umap);
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("吉林市", cpcaSeg.getCityName());
		assertEquals("龙潭区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("吉林龙潭区虹漕路461号58号楼5楼", umap);
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("龙潭区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("拱墅祥园路100号", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("拱墅祥园路100号", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("古墩路889号温州村", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("古墩路889号温州村", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("白云区龙湖工业三街一号多城国际", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("白云区龙湖工业三街一号多城国际", cpcaSeg.getAddress());
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("前郭县百顺堂药业有限公司");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("五大连池市红旗路100号");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("湖北仙桃红旗路100号");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("万宁市万宁中学");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		assertEquals("海南省", cpcaSeg.getProvinceName());
		assertEquals("省直辖县级行政区划", cpcaSeg.getCityName());
		assertEquals("万宁市", cpcaSeg.getAreaName());
		assertEquals("万宁中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("万宁中学");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("万宁中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州市第十中学");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("第十中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州市杭州第十中学");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
		System.out.println(cpcaSeg.noPca());
		System.out.println(cpcaSeg.fullPca());
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("杭州第十中学", cpcaSeg.getAddress());
		// int count = 1000000;
		// Long t1 = System.currentTimeMillis();
		// for(int i=0;i<count;i++) {
		// cpcaSeg = cpcaExtractor.transform("上海市上海市徐汇区虹漕路461号58号楼5楼", umap);
		// assertTrue(cpcaSeg.fullPca());
		// }
		// Long t2 = System.currentTimeMillis();
		// System.out.println(count/Math.max(1, (t2-t1)/1000));
	}
}
