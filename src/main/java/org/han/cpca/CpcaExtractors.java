package org.han.cpca;

import com.google.common.base.Preconditions;

public final class CpcaExtractors {
	private CpcaExtractors() {}

	public static Builder builder() {
		return new Builder();
	}
	
	public static class Builder {
		private String cpcaCvsFile;
		public Builder withCpcaCvsFile(String file) {
			this.cpcaCvsFile = file;
			return this;
		}
		public CpcaExtractor build() {
			Preconditions.checkNotNull(this.cpcaCvsFile, "cpcaCvsFile不能为空");
			return new CpcaExtractorImpl(this.cpcaCvsFile); 
		}
	}
}
