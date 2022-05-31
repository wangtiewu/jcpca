package org.han.cpca;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class CpcaExtractorImplTest {
	CpcaExtractorImpl cpcaExtractor = null;

	@Before
	public void setUp() throws Exception {
		cpcaExtractor = new CpcaExtractorImpl("adcodes.csv");
	}

	@Test
	public void test() {
		CpcaSeg cpcaSeg = cpcaExtractor.transform("浙江省杭州市拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("浙江杭州市拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州市拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("拱墅区祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("拱墅区");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals("拱墅区", cpcaSeg.getAreaName());
		assertEquals(null, cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("浙江省祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("祥园路300号", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("浙江省路祥园路300号");
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("路祥园路300号", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("上海路990号");
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("上海路990号", cpcaSeg.getAddress());
		assertTrue(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店");
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("朝阳区汉庭酒店大山子店", cpcaSeg.getAddress());
		assertTrue(cpcaSeg.noPca());
		Map<String, String> umap = new HashMap<>();
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店", umap);
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("长春市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("汉庭酒店大山子店", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		umap.put("朝阳区", "110105");
		cpcaSeg = cpcaExtractor.transform("朝阳区汉庭酒店大山子店", umap);
		assertEquals("北京市", cpcaSeg.getProvinceName());
		assertEquals("北京市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("汉庭酒店大山子店", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("朝阳区朝阳区汉庭酒店大山子店", umap);
		assertEquals("北京市", cpcaSeg.getProvinceName());
		assertEquals("北京市", cpcaSeg.getCityName());
		assertEquals("朝阳区", cpcaSeg.getAreaName());
		assertEquals("汉庭酒店大山子店", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("徐汇区虹漕路461号58号楼5楼", umap);
		assertEquals("上海市", cpcaSeg.getProvinceName());
		assertEquals("上海市", cpcaSeg.getCityName());
		assertEquals("徐汇区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		cpcaSeg = cpcaExtractor.transform("上海市上海市徐汇区虹漕路461号58号楼5楼", umap);
		assertEquals("上海市", cpcaSeg.getProvinceName());
		assertEquals("上海市", cpcaSeg.getCityName());
		assertEquals("徐汇区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("吉林省吉林市龙潭区虹漕路461号58号楼5楼", umap);
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("吉林市", cpcaSeg.getCityName());
		assertEquals("龙潭区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("吉林龙潭区虹漕路461号58号楼5楼", umap);
		assertEquals("吉林省", cpcaSeg.getProvinceName());
		assertEquals("龙潭区", cpcaSeg.getAreaName());
		assertEquals("虹漕路461号58号楼5楼", cpcaSeg.getAddress());
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("拱墅祥园路100号", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("拱墅祥园路100号", cpcaSeg.getAddress());
		assertTrue(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("古墩路889号温州村", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("古墩路889号温州村", cpcaSeg.getAddress());
		assertTrue(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("白云区龙湖工业三街一号多城国际", umap);
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("白云区龙湖工业三街一号多城国际", cpcaSeg.getAddress());
		assertTrue(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("前郭县百顺堂药业有限公司");
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("五大连池市红旗路100号");
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("湖北仙桃红旗路100号");
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		cpcaSeg = cpcaExtractor.transform("万宁市万宁中学");
		assertFalse(cpcaSeg.noPca());
		assertTrue(cpcaSeg.fullPca());
		assertEquals("海南省", cpcaSeg.getProvinceName());
		assertEquals("省直辖县级行政区划", cpcaSeg.getCityName());
		assertEquals("万宁市", cpcaSeg.getAreaName());
		assertEquals("万宁中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("万宁中学");
		assertTrue(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		assertEquals(null, cpcaSeg.getProvinceName());
		assertEquals(null, cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("万宁中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州市第十中学");
		assertFalse(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("第十中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("杭州市杭州第十中学");
		assertFalse(cpcaSeg.noPca());
		assertFalse(cpcaSeg.fullPca());
		assertEquals("浙江省", cpcaSeg.getProvinceName());
		assertEquals("杭州市", cpcaSeg.getCityName());
		assertEquals(null, cpcaSeg.getAreaName());
		assertEquals("杭州第十中学", cpcaSeg.getAddress());
		cpcaSeg = cpcaExtractor.transform("天津市南开区芥园西道宜君北里10号楼3门803号");
		System.out.println(cpcaExtractor.encodeJson(cpcaSeg));
	}

	@Test
	public void testPerformance() {
		int count = 1000000;
		Long t1 = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			CpcaSeg cpcaSeg = cpcaExtractor.transform("上海市上海市徐汇区虹漕路461号58号楼5楼");
			assertTrue(cpcaSeg.fullPca());
		}
		Long t2 = System.currentTimeMillis();
		System.out.println(count / Math.max(1, (t2 - t1) / 1000) + "次/秒");
	}

}
