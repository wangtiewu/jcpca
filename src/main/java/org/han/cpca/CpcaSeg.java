package org.han.cpca;

public class CpcaSeg {
	private String provinceName;
	private String cityName;
	private String areaName;
	private String cpcaCode;//6位的编码
	private String address;//不带省、市、县的地址
	private CpcaIndex provinceNameIndex;
	private CpcaIndex cityNameIndex;
	private CpcaIndex areaNameIndex;
	public String getProvinceName() {
		return provinceName;
	}
	public void setProvinceName(String provinceName) {
		this.provinceName = provinceName;
	}
	public String getCityName() {
		return cityName;
	}
	public void setCityName(String cityName) {
		this.cityName = cityName;
	}
	public String getAreaName() {
		return areaName;
	}
	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}
	public String getCpcaCode() {
		return cpcaCode;
	}
	public void setCpcaCode(String cpcaCode) {
		this.cpcaCode = cpcaCode;
	}
	public CpcaIndex getProvinceNameIndex() {
		return provinceNameIndex;
	}
	public void setProvinceNameIndex(CpcaIndex provinceNameIndex) {
		this.provinceNameIndex = provinceNameIndex;
	}
	public CpcaIndex getCityNameIndex() {
		return cityNameIndex;
	}
	public void setCityNameIndex(CpcaIndex cityNameIndex) {
		this.cityNameIndex = cityNameIndex;
	}
	public CpcaIndex getAreaNameIndex() {
		return areaNameIndex;
	}
	public void setAreaNameIndex(CpcaIndex areaNameIndex) {
		this.areaNameIndex = areaNameIndex;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * 地址是否包含省、市、区至少一处信息
	 * @return
	 */
	public boolean hasPca() {
		return this.provinceName != null || this.cityName != null || this.areaName != null;
	}
	
	/**
	 * 地址无省、市、区信息
	 * @return
	 */
	public boolean noPca() {
		return !hasPca();
	}
	
	public boolean fullPca() {
		return this.provinceName != null && this.cityName != null && this.areaName != null; 
	}
	
	public boolean hasProvince() {
		return this.provinceNameIndex != null; 
	}
	
	public boolean hasCity() {
		return this.cityNameIndex != null; 
	}
	
	public boolean hasArea() {
		return this.areaNameIndex != null; 
	}
	
	public static CpcaSeg none() {
		return new CpcaSeg();
	}
	
	public boolean hasAddress() {
		return this.address != null && !this.address.equals("");
	}
	
	public void reset() {
		setProvinceName(null);
		setCityName(null);
		setAreaName(null);
		setAddress(null);
		setCpcaCode(null);
		setProvinceNameIndex(null);
		setCityNameIndex(null);
		setAreaNameIndex(null);
	}
}

class CpcaIndex {
	private Integer beginIndex;
	private Integer endIndex;
	public CpcaIndex() {
	}
	public CpcaIndex(Integer beginIndex, Integer endIndex) {
		this.beginIndex = beginIndex;
		this.endIndex = endIndex;
	}
	
	public Integer getBeginIndex() {
		return beginIndex;
	}
	public void setBeginIndex(Integer beginIndex) {
		this.beginIndex = beginIndex;
	}
	public Integer getEndIndex() {
		return endIndex;
	}
	public void setEndIndex(Integer endIndex) {
		this.endIndex = endIndex;
	}
}
